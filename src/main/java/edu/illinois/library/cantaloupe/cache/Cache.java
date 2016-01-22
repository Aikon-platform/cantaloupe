package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;

import java.awt.Dimension;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>Interface to be implemented by all caches. A cache stores and retrieves
 * unique images corresponding to
 * {@link edu.illinois.library.cantaloupe.image.OperationList} objects, as
 * well as {@link java.awt.Dimension} objects corresponding to
 * {@link edu.illinois.library.cantaloupe.image.Identifier} objects.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface Cache {

    /**
     * <p>Reads cached dimension information.</p>
     *
     * <p>If a dimension corresponding to the given identifier exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param identifier Image identifier for which to retrieve a dimension.
     * @return Dimension corresponding to the given identifier, or null if no
     * non-expired dimension exists in the cache.
     * @throws CacheException
     */
    Dimension getDimension(Identifier identifier) throws CacheException;

    /**
     * <p>Returns a readable byte channel corresponding to the given
     * OperationList.</p>
     *
     * <p>If an image corresponding to the given parameters exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param opList Operation list for which to retrieve an input stream, for
     *               reading from the cache.
     * @return Readable byte channel corresponding to the given operation list,
     * or null if a non-expired image corresponding to the given operation list
     * does not exist in the cache.
     */
    ReadableByteChannel getImageReadableChannel(OperationList opList);

    /**
     * @param opList Operation list for which to retrieve an output stream, for
     *               writing to the cache.
     * @return Writable byte channel to which an image corresponding to the
     * given parameters can be written.
     * @throws CacheException
     */
    WritableByteChannel getImageWritableChannel(OperationList opList)
            throws CacheException;

    /**
     * Deletes the entire cache contents.
     *
     * @throws CacheException If the cache is misconfigured or there is some
     * other error.
     * @throws CacheException
     */
    void purge() throws CacheException;

    /**
     * Deletes all cached content corresponding to the image with the given
     * identifier.
     *
     * @param identifier
     * @throws CacheException
     */
    void purge(Identifier identifier) throws CacheException;

    /**
     * Deletes the cached image corresponding to the given operation list.
     *
     * @param opList
     * @throws CacheException
     */
    void purge(OperationList opList) throws CacheException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws CacheException
     */
    void purgeExpired() throws CacheException;

    /**
     * Adds an image's dimension information to the cache. If the writing of
     * the dimension is interrupted, implementations should clean it up, if
     * necessary.
     *
     * @param identifier Identifier of the image corresponding to the given
     *                   size.
     * @param size Dimension containing width and height in pixels.
     * @throws CacheException
     */
    void putDimension(Identifier identifier, Dimension size)
            throws CacheException;

}
