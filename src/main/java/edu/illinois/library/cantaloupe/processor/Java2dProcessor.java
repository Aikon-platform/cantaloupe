package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Processor using the Java 2D and ImageIO libraries.</p>
 */
class Java2dProcessor extends AbstractImageIOProcessor
        implements StreamProcessor, FileProcessor {

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        return Java2DPostProcessor.SUPPORTED_FEATURES;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality> getSupportedIIIF1Qualities() {
        return Java2DPostProcessor.SUPPORTED_IIIF_1_QUALITIES;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality> getSupportedIIIF2Qualities() {
        return Java2DPostProcessor.SUPPORTED_IIIF_2_QUALITIES;
    }

    @Override
    public void process(final OperationList ops,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(ops, imageInfo, outputStream);

        ImageReader reader = null;
        try {
            reader = getReader();
            final ReductionFactor rf    = new ReductionFactor();
            final Set<ReaderHint> hints = EnumSet.noneOf(ReaderHint.class);

            // If the source and output formats are both GIF, the source may
            // contain multiple frames, in which case the post-processing steps
            // will have to be different. (No problem if it only contains one
            // frame, though.)
            if (Format.GIF.equals(imageInfo.getSourceFormat()) &&
                    Format.GIF.equals(ops.getOutputFormat())) {
                BufferedImageSequence seq = reader.readSequence();
                Java2DPostProcessor.postProcess(
                        seq, ops, imageInfo, reader.getMetadata(0),
                        outputStream);
            } else {
                BufferedImage image =
                        reader.read(ops, imageInfo.getOrientation(), rf, hints);
                Java2DPostProcessor.postProcess(
                        image, hints, ops, imageInfo, rf, reader.getMetadata(0),
                        outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

}
