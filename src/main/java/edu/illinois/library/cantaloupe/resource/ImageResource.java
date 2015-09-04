package edu.illinois.library.cantaloupe.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import edu.illinois.library.cantaloupe.request.Format;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

public class ImageResource extends AbstractResource {

    class ImageRepresentation extends OutputRepresentation {

        Parameters params;

        public ImageRepresentation(MediaType mediaType, Parameters params) {
            super(mediaType);
            this.params = params;
        }

        public void write(OutputStream outputStream) throws IOException {
            try {
                Processor proc = ProcessorFactory.getProcessor();
                proc.process(this.params, outputStream);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    @Get
    public Representation doGet() throws IllegalArgumentException,
            UnsupportedEncodingException, FileNotFoundException {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = java.net.URLDecoder.
                decode((String) attrs.get("identifier"), "UTF-8");
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);

        Resolver resolver = ResolverFactory.getResolver();
        if (resolver.resolve(identifier) == null) {
            throw new FileNotFoundException("Resource not found");
        }

        Processor proc = ProcessorFactory.getProcessor();
        if (!proc.getSupportedFormats().contains(params.getFormat())) {
            String msg = String.format("%s supports only the following formats: %s",
                    proc, StringUtils.join(proc.getSupportedFormats(), ", "));
            throw new IllegalArgumentException(msg);
        }

        this.addHeader("Link",
                "<" + params.getCanonicalUri(this.getRootRef().toString()) +
                        ">;rel=\"canonical\"");

        MediaType mediaType = new MediaType(
                Format.valueOf(format.toUpperCase()).getMediaType());
        return new ImageRepresentation(mediaType, params);
    }

}
