package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Processor using the Java 2D and ImageIO frameworks.</p>
 *
 * <p>Because they both use ImageIO, this processor has a lot in common with
 * {@link JaiProcessor} and so common functionality has been extracted into a
 * base class.</p>
 */
class Java2dProcessor extends AbstractJava2dProcessor
        implements StreamProcessor, FileProcessor {

    static final String NORMALIZE_CONFIG_KEY = "Java2dProcessor.normalize";
    static final String SHARPEN_CONFIG_KEY = "Java2dProcessor.sharpen";

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final ImageReader reader = getReader();
        try {
            final Orientation orientation = getEffectiveOrientation();
            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageReader.Hint> hints = new HashSet<>();

            final Configuration config = ConfigurationFactory.getInstance();
            final boolean normalize =
                    config.getBoolean(NORMALIZE_CONFIG_KEY, false);
            if (normalize) {
                // When normalizing, the reader needs to read the entire image
                // so that its histogram can be sampled accurately. This will
                // preserve the luminance across tiles.
                hints.add(ImageReader.Hint.IGNORE_CROP);
            }

            BufferedImage image = reader.read(ops, orientation, rf, hints);
            postProcess(image, hints, ops, imageInfo, rf, orientation,
                    normalize, config.getFloat(SHARPEN_CONFIG_KEY, 0f),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

}
