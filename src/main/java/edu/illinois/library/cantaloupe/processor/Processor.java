package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Interface to be implemented by all image processors.
 */
public interface Processor {

    /**
     * @param sourceFormat The source format for which to get a list of
     *                     available output formats.
     * @return Output formats available for the given source format.
     */
    Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat);

    /**
     * @param file Source image
     * @param sourceFormat
     * @return Size of the source image in pixels.
     * @throws Exception
     */
    Dimension getSize(File file, SourceFormat sourceFormat) throws Exception;

    /**
     * @param inputStream Source image
     * @param sourceFormat
     * @return Size of the source image in pixels.
     * @throws Exception
     */
    Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws Exception;

    /**
     * @param sourceFormat
     * @return All features supported by the processor for the given source
     * format.
     */
    Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat);

    /**
     * @param sourceFormat
     * @return All features supported by the processor for the given source
     * format.
     */
    Set<Quality> getSupportedQualities(SourceFormat sourceFormat);

    /**
     * @return Set of source formats for which there are any available output
     * formats.
     */
    Set<SourceFormat> getSupportedSourceFormats();

    /**
     * Uses the supplied parameters to process an image from the supplied
     * File, and writes the result to the given OutputStream.
     *
     * @param params Parameters of the output image
     * @param sourceFormat Format of the source image
     * @param sourceFile Source image file
     * @param outputStream An OutputStream to which to write the image
     * @throws Exception
     */
    void process(Parameters params, SourceFormat sourceFormat,
                 File sourceFile, OutputStream outputStream)
            throws Exception;

    /**
     * <p>Uses the supplied parameters to process an image from the supplied
     * InputStream, and writes the result to the given OutputStream.</p>
     *
     * <p>If implementations are not able to use an InputStream, then they
     * should do nothing.</p>
     *
     * @param params Parameters of the output image
     * @param sourceFormat Format of the source image
     * @param inputStream An InputStream from which to read the image
     * @param outputStream An OutputStream to which to write the image
     * @throws Exception
     */
    void process(Parameters params, SourceFormat sourceFormat,
                 InputStream inputStream, OutputStream outputStream)
            throws Exception;

}
