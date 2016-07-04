package edu.illinois.library.cantaloupe.processor.io;

import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.RIOT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

class AbstractImageIoMetadata {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractImageIoMetadata.class);

    private String formatName;
    private IIOMetadata iioMetadata;

    AbstractImageIoMetadata(IIOMetadata metadata, String format) {
        iioMetadata = metadata;
        formatName = format;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIioMetadata().getAsTree(getFormatName());
    }

    private String getFormatName() {
        return formatName;
    }

    IIOMetadata getIioMetadata() {
        return iioMetadata;
    }

    protected ImageIoMetadata.Orientation orientationForExifValue(int value) {
        switch (value) {
            case 6:
                return ImageIoMetadata.Orientation.ROTATE_90;
            case 3:
                return ImageIoMetadata.Orientation.ROTATE_180;
            case 8:
                return ImageIoMetadata.Orientation.ROTATE_270;
        }
        return null;
    }

    /**
     * Reads the orientation (tiff:Orientation) from EXIF data.
     *
     * @param exif EXIF data.
     * @return Orientation, or null if unspecified.
     */
    protected ImageIoMetadata.Orientation readOrientation(byte[] exif) {
        // See https://community.oracle.com/thread/1264022?start=0&tstart=0
        // for an explanation of the technique used here.
        if (exif != null) {
            final Iterator<ImageReader> it =
                    ImageIO.getImageReadersByFormatName("TIFF");
            final ImageReader reader = it.next();
            try {
                final ImageInputStream wrapper = new MemoryCacheImageInputStream(
                        new ByteArrayInputStream(exif, 6, exif.length - 6));
                reader.setInput(wrapper, true, false);

                final IIOMetadata exifMetadata = reader.getImageMetadata(0);
                final TIFFDirectory exifDir =
                        TIFFDirectory.createFromMetadata(exifMetadata);
                final TIFFField orientationField = exifDir.getTIFFField(274);
                if (orientationField != null) {
                    return orientationForExifValue(orientationField.getAsInt(0));
                }
            } catch (IOException e) {
                logger.info(e.getMessage(), e);
            } finally {
                reader.dispose();
            }
        }
        return null;
    }

    /**
     * Reads the orientation (tiff:Orientation) from an XMP string.
     *
     * @param xmp XMP string.
     * @return Orientation, or null if unspecified.
     */
    protected ImageIoMetadata.Orientation readOrientation(String xmp) {
        RIOT.init();

        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(xmp), null, "RDF/XML");

        final NodeIterator it = model.listObjectsOfProperty(
                model.createProperty("http://ns.adobe.com/tiff/1.0/Orientation"));
        if (it.hasNext()) {
            final int orientationValue =
                    Integer.parseInt(it.next().asLiteral().getString());
            return orientationForExifValue(orientationValue);
        }
        return null;
    }

}
