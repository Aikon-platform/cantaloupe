package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Image reader wrapping an ImageIO {@link javax.imageio.ImageReader}
 * instance, with enhancements to support efficient reading of multi-resolution
 * and/or tiled source images.</p>
 *
 * <p>Various image property accessors are available on the instance, and
 * additional metadata is available via {@link #getMetadata(int)}.</p>
 *
 * <p>Clients should remember to call {@link #dispose()} when done with an
 * instance.</p>
 */
public class ImageReader {

    public enum Hint {
        /**
         * Returned from a reader. The reader has read only the requested region
         * of the image and there will be no need to crop it any further.
         */
        ALREADY_CROPPED,

        /**
         * Provided to a reader. The reader should read the entire image
         * regardless of any cropping directives provided.
         */
        IGNORE_CROP
    }

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.BMP, Format.DCM,
                    Format.GIF, Format.JPG, Format.PNG, Format.TIF));

    private Metadata cachedMetadata;
    private AbstractImageReader reader;

    static {
        // The application will handle caching itself, if so configured. The
        // ImageIO cache would be redundant.
        ImageIO.setUseCache(false);
    }

    /**
     * @return Map of available output formats for all known source formats,
     *         based on information reported by ImageIO.
     */
    public static Set<Format> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    /**
     * Constructor for reading from files.
     *
     * @param sourceFile File to read from.
     * @param format Format of the source image.
     */
    public ImageReader(File sourceFile, Format format)
            throws IOException {
        switch (format) {
            case BMP:
                reader = new BMPImageReader(sourceFile);
                break;
            case DCM:
                reader = new DICOMImageReader(sourceFile);
                break;
            case GIF:
                reader = new GIFImageReader(sourceFile);
                break;
            case JPG:
                reader = new JPEGImageReader(sourceFile);
                break;
            case PNG:
                reader = new PNGImageReader(sourceFile);
                break;
            case TIF:
                reader = new TIFFImageReader(sourceFile);
                break;
        }
    }

    /**
     * Constructor for reading from streams.
     *
     * @param streamSource Source of stream to read from.
     * @param format Format of the source image.
     */
    public ImageReader(StreamSource streamSource, Format format)
            throws IOException {
        switch (format) {
            case BMP:
                reader = new BMPImageReader(streamSource);
                break;
            case DCM:
                reader = new DICOMImageReader(streamSource);
                break;
            case GIF:
                reader = new GIFImageReader(streamSource);
                break;
            case JPG:
                reader = new JPEGImageReader(streamSource);
                break;
            case PNG:
                reader = new PNGImageReader(streamSource);
                break;
            case TIF:
                reader = new TIFFImageReader(streamSource);
                break;
        }
    }

    /**
     * Should be called when the reader is no longer needed.
     */
    public void dispose() {
        reader.dispose();
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Compression type of the image at the given index.
     */
    public Compression getCompression(int imageIndex) throws IOException {
        return reader.getCompression(imageIndex);
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Metadata of the image at the given index.
     */
    public Metadata getMetadata(int imageIndex) throws IOException {
        if (cachedMetadata == null) {
            cachedMetadata = reader.getMetadata(imageIndex);
        }
        return cachedMetadata;
    }

    /**
     * @return
     */
    public int getNumResolutions() throws IOException {
        return reader.getNumResolutions();
    }

    /**
     * @return Actual dimensions of the image at the zero index, not taking
     *         into account embedded orientation metadata.
     */
    public Dimension getSize() throws IOException {
        return reader.getSize();
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Actual dimensions of the image at the given index, not taking
     *         into account embedded orientation metadata.
     */
    public Dimension getSize(int imageIndex) throws IOException {
        return reader.getSize(imageIndex);
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Size of the tiles of the image at the given index, not taking
     *         into account embedded orientation metadata. If the image is not
     *         tiled, the full image dimensions are returned.
     */
    public Dimension getTileSize(int imageIndex) throws IOException {
        return reader.getTileSize(imageIndex);
    }

    /**
     * Expedient but not necessarily efficient method that reads a whole
     * image (excluding subimages) in one shot.
     *
     * @return BufferedImage guaranteed to not be of type
     *         {@link BufferedImage#TYPE_CUSTOM}.
     */
    public BufferedImage read() throws IOException {
        return reader.read();
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, exploiting its
     * tile layout and/or subimages, if possible.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param opList          Note that if a
     *                        {@link edu.illinois.library.cantaloupe.operation.Crop}
     *                        operation is present, it will be modified
     *                        according to the <code>orientation</code>
     *                        argument.
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of
     *                        the returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     *         not be of type {@link BufferedImage#TYPE_CUSTOM}. Clients should
     *         check the hints set to see whether they need to perform
     *         additional cropping.
     */
    public BufferedImage read(final OperationList opList,
                              final Orientation orientation,
                              final ReductionFactor reductionFactor,
                              final Set<Hint> hints)
            throws IOException, ProcessorException {
        return reader.read(opList, orientation, reductionFactor, hints);
    }

    /**
     * Reads an image (excluding subimages).
     *
     * @return RenderedImage
     */
    public RenderedImage readRendered() throws IOException,
            UnsupportedSourceFormatException {
        return reader.readRendered();
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, exploiting its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param opList          Note that if a
     *                        {@link edu.illinois.library.cantaloupe.operation.Crop}
     *                        operation is present, it will be modified
     *                        according to the <code>orientation</code>
     *                        argument.
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of
     *                        the returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return RenderedImage best matching the given parameters.
     */
    public RenderedImage readRendered(final OperationList opList,
                                      final Orientation orientation,
                                      final ReductionFactor reductionFactor,
                                      final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        return reader.readRendered(opList, orientation, reductionFactor, hints);
    }

}
