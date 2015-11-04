package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.request.iiif.v1_1.Parameters;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import org.restlet.data.MediaType;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Handles IIIF image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
public class ImageResource extends AbstractResource {

    private static Logger logger = LoggerFactory.
            getLogger(ImageResource.class);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        if (this.getReference().toString().length() > 1024) {
            throw new ResourceException(414);
        }
        getResponseCacheDirectives().addAll(getCacheDirectives());
    }

    /**
     * Responds to IIIF Image requests.
     *
     * @return ImageRepresentation
     * @throws Exception
     */
    @Get
    public ImageRepresentation doGet() throws Exception {
        // Assemble the URI parameters into a Operations object
        Map<String,Object> attrs = this.getRequest().getAttributes();
        Parameters params = new Parameters(
                (String) attrs.get("identifier"),
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                (String) attrs.get("quality"),
                (String) attrs.get("format"));
        Operations ops = params.toOperations();
        Resolver resolver = ResolverFactory.getResolver();
        // Determine the format of the source image
        SourceFormat sourceFormat = resolver.
                getSourceFormat(ops.getIdentifier());
        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            throw new UnsupportedSourceFormatException();
        }
        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);

        ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                proc.getSupportedFeatures(sourceFormat),
                proc.getSupportedQualities(sourceFormat),
                proc.getAvailableOutputFormats(sourceFormat));
        this.addHeader("Link", String.format("<%s>;rel=\"profile\";",
                complianceLevel.getUri()));

        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        Set<OutputFormat> availableOutputFormats =
                proc.getAvailableOutputFormats(sourceFormat);
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    proc.getClass().getSimpleName(),
                    ops.getOutputFormat().getExtension());
            logger.warn(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        MediaType mediaType = new MediaType(
                ops.getOutputFormat().getMediaType());

        // FileResolver -> StreamProcessor: OK, using FileInputStream
        // FileResolver -> FileProcessor: OK, using File
        // StreamResolver -> StreamProcessor: OK, using InputStream
        // StreamResolver -> FileProcessor: NOPE
        if (!(resolver instanceof FileResolver) &&
                !(proc instanceof StreamProcessor)) {
            // FileProcessors can't work with StreamResolvers
            throw new UnsupportedSourceFormatException(
                    String.format("%s is not compatible with %s",
                            proc.getClass().getSimpleName(),
                            resolver.getClass().getSimpleName()));
        } else if (resolver instanceof FileResolver &&
                proc instanceof FileProcessor) {
            logger.debug("Using {} as a FileProcessor",
                    proc.getClass().getSimpleName());
            File inputFile = ((FileResolver) resolver).
                    getFile(ops.getIdentifier());
            return new ImageRepresentation(mediaType, sourceFormat, ops,
                    inputFile);
        } else if (resolver instanceof StreamResolver) {
            logger.debug("Using {} as a StreamProcessor",
                    proc.getClass().getSimpleName());
            StreamResolver sres = (StreamResolver) resolver;
            if (proc instanceof StreamProcessor) {
                StreamProcessor sproc = (StreamProcessor) proc;
                InputStream inputStream = sres.
                        getInputStream(ops.getIdentifier());
                Dimension fullSize = sproc.getSize(inputStream, sourceFormat);
                // avoid reusing the stream
                inputStream = sres.getInputStream(ops.getIdentifier());
                return new ImageRepresentation(mediaType, sourceFormat, fullSize,
                        ops, inputStream);
            }
        }
        return null; // this should never happen
    }

}
