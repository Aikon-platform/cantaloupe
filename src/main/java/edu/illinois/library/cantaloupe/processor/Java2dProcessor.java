package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.processor.io.ImageIoImageReader;
import edu.illinois.library.cantaloupe.processor.io.ImageIoImageWriter;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Processor using the Java 2D and ImageIO frameworks.</p>
 *
 * <p>Because they both use ImageIO, this processor has a lot in common with
 * {@link JaiProcessor} and so a lot of functionality in both has been
 * extracted into a base class.</p>
 */
class Java2dProcessor extends AbstractImageIoProcessor
        implements StreamProcessor, FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(Java2dProcessor.class);

    static final String SCALE_MODE_CONFIG_KEY = "Java2dProcessor.scale_mode";

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        SUPPORTED_IIIF_2_0_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));
        SUPPORTED_FEATURES.addAll(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            final ImageIoImageReader reader = getReader();
            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageIoImageReader.ReaderHint> hints = new HashSet<>();
            BufferedImage image = reader.read(ops, rf, hints);

            // Apply the crop operation, if present, and maintain a reference
            // to it for subsequent operations to refer to.
            Crop crop = new Crop(0, 0, image.getWidth(), image.getHeight());
            for (Operation op : ops) {
                if (op instanceof Crop) {
                    crop = (Crop) op;
                    if (!hints.contains(ImageIoImageReader.ReaderHint.ALREADY_CROPPED)) {
                        image = Java2dUtil.cropImage(image, crop, rf);
                    }
                }
            }

            // Apply redactions, if present.
            final List<Redaction> redactions = new ArrayList<>();
            for (Operation op : ops) {
                if (op instanceof Redaction) {
                    redactions.add((Redaction) op);
                }
            }
            image = Java2dUtil.applyRedactions(image, crop, rf, redactions);

            // Apply all other operations.
            for (Operation op : ops) {
                if (op instanceof Scale) {
                    final boolean highQuality = Configuration.getInstance().
                            getString(SCALE_MODE_CONFIG_KEY, "speed").
                            equals("quality");
                    image = Java2dUtil.scaleImage(image, (Scale) op, rf,
                            highQuality);
                } else if (op instanceof Transpose) {
                    image = Java2dUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2dUtil.rotateImage(image, (Rotate) op);
                } else if (op instanceof Filter) {
                    image = Java2dUtil.filterImage(image, (Filter) op);
                } else if (op instanceof Watermark) {
                    try {
                        image = Java2dUtil.applyWatermark(image,
                                (Watermark) op);
                    } catch (ConfigurationException e) {
                        logger.error(e.getMessage());
                    }
                }
            }

            new ImageIoImageWriter(ops, reader.getMetadata(0)).
                    write(image, ops.getOutputFormat(), outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
