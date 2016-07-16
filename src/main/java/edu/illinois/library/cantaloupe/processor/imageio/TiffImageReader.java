package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.processor.Java2dUtil;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

class TiffImageReader extends AbstractImageReader {

    private static Logger logger = LoggerFactory.
            getLogger(TiffImageReader.class);

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    TiffImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.TIF);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    TiffImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.TIF);
    }

    @Override
    protected void createReader() throws IOException {
        if (inputStream == null) {
            throw new IOException("No source set.");
        }

        Iterator<javax.imageio.ImageReader> it = ImageIO.getImageReadersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        while (it.hasNext()) {
            reader = it.next();
            // This version contains improvements over the Sun version,
            // namely support for BigTIFF.
            if (reader instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader) {
                break;
            }
        }
        if (reader != null) {
            reader.setInput(inputStream);
            logger.info("createReader(): using {}", reader.getClass().getName());
        } else {
            throw new IOException("Unable to determine the format of the " +
                    "source image.");
        }
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        if (reader == null) {
            createReader();
        }
        final IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new TiffMetadata(metadata, metadataFormat);
    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// BufferedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * <p>Attempts to read an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param ops
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     *         not be of {@link BufferedImage#TYPE_CUSTOM}. Clients should
     *         check the hints set to see whether they need to perform
     *         additional cropping.
     * @throws IOException
     * @throws ProcessorException
     */
    public BufferedImage read(final OperationList ops,
                              final ReductionFactor reductionFactor,
                              final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        if (reader == null) {
            createReader();
        }
        BufferedImage image = null;
        try {
            Crop crop = new Crop();
            crop.setFull(true);
            Scale scale = new Scale();
            scale.setMode(Scale.Mode.FULL);
            for (Operation op : ops) {
                if (op instanceof Crop) {
                    crop = (Crop) op;
                } else if (op instanceof Scale) {
                    scale = (Scale) op;
                }
            }
            image = readSmallestUsableSubimage(crop, scale,
                    reductionFactor, hints);
        } finally {
            dispose();
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(reader.getFormatName());
        }
        BufferedImage rgbImage = Java2dUtil.convertCustomToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converted {} to RGB (this is very expensive)",
                    ops.getIdentifier());
        }
        return rgbImage;
    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// RenderedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * <p>Attempts to reads an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param ops
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @return RenderedImage best matching the given parameters.
     * @throws IOException
     * @throws ProcessorException
     */
    public RenderedImage readRendered(final OperationList ops,
                                      final ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        if (reader == null) {
            createReader();
        }
        RenderedImage image;

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        for (Operation op : ops) {
            if (op instanceof Crop) {
                crop = (Crop) op;
            } else if (op instanceof Scale) {
                scale = (Scale) op;
            }
        }
        image = readSmallestUsableSubimage(crop, scale,
                reductionFactor);

        if (image == null) {
            throw new UnsupportedSourceFormatException(reader.getFormatName());
        }
        return image;
    }

}
