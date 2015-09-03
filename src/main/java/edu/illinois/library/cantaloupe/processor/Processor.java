package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.Parameters;

import java.io.OutputStream;

/**
 * Interface to be implemented by all image processors.
 */
public interface Processor {

    /**
     * @param identifier IIIF identifier
     * @param imageBaseUri Base URI of the image
     * @return ImageInfo for the image corresponding to the given identifier.
     */
    ImageInfo getImageInfo(String identifier, String imageBaseUri);

    /**
     * Processes an image based on the supplied parameters and writes the
     * result to the given OutputStream. Implementations should use
     * <code>ResolverFactory.getResolver()</code> to obtain an instance of the
     * correct Resolver, and use that to find the source image based on its
     * identifier (<code>Parameters.getIdentifier()</code>).
     *
     * @param p
     * @param os
     * @throws Exception
     */
    void process(Parameters p, OutputStream os) throws Exception;

    /**
     * @param identifier IIIF identifier
     * @return Whether or not the resource corresponding to the given
     * identifier exists.
     */
    boolean resourceExists(String identifier);

}
