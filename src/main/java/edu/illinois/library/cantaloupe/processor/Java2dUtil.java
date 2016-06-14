package edu.illinois.library.cantaloupe.processor;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.Position;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import edu.illinois.library.cantaloupe.processor.io.ImageIoImageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * A collection of methods for operating on {@link BufferedImage}s.
 */
public abstract class Java2dUtil {

    private static Logger logger = LoggerFactory.getLogger(Java2dUtil.class);

    /**
     * Redacts regions from the given image.
     *
     * @param baseImage Image to apply the watermark on top of.
     * @param appliedCrop Crop already applied to <code>baseImage</code>.
     * @param reductionFactor Reduction factor already applied to
     *                        <code>baseImage</code>.
     * @param redactions Regions of the image to redact.
     * @return Input image with redactions applied.
     */
    static BufferedImage applyRedactions(final BufferedImage baseImage,
                                         final Crop appliedCrop,
                                         final ReductionFactor reductionFactor,
                                         final List<Redaction> redactions) {
        if (baseImage != null && redactions.size() > 0) {
            final long msec = System.currentTimeMillis();
            final Dimension imageSize = new Dimension(
                    baseImage.getWidth(), baseImage.getHeight());

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setColor(Color.black);

            for (final Redaction redaction : redactions) {
                final Rectangle redactionRegion =
                        redaction.getResultingRegion(imageSize, appliedCrop);
                redactionRegion.x *= reductionFactor.getScale();
                redactionRegion.y *= reductionFactor.getScale();
                redactionRegion.width *= reductionFactor.getScale();
                redactionRegion.height *= reductionFactor.getScale();

                if (!redactionRegion.isEmpty()) {
                    logger.debug("applyRedactions(): applying {} at {},{}/{}x{}",
                            redaction, redactionRegion.x, redactionRegion.y,
                            redactionRegion.width, redactionRegion.height);
                    g2d.fill(redactionRegion);
                } else {
                    logger.debug("applyRedactions(): {} is outside crop area; skipping",
                            redaction);
                }
            }
            g2d.dispose();
            logger.debug("applyRedactions() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return baseImage;
    }

    /**
     * Applies the watermark to the given image.
     *
     * @param baseImage Image to apply the watermark on top of.
     * @param watermark Watermark to apply to the base image.
     * @return Watermarked image, or the input image if there is no watermark
     *         set in the application configuration.
     * @throws ConfigurationException
     * @throws IOException
     */
    static BufferedImage applyWatermark(final BufferedImage baseImage,
                                        final Watermark watermark)
            throws ConfigurationException, IOException {
        BufferedImage markedImage = baseImage;
        final Dimension imageSize = new Dimension(baseImage.getWidth(),
                baseImage.getHeight());
        if (WatermarkService.shouldApplyToImage(imageSize)) {
            markedImage = overlayImage(baseImage,
                    getWatermarkImage(watermark),
                    watermark.getPosition(),
                    watermark.getInset());
        }
        return markedImage;
    }

    /**
     * @param baseImage
     * @param overlayImage
     * @param position
     * @param inset Inset in pixels.
     * @return
     */
    private static BufferedImage overlayImage(final BufferedImage baseImage,
                                              final BufferedImage overlayImage,
                                              final Position position,
                                              final int inset) {
        if (overlayImage != null && position != null) {
            final long msec = System.currentTimeMillis();
            int overlayX, overlayY;
            switch (position) {
                case TOP_LEFT:
                    overlayX = inset;
                    overlayY = inset;
                    break;
                case TOP_RIGHT:
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = inset;
                    break;
                case BOTTOM_LEFT:
                    overlayX = inset;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
                // case BOTTOM_RIGHT: will be handled in default:
                case TOP_CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2;
                    overlayY = inset;
                    break;
                case BOTTOM_CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
                case LEFT_CENTER:
                    overlayX = inset;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2;
                    break;
                case RIGHT_CENTER:
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2;
                    break;
                case CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2;
                    break;
                default: // bottom right
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
            }

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(baseImage, 0, 0, null);
            g2d.drawImage(overlayImage, overlayX, overlayY, null);
            g2d.dispose();
            logger.debug("overlayImage() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return baseImage;
    }

    /**
     * <p>Copies the given BufferedImage of type
     * {@link BufferedImage#TYPE_CUSTOM} into a new image of type
     * {@link BufferedImage#TYPE_INT_RGB}, to make it work with the
     * rest of the image operation pipeline.</p>
     *
     * <p>This is extremely expensive and should be avoided if possible.</p>
     *
     * @param inImage Image to convert
     * @return A new BufferedImage of type RGB, or the input image if it
     * is not of custom type.
     */
    public static BufferedImage convertCustomToRgb(
            final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage != null && inImage.getType() == BufferedImage.TYPE_CUSTOM) {
            final long msec = System.currentTimeMillis();

            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();

            logger.debug("convertCustomToRgb() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return outImage;
    }

    /**
     * @param inImage Image to crop
     * @param crop Crop operation
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    static BufferedImage cropImage(final BufferedImage inImage,
                                   final Crop crop) {
        return cropImage(inImage, crop, new ReductionFactor());
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop Crop operation
     * @param rf Number of times the dimensions of <code>inImage</code> have
     *           already been halved relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    static BufferedImage cropImage(final BufferedImage inImage,
                                   final Crop crop,
                                   final ReductionFactor rf) {
        final Dimension croppedSize = crop.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));
        BufferedImage croppedImage;
        if (crop.isNoOp() || (croppedSize.width == inImage.getWidth() &&
                croppedSize.height == inImage.getHeight())) {
            croppedImage = inImage;
        } else {
            final long msec = System.currentTimeMillis();
            final double scale = rf.getScale();
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            int x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (crop.getShape().equals(Crop.Shape.SQUARE)) {
                final int shortestSide =
                        Math.min(inImage.getWidth(), inImage.getHeight());
                x = (inImage.getWidth() - shortestSide) / 2;
                y = (inImage.getHeight() - shortestSide) / 2;
                requestedWidth = requestedHeight = shortestSide;
            } else if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                x = (int) Math.round(regionX * inImage.getWidth());
                y = (int) Math.round(regionY * inImage.getHeight());
                requestedWidth = (int) Math.round(regionWidth  *
                        inImage.getWidth());
                requestedHeight = (int) Math.round(regionHeight *
                        inImage.getHeight());
            } else {
                x = (int) Math.round(regionX);
                y = (int) Math.round(regionY);
                requestedWidth = (int) Math.round(regionWidth);
                requestedHeight = (int) Math.round(regionHeight);
            }
            // BufferedImage.getSubimage() will protest if asked for more
            // width/height than is available
            croppedWidth = (x + requestedWidth > inImage.getWidth()) ?
                    inImage.getWidth() - x : requestedWidth;
            croppedHeight = (y + requestedHeight > inImage.getHeight()) ?
                    inImage.getHeight() - y : requestedHeight;
            croppedImage = inImage.getSubimage(x, y, croppedWidth,
                    croppedHeight);

            logger.debug("cropImage(): cropped {}x{} image to {} in {} msec",
                    inImage.getWidth(), inImage.getHeight(), crop,
                    System.currentTimeMillis() - msec);
        }
        return croppedImage;
    }

    /**
     * @param inImage Image to filter
     * @param filter Filter operation
     * @return Filtered image, or the input image if the given filter operation
     * is a no-op.
     */
    static BufferedImage filterImage(final BufferedImage inImage,
                                     final Filter filter) {
        BufferedImage filteredImage = inImage;
        final long msec = System.currentTimeMillis();
        switch (filter) {
            case GRAY:
                filteredImage = new BufferedImage(inImage.getWidth(),
                        inImage.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY);
                break;
            case BITONAL:
                filteredImage = new BufferedImage(inImage.getWidth(),
                        inImage.getHeight(),
                        BufferedImage.TYPE_BYTE_BINARY);
                break;
        }
        if (filteredImage != inImage) {
            Graphics2D g2d = filteredImage.createGraphics();
            g2d.drawImage(inImage, 0, 0, null);

            logger.debug("filterImage(): filtered {}x{} image in {} msec",
                    inImage.getWidth(), inImage.getHeight(),
                    System.currentTimeMillis() - msec);
        }
        return filteredImage;
    }

    /**
     * @param watermark
     * @return Watermark image, or null if
     *         {@link WatermarkService#WATERMARK_FILE_CONFIG_KEY} is not set.
     * @throws IOException
     */
    static BufferedImage getWatermarkImage(Watermark watermark)
            throws IOException {
        final ImageIoImageReader reader =
                new ImageIoImageReader(watermark.getImage(), Format.PNG);
        return reader.read();
    }

    public static BufferedImage removeAlpha(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getColorModel().hasAlpha()) {
            final long msec = System.currentTimeMillis();
            int newType;
            switch (inImage.getType()) {
                case BufferedImage.TYPE_4BYTE_ABGR:
                    newType = BufferedImage.TYPE_INT_BGR;
                    break;
                default:
                    newType = BufferedImage.TYPE_INT_RGB;
                    break;
            }
            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), newType);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();
            logger.debug("removeAlpha(): converted BufferedImage type {} to " +
                    "RGB in {} msec", inImage.getType(),
                    System.currentTimeMillis() - msec);
        }
        return outImage;
    }

    /**
     * @param inImage Image to rotate
     * @param rotate Rotate operation
     * @return Rotated image, or the input image if the given rotation is a
     * no-op.
     */
    static BufferedImage rotateImage(final BufferedImage inImage,
                                     final Rotate rotate) {
        BufferedImage rotatedImage = inImage;
        if (!rotate.isNoOp()) {
            final long msec = System.currentTimeMillis();
            final double radians = Math.toRadians(rotate.getDegrees());
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            final int canvasWidth = (int) Math.round(Math.abs(sourceWidth *
                    Math.cos(radians)) + Math.abs(sourceHeight *
                    Math.sin(radians)));
            final int canvasHeight = (int) Math.round(Math.abs(sourceHeight *
                    Math.cos(radians)) + Math.abs(sourceWidth *
                    Math.sin(radians)));

            // note: operations happen in reverse order of declaration
            AffineTransform tx = new AffineTransform();
            // 3. translate the image to the center of the "canvas"
            tx.translate(canvasWidth / 2, canvasHeight / 2);
            // 2. rotate it
            tx.rotate(radians);
            // 1. translate the image so that it is rotated about the center
            tx.translate(-sourceWidth / 2, -sourceHeight / 2);

            rotatedImage = new BufferedImage(canvasWidth, canvasHeight,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = rotatedImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inImage, tx, null);
            logger.debug("rotateImage() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return rotatedImage;
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage Image to scale
     * @param scale   Scale operation
     * @return Downscaled image, or the input image if the given scale is a
     *         no-op.
     */
    static BufferedImage scaleImage(final BufferedImage inImage,
                                    final Scale scale) {
        return scaleImage(inImage, scale, new ReductionFactor(0));
    }

    /**
     * Scales an image, taking an already-applied reduction factor into
     * account. In other words, the dimensions of the input image have already
     * been halved <code>rf</code> times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage Image to scale
     * @param scale Requested size ignoring any reduction factor
     * @param rf Reduction factor that has already been applied to
     *           <code>inImage</code>
     * @return Downscaled image, or the input image if the given scale is a
     *         no-op.
     */
    static BufferedImage scaleImage(final BufferedImage inImage,
                                    final Scale scale,
                                    final ReductionFactor rf) {
        final Dimension sourceSize = new Dimension(
                inImage.getWidth(), inImage.getHeight());

        // Calculate the size that the image will need to be scaled to based
        // on the source image size, scale, and already-applied reduction
        // factor.
        Dimension targetSize;
        if (scale.getPercent() != null) {
            targetSize = new Dimension();
            targetSize.width = (int) Math.round(sourceSize.width *
                    (scale.getPercent() / rf.getScale()));
            targetSize.height = (int) Math.round(sourceSize.height *
                    (scale.getPercent() / rf.getScale()));
        } else {
            targetSize = scale.getResultingSize(sourceSize);
        }

        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp() && (targetSize.width != sourceSize.width &&
                targetSize.height != sourceSize.height)) {
            /*
            This method uses the image scaling code in
            com.mortennobel.imagescaling (see
            https://blog.nobel-joergensen.com/2008/12/20/downscaling-images-in-java/)
            as an alternative to the scaling available in the Java 2D APIs.
            Problem is, while the performance of Graphics2D.drawImage() is OK,
            the quality, even using RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            is utter crap for downscaling. BufferedImage.getScaledInstance()
            is somewhat the opposite: great quality but very slow. There may be
            ways to mitigate the former (like multi-step downscaling) but not
            without cost.

            Subjective quality of downscale from 2288x1520 to 200x200:

            Lanczos3 > Box > Bicubic > Mitchell > Triangle > Bell > Hermite >
                BSpline > Graphics2D

            Approximate time-to-complete of same (milliseconds, 2.3GHz i7):
            Triangle: 19
            Box: 20
            Hermite: 24
            Bicubic: 25
            Mitchell: 30
            BSpline: 53
            Graphics2D: 70
            Bell: 145
            Lanczos3: 238

            Subjective quality of upscale from 2288x1520 to 3000x3000:
            Lanczos3 > Bicubic > Mitchell > Graphics2D = Triangle = Hermite >
                Bell > BSpline > Box

            Approximate time-to-complete of same (milliseconds, 2.3GHz i7):
            Triangle: 123
            Hermite: 142
            Box: 162
            Bicubic: 206
            Mitchell: 224
            BSpline: 230
            Graphics2D: 268
            Lanczos3: 355
            Bell: 468
            */
            final long startMsec = System.currentTimeMillis();

            final ResampleOp resampleOp = new ResampleOp(
                    targetSize.width, targetSize.height);

            if (targetSize.width < sourceSize.width ||
                    targetSize.height < sourceSize.height) {
                resampleOp.setFilter(ResampleFilters.getBoxFilter());
            } else {
                resampleOp.setFilter(ResampleFilters.getBiCubicFilter());
            }

            scaledImage = resampleOp.filter(inImage, null);

            logger.debug("scaleImage(): scaled {}x{} image to {}x{} in {} msec",
                    sourceSize.width, sourceSize.height,
                    targetSize.width, targetSize.height,
                    System.currentTimeMillis() - startMsec);
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image.
     */
    static BufferedImage transposeImage(final BufferedImage inImage,
                                        final Transpose transpose) {
        final long msec = System.currentTimeMillis();
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        switch (transpose) {
            case HORIZONTAL:
                tx.translate(-inImage.getWidth(null), 0);
                break;
            case VERTICAL:
                tx.translate(0, -inImage.getHeight(null));
                break;
        }
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        BufferedImage outImage = op.filter(inImage, null);

        logger.debug("transposeImage(): transposed image in {} msec",
                System.currentTimeMillis() - msec);
        return outImage;
    }

}
