package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Abstract reader that supplies some base functionality and tries to be
 * efficient with most formats. Format-specific readers may override what they
 * need to.
 */
abstract class AbstractImageReader {

    // Note: methods that return BufferedImages (for Java 2D) are arranged
    // toward the beginning of the class; methods that return RenderedImages
    // (for JAI) are toward the end.

    private Format format;

    /** Set in setSource(). */
    protected ImageInputStream inputStream;

    /** Assigned by createReader(). */
    javax.imageio.ImageReader iioReader;

    /** Set in setSource(). */
    private Object source;

    /**
     * Initializes an instance.
     *
     * @param inputFile Image file to read.
     */
    AbstractImageReader(Path inputFile, Format format) throws IOException {
        setSource(inputFile);
        setFormat(format);

        createReader();
    }

    /**
     * Initializes an instance.
     *
     * @param streamSource Source of streams to read.
     */
    AbstractImageReader(StreamSource streamSource,
                        Format format) throws IOException {
        setSource(streamSource);
        setFormat(format);

        createReader();
    }

    private List<javax.imageio.ImageReader> availableIIOReaders() {
        final Iterator<javax.imageio.ImageReader> it;
        if (format != null) {
            it = ImageIO.getImageReadersByMIMEType(
                    format.getPreferredMediaType().toString());
        } else {
            it = ImageIO.getImageReaders(inputStream);
        }

        final List<javax.imageio.ImageReader> iioReaders = new ArrayList<>();
        while (it.hasNext()) {
            iioReaders.add(it.next());
        }
        return iioReaders;
    }

    /**
     * @return Whether the metadata will need to be read.
     */
    private boolean canIgnoreMetadata() {
        final Configuration config = Configuration.getInstance();
        final boolean preserveMetadata = config.getBoolean(
                Key.PROCESSOR_PRESERVE_METADATA, false);
        final boolean respectOrientation = config.getBoolean(
                Key.PROCESSOR_RESPECT_ORIENTATION, false);
        return (!preserveMetadata && !respectOrientation);
    }

    private void createReader() throws IOException {
        if (inputStream == null) {
            throw new IOException("No source set.");
        }

        iioReader = negotiateIIOReader();

        if (iioReader != null) {
            getLogger().debug("Using {}", iioReader.getClass().getName());

            /*
            http://docs.oracle.com/javase/8/docs/api/javax/imageio/ImageReader.html#setInput(java.lang.Object,%20boolean,%20boolean)
            The ignoreMetadata parameter, if set to true, allows the reader
            to disregard any metadata encountered during the read. Subsequent
            calls to the getStreamMetadata and getImageMetadata methods may
            return null, and an IIOImage returned from readAll may return null
            from their getMetadata method. Setting this parameter may allow
            the reader to work more efficiently. The reader may choose to
            disregard this setting and return metadata normally.
            */
            final boolean ignoreMetadata = canIgnoreMetadata();
            getLogger().debug("Ignoring metadata? {}", ignoreMetadata);

            iioReader.setInput(inputStream, false, ignoreMetadata);
        } else {
            throw new IOException("Unable to determine the format of the" +
                    "source image.");
        }
    }

    /**
     * Should be called when the instance is no longer needed.
     */
    void dispose() {
        try {
            IOUtils.closeQuietly(inputStream);
        } finally {
            if (iioReader != null) {
                iioReader.dispose();
                iioReader = null;
            }
        }
    }

    abstract Compression getCompression(int imageIndex) throws IOException;

    javax.imageio.ImageReader getIIOReader() {
        return iioReader;
    }

    abstract Logger getLogger();
    abstract Metadata getMetadata(int imageIndex) throws IOException;

    /**
     * @return The number of images contained inside the source image.
     */
    int getNumResolutions() throws IOException {
        // The boolean parameter tells getNumImages() whether to scan for
        // images, which seems to be necessary for some, but is slower.
        int numImages = iioReader.getNumImages(false);
        if (numImages == -1) {
            numImages = iioReader.getNumImages(true);
        }
        return numImages;
    }

    /**
     * Gets the dimensions of the source image.
     *
     * @return Dimensions in pixels
     */
    Dimension getSize() throws IOException {
        return getSize(iioReader.getMinIndex());
    }

    /**
     * Gets the dimensions of the image at the given index.
     *
     * @param imageIndex
     * @return Dimensions in pixels
     */
    Dimension getSize(int imageIndex) throws IOException {
        final int width = iioReader.getWidth(imageIndex);
        final int height = iioReader.getHeight(imageIndex);
        return new Dimension(width, height);
    }

    /**
     * @param imageIndex
     * @return Tile size of the image at the given index. If the image is not
     *         tiled, the full image dimensions are returned.
     */
    Dimension getTileSize(int imageIndex) throws IOException {
        final int width = iioReader.getTileWidth(imageIndex);
        final int height = iioReader.getTileHeight(imageIndex);
        return new Dimension(width, height);
    }

