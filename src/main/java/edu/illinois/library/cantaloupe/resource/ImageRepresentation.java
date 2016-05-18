package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.Disposition;
import org.restlet.representation.OutputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Restlet representation for images.
 */
public class ImageRepresentation extends OutputRepresentation {

    private static Logger logger = LoggerFactory.
            getLogger(ImageRepresentation.class);

    public static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    private ImageInfo imageInfo;
    private Processor processor;
    private RequestAttributes requestAttributes;

    /**
     * @param imageInfo
     * @param attrs
     * @param disposition
     * @param processor
     */
    public ImageRepresentation(final ImageInfo imageInfo,
                               final Processor processor,
                               final RequestAttributes attrs,
                               final Disposition disposition) {
        super(attrs.getOperationList().getOutputFormat().getPreferredMediaType());
        this.imageInfo = imageInfo;
        this.processor = processor;
        this.requestAttributes = attrs;
        this.setDisposition(disposition);
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param outputStream Response body output stream supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        final DerivativeCache cache = CacheFactory.getDerivativeCache();
        if (cache != null) {
            OutputStream cacheOutputStream = null;
            try (InputStream inputStream = cache.getImageInputStream(
                    requestAttributes.getOperationList())) {
                if (inputStream != null) {
                    // A cached image is available; write it to the response
                    // output stream.
                    IOUtils.copy(inputStream, outputStream);
                } else {
                    // Create a TeeOutputStream to write to the response output
                    // output stream and the cache pseudo-simultaneously.
                    cacheOutputStream = cache.getImageOutputStream(
                            requestAttributes.getOperationList());
                    OutputStream teeStream = new TeeOutputStream(
                            outputStream, cacheOutputStream);
                    doCacheAwareWrite(teeStream, cache);
                }
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                if (cacheOutputStream != null) {
                    cacheOutputStream.close();
                }
            }
        } else {
            doWrite(outputStream);
        }
    }

    /**
     * Variant of doWrite() that cleans up incomplete cached images when
     * the connection has been broken.
     *
     * @param outputStream
     * @param cache
     * @throws CacheException
     */
    private void doCacheAwareWrite(OutputStream outputStream,
                                   DerivativeCache cache)
            throws CacheException {
        try {
            doWrite(outputStream);
        } catch (IOException e) {
            logger.info(e.getMessage());
            cache.purge(requestAttributes.getOperationList());
        }
    }

    private void doWrite(OutputStream outputStream) throws IOException {
        try {
            final OperationList opList = requestAttributes.getOperationList();
            processor.setRequestAttributes(requestAttributes);

            final long msec = System.currentTimeMillis();

            // If the operations are effectively a no-op, the source image can
            // be streamed right through.
            if (opList.isNoOp(processor.getSourceFormat())) {
                if (processor instanceof FileProcessor &&
                        ((FileProcessor) processor).getSourceFile() != null) {
                    final File sourceFile = ((FileProcessor) processor).getSourceFile();
                    final InputStream inputStream = new FileInputStream(sourceFile);
                    IOUtils.copy(inputStream, outputStream);
                } else {
                    final StreamSource streamSource =
                            ((StreamProcessor) processor).getStreamSource();
                    final InputStream inputStream = streamSource.newInputStream();
                    IOUtils.copy(inputStream, outputStream);
                }
                logger.info("Streamed with no processing in {} msec: {}",
                        System.currentTimeMillis() - msec, opList);
            } else {
                processor.process(opList, imageInfo, outputStream);

                logger.info("{} processed in {} msec: {}",
                        processor.getClass().getSimpleName(),
                        System.currentTimeMillis() - msec, opList);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
