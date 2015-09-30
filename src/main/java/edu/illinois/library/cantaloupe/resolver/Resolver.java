package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;

/**
 * Locates and provides access to a source image. This is an abstract interface;
 * implementations should implement at least one of the sub-interfaces.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return The expected source format of the image corresponding with the
     * given identifier, or <code>SourceFormat.UNKNOWN</code> if unknown.
     * Never null.
     */
    SourceFormat getSourceFormat(String identifier);

}
