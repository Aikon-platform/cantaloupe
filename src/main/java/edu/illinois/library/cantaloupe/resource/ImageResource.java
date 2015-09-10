package edu.illinois.library.cantaloupe.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles IIIF image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-request-parameters">Image
 * Request Parameters</a>
 */
public class ImageResource extends AbstractResource {

    /**
     * Restlet representation for images, returned by ImageResource.doGet().
     *
     * <em>Note:</em> doGet() should handle all preflight checks. Once it has
     * returned an instance of this class, it will no longer be possible to
     * render the error page, as response headers will have already been sent.
     */
    class ImageRepresentation extends OutputRepresentation {

        File sourceFile;
        InputStream inputStream;
        Parameters params;
        SourceFormat sourceFormat;

        /**
         * Constructor for local-file images.
         *
         * @param mediaType
         * @param sourceFormat
         * @param params
         * @param sourceFile
         */
        public ImageRepresentation(MediaType mediaType,
                                   SourceFormat sourceFormat,
                                   Parameters params,
                                   File sourceFile) {
            super(mediaType);
            this.sourceFile = sourceFile;
            this.params = params;
            this.sourceFormat = sourceFormat;
        }

        /**
         * Constructor for images from InputStreams.
         *
         * @param mediaType
         * @param sourceFormat
         * @param params
         * @param inputStream
         */
        public ImageRepresentation(MediaType mediaType,
                                   SourceFormat sourceFormat,
                                   Parameters params,
                                   InputStream inputStream) {
            super(mediaType);
            this.inputStream = inputStream;
            this.params = params;
            this.sourceFormat = sourceFormat;
        }

        /**
         * Writes the source image to the given output stream.
         *
         * @param outputStream Response body output stream supplied by Restlet
         * @throws IOException
         */
        public void write(OutputStream outputStream) throws IOException {
            try {
                Processor proc = ProcessorFactory.
                        getProcessor(this.sourceFormat);
                if (this.sourceFile != null) {
                    proc.process(this.params, this.sourceFormat,
                            this.sourceFile, outputStream);
                } else {
                    proc.process(this.params, this.sourceFormat,
                            this.inputStream, outputStream);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(ImageResource.class);

    @Get
    public Representation doGet() throws Exception {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = Reference.decode((String) attrs.get("identifier"));
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);

        Resolver resolver = ResolverFactory.getResolver();
        SourceFormat sourceFormat = SourceFormat.getSourceFormat(identifier);
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);
        Set availableOutputFormats = proc.getAvailableOutputFormats(sourceFormat);
        if (!availableOutputFormats.contains(params.getOutputFormat())) {
            String msg;
            if (sourceFormat == SourceFormat.UNKNOWN) {
                msg = String.format("%s does not support this source format",
                        proc.getClass().getSimpleName());
            } else {
                msg = String.format("%s does not support the \"%s\" source format",
                        proc.getClass().getSimpleName(),
                        sourceFormat.getPreferredExtension());
            }
            logger.warn(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        this.addHeader("Link", String.format("<%s>;rel=\"canonical\"",
                params.getCanonicalUri(this.getRootRef().toString() +
                        ImageServerApplication.BASE_IIIF_PATH)));

        File sourceFile = resolver.getFile(identifier);
        InputStream inputStream = null;
        if (sourceFile == null) {
            inputStream = resolver.getInputStream(identifier);
        }
        MediaType mediaType = new MediaType(
                OutputFormat.valueOf(format.toUpperCase()).getMediaType());

        if (sourceFile != null) {
            return new ImageRepresentation(mediaType, sourceFormat, params,
                    sourceFile);
        } else {
            return new ImageRepresentation(mediaType, sourceFormat, params,
                    inputStream);
        }
    }

}
