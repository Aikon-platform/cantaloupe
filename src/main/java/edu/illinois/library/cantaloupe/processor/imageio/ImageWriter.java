package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s in several formats.
 */
public class ImageWriter {

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.GIF, Format.JPG,
                    Format.PNG, Format.TIF));

    private OperationList opList;
    private Metadata sourceMetadata;

    /**
     * @return Set of supported output formats.
     */
    public static Set<Format> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    public ImageWriter(final OperationList opList) {
        this.opList = opList;
    }

    public ImageWriter(final OperationList opList,
                       final Metadata sourceMetadata) {
        this.opList = opList;
        this.sourceMetadata = sourceMetadata;
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    public void write(final RenderedImage image,
                      final Format outputFormat,
                      final OutputStream outputStream) throws IOException {
        switch (outputFormat) {
            case GIF:
                new GIFImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
            case JPG:
                new JPEGImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
            case PNG:
                new PNGImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
            case TIF:
                new TIFFImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
        }
    }

}
