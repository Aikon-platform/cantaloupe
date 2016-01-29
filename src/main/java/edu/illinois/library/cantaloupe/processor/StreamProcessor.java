package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to be implemented by image processors that support input via
 * input streams.
 */
public interface StreamProcessor extends Processor {

    /**
     * @param inputStream Stream for reading the source image.
     *                    Implementations should close it.
     * @param sourceFormat Format of the source image.
     * @return Pixel dimensions of the source image.
     * @throws ProcessorException
     */
    Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException;

    /**
     * <p>Performs the supplied operations on an image, reading it from the
     * supplied stream, and writing the result to the supplied stream.</p>
     *
     * <p>Operations should be applied in the order they appear in the
     * OperationList iterator. For the sake of efficiency, implementations
     * should check whether each one is a no-op
     * ({@link edu.illinois.library.cantaloupe.image.Operation#isNoOp()})
     * before performing it.</p>
     *
     * <p>Implementations should get the full size of the source image from
     * the sourceSize parameter instead of their {#link #getSize} method,
     * for efficiency's sake.</p>
     *
     * @param ops OperationList of the image to process.
     * @param sourceFormat Format of the source image. Will never be
     * {@link SourceFormat#UNKNOWN}.
     * @param sourceSize Scale of the source image.
     * @param streamSource Source for acquiring streams from which to read
     *                     the imagee.
     * @param outputStream Stream to write the image to.
     *                     Implementations should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(OperationList ops, SourceFormat sourceFormat,
                 Dimension sourceSize, StreamSource streamSource,
                 OutputStream outputStream) throws ProcessorException;

}