    private javax.imageio.ImageReader negotiateIIOReader() {
        javax.imageio.ImageReader negotiatedReader = null;

        final List<javax.imageio.ImageReader> iioReaders =
                availableIIOReaders();
        boolean found = false;

        if (!iioReaders.isEmpty()) {
            final String[] preferredImplClasses = preferredIIOImplementations();

            getLogger().debug("ImageIO plugin preferences: {}",
                    (preferredImplClasses.length > 0) ?
                            String.join(", ", preferredImplClasses) : "none");

            if (preferredImplClasses.length > 0) {
                for (String preferredImplClass : preferredImplClasses) {
                    if (!found) {
                        for (javax.imageio.ImageReader candidateReader : iioReaders) {
                            if (preferredImplClass.equals(candidateReader.getClass().getName())) {
                                negotiatedReader = candidateReader;
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (negotiatedReader == null) {
                negotiatedReader = iioReaders.get(0);
            }
        }
        return negotiatedReader;
    }

    /**
     * N.B. This method returns a list of strings rather than classes because
     * some readers reside under the com.sun package which is private in
     * Java 9.
     *
     * @return Preferred reader implementation classes, in priority order, or
     *         an empty array if there is no preference.
     */
    String[] preferredIIOImplementations() {
        return new String[] {};
    }

    private void reset() throws IOException {
        if (source instanceof Path) {
            setSource((Path) source);
        } else {
            setSource((StreamSource) source);
        }
        createReader();
    }

    void setFormat(Format format) {
        this.format = format;
    }

    void setSource(Path inputFile) throws IOException {
        dispose();
        source = inputFile;
        inputStream = ImageIO.createImageInputStream(inputFile.toFile());
    }

    void setSource(StreamSource streamSource) throws IOException {
        dispose();
        source = streamSource;
        inputStream = streamSource.newImageInputStream();
    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// BufferedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * Expedient but not necessarily efficient method that reads a whole image
     * (excluding subimages) in one shot.
     *
     * @return Read image.
     */
    BufferedImage read() throws IOException {
        return iioReader.read(0);
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, exploiting its
     * tile layout and/or subimages, if possible.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param ops
     * @param orientation     Orientation of the source image data as reported
     *                        by e.g. embedded metadata.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     *         not be of {@link BufferedImage#TYPE_CUSTOM}. Clients should
     *         check the hints set to see whether they need to perform
     *         additional cropping.
     */
    BufferedImage read(final OperationList ops,
                       final Orientation orientation,
                       final ReductionFactor reductionFactor,
                       final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        BufferedImage image;

        Crop crop = (Crop) ops.getFirst(Crop.class);
        if (crop != null && !hints.contains(ImageReader.Hint.IGNORE_CROP)) {
            final Dimension fullSize = new Dimension(
                    iioReader.getWidth(0), iioReader.getHeight(0));
            image = tileAwareRead(0, crop.getRectangle(fullSize), hints);
        } else {
            image = iioReader.read(0);
        }

        if (image == null) {
            throw new UnsupportedSourceFormatException(iioReader.getFormatName());
        }

        return image;
    }

    /**
     * Reads the smallest image that can fulfill the given crop and scale from
     * a multi-resolution image.
     *
     * @param crop   Requested crop
     * @param scale  Requested scale
     * @param rf     The {@link ReductionFactor#factor} will be set to the
     *               reduction factor of the returned image.
     * @param hints  Will be populated by information returned by the reader.
     * @return The smallest image fitting the requested crop and scale
     *         operations from the given reader.
     */
    BufferedImage readSmallestUsableSubimage(
            final Crop crop,
            final Scale scale,
            final ReductionFactor rf,
            final Set<ImageReader.Hint> hints)
            throws IOException {
        final Dimension fullSize = new Dimension(
                iioReader.getWidth(0), iioReader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(fullSize);
        BufferedImage bestImage = null;
        if (!scale.hasEffect()) {
            bestImage = tileAwareRead(0, regionRect, hints);
            getLogger().debug("readSmallestUsableSubimage(): using a {}x{} source " +
                            "image (0x reduction factor)",
                    bestImage.getWidth(), bestImage.getHeight());
        } else {
            // Pyramidal TIFFs will have > 1 image, each with half the
            // dimensions of the previous one. The boolean parameter tells
            // getNumImages() whether to scan for images, which seems to be
            // necessary for at least some files, but is slower. If it is
            // false, and getNumImages() can't find anything, it will return -1.
            int numImages = iioReader.getNumImages(false);
            if (numImages > 1) {
                getLogger().debug("Detected {} subimage(s)", numImages);
            } else if (numImages == -1) {
                numImages = iioReader.getNumImages(true);
                if (numImages > 1) {
                    getLogger().debug("Scan revealed {} subimage(s)", numImages);
                }
            }
            // At this point, we know how many images are available.
            if (numImages == 1) {
                bestImage = tileAwareRead(0, regionRect, hints);
                getLogger().debug("readSmallestUsableSubimage(): using a " +
                                "{}x{} source image (0x reduction factor)",
                        bestImage.getWidth(), bestImage.getHeight());
            } else if (numImages > 1) {
                // Loop through the reduced images from smallest to largest to
                // find the first one that can supply the requested scale.
                for (int i = numImages - 1; i >= 0; i--) {
                    final int subimageWidth = iioReader.getWidth(i);
                    final int subimageHeight = iioReader.getHeight(i);

                    final double reducedScale = (double) subimageWidth /
                            (double) fullSize.width;
                    if (fits(regionRect, scale, reducedScale)) {
                        rf.factor = ReductionFactor.
                                forScale(reducedScale, 0).factor;
                        getLogger().debug("Subimage {}: {}x{} - fits! " +
                                        "({}x reduction factor)",
                                i + 1, subimageWidth, subimageHeight,
                                rf.factor);
                        final Rectangle reducedRect = new Rectangle(
                                (int) Math.round(regionRect.x * reducedScale),
                                (int) Math.round(regionRect.y * reducedScale),
                                (int) Math.round(regionRect.width * reducedScale),
                                (int) Math.round(regionRect.height * reducedScale));
                        bestImage = tileAwareRead(i, reducedRect, hints);
                        break;
                    } else {
                        getLogger().debug("Subimage {}: {}x{} - too small",
                                i + 1, subimageWidth, subimageHeight);
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * <p>Returns an image for the requested source area by reading the tiles
     * (or strips) of the source image and joining them into a single image.</p>
     *
     * <p>This method is intended to be compatible with all source images, no
     * matter the data layout (tiled, striped, etc.).</p>
     *
     * <p>This method may populate <code>hints</code> with
     * {@link ImageReader.Hint#ALREADY_CROPPED}, in which case
     * cropping will have already been performed according to the
     * <code>requestedSourceArea</code> parameter.</p>
     *
     * @param imageIndex Index of the image to read from the ImageReader.
     * @param region     Image region to retrieve. The returned image will be
     *                   this size or smaller if it would overlap the right or
     *                   bottom edge of the source image.
     * @param hints      Will be populated with information returned from the
     *                   reader.
     * @return Image
     */
    private BufferedImage tileAwareRead(final int imageIndex,
                                        final Rectangle region,
                                        final Set<ImageReader.Hint> hints)
            throws IOException {
        final Dimension imageSize = new Dimension(
                iioReader.getWidth(imageIndex),
                iioReader.getHeight(imageIndex));
        getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} image",
                region.x, region.y, region.width, region.height,
                imageSize.width, imageSize.height);

        hints.add(ImageReader.Hint.ALREADY_CROPPED);
        final ImageReadParam param = iioReader.getDefaultReadParam();
        param.setSourceRegion(region);

        try {
            return iioReader.read(imageIndex, param);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Numbers of source Raster bands " +
                    "and source color space components do not match")) {
                /*
                This probably means that the embedded ICC profile is
                incompatible with the source image data.
                (JPEGImageReader is not very lenient.)
                See: https://github.com/medusa-project/cantaloupe/issues/41

                To deal with this situation, we will try reading again,
                ignoring the color profile. We need to reset the reader, and
                then read into a grayscale BufferedImage.
                */
                reset();

                // Credit: http://stackoverflow.com/a/11571181
                final Iterator<ImageTypeSpecifier> imageTypes =
                        iioReader.getImageTypes(0);
                while (imageTypes.hasNext()) {
                    final ImageTypeSpecifier imageTypeSpecifier = imageTypes.next();
                    final int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
                    if (bufferedImageType == BufferedImage.TYPE_BYTE_GRAY) {
                        param.setDestinationType(imageTypeSpecifier);
                        break;
                    }
                }

                return iioReader.read(imageIndex, param);
            } else {
                throw e;
            }
        }

    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// RenderedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * Reads an image (excluding subimages).
     *
     * @return RenderedImage
     */
    RenderedImage readRendered() throws IOException {
        return iioReader.readAsRenderedImage(0,
                iioReader.getDefaultReadParam());
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, exploiting its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param ops
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return RenderedImage best matching the given parameters.
     */
    RenderedImage readRendered(final OperationList ops,
                               final Orientation orientation,
                               final ReductionFactor reductionFactor,
                               final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        RenderedImage image;
        Crop crop = (Crop) ops.getFirst(Crop.class);
        if (crop != null && !hints.contains(ImageReader.Hint.IGNORE_CROP)) {
            image = iioReader.readAsRenderedImage(0,
                    iioReader.getDefaultReadParam());
        } else {
            image = iioReader.read(0);
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(iioReader.getFormatName());
        }
        return image;
    }

    /**
     * Reads the smallest image that can fulfill the given crop and scale from
     * a multi-resolution image.
     *
     * @param crop  Requested crop
     * @param scale Requested scale
     * @param rf    The {@link ReductionFactor#factor} will be set to the
     *              reduction factor of the returned image.
     * @return The smallest image fitting the requested crop and scale
     *         operations from the given reader.
     */
    RenderedImage readSmallestUsableSubimage(final Crop crop,
                                             final Scale scale,
                                             final ReductionFactor rf)
            throws IOException {
        final Dimension fullSize = new Dimension(
                iioReader.getWidth(0), iioReader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(fullSize);
        final ImageReadParam param = iioReader.getDefaultReadParam();
        RenderedImage bestImage = null;
        if (!scale.hasEffect()) {
            bestImage = iioReader.readAsRenderedImage(0, param);
            getLogger().debug("Using a {}x{} source image (0x reduction factor)",
                    bestImage.getWidth(), bestImage.getHeight());
        } else {
            // Pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger. The "true" parameter tells getNumImages() to
            // scan for images, which seems to be necessary for at least some
            // files, but is slower.
            int numImages = iioReader.getNumImages(false);
            if (numImages > 1) {
                getLogger().debug("Detected {} subimage(s)", numImages - 1);
            } else if (numImages == -1) {
                numImages = iioReader.getNumImages(true);
                if (numImages > 1) {
                    getLogger().debug("Scan revealed {} subimage(s)",
                            numImages - 1);
                }
            }
            if (numImages == 1) {
                bestImage = iioReader.read(0, param);
                getLogger().debug("Using a {}x{} source image (0x reduction " +
                                "factor)",
                        bestImage.getWidth(), bestImage.getHeight());
            } else if (numImages > 1) {
                // Loop through the reduced images from smallest to largest to
                // find the first one that can supply the requested scale.
                for (int i = numImages - 1; i >= 0; i--) {
                    final int subimageWidth = iioReader.getWidth(i);
                    final int subimageHeight = iioReader.getHeight(i);

                    final double reducedScale = (double) subimageWidth /
                            (double) fullSize.width;
                    if (fits(regionRect, scale, reducedScale)) {
                        rf.factor = ReductionFactor.forScale(reducedScale, 0).factor;
                        getLogger().debug("Subimage {}: {}x{} - fits! " +
                                        "({}x reduction factor)",
                                i + 1, subimageWidth, subimageHeight,
                                rf.factor);
                        bestImage = iioReader.readAsRenderedImage(i, param);
                        break;
                    } else {
                        getLogger().debug("Subimage {}: {}x{} - too small",
                                i + 1, subimageWidth, subimageHeight);
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * @param regionRect   Cropped source image region, in source image
     *                     coordinates.
     * @param scale        Requested scale.
     * @param reducedScale Reduced scale of a pyramid level.
     * @return Whether the given source image region can be satisfied by the
     *         given reduced scale at the requested scale.
     */
    private boolean fits(Rectangle regionRect,
                         Scale scale,
                         double reducedScale) {
        boolean fits = false;
        if (scale.getPercent() != null) {
            float cappedScale = (scale.getPercent() > 1) ?
                    1 : scale.getPercent();
            fits = (cappedScale <= reducedScale);
        } else if (Scale.Mode.ASPECT_FIT_WIDTH.equals(scale.getMode())) {
            int cappedWidth = (scale.getWidth() > regionRect.width) ?
                    regionRect.width : scale.getWidth();
            fits = (cappedWidth / (double) regionRect.width <= reducedScale);
        } else if (Scale.Mode.ASPECT_FIT_HEIGHT.equals(scale.getMode())) {
            int cappedHeight = (scale.getHeight() > regionRect.height) ?
                    regionRect.height : scale.getHeight();
            fits = (cappedHeight / (double) regionRect.height <= reducedScale);
        } else if (Scale.Mode.ASPECT_FIT_INSIDE.equals(scale.getMode()) ||
                Scale.Mode.NON_ASPECT_FILL.equals(scale.getMode())) {
            int cappedWidth = (scale.getWidth() > regionRect.width) ?
                    regionRect.width : scale.getWidth();
            int cappedHeight = (scale.getHeight() > regionRect.height) ?
                    regionRect.height : scale.getHeight();
            fits = (cappedWidth / (double) regionRect.width <= reducedScale &&
                    cappedHeight / (double) regionRect.height <= reducedScale);
        }
        return fits;
    }

}
