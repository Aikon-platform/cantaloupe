package edu.illinois.library.cantaloupe.processor.codec;

import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import edu.illinois.library.cantaloupe.image.Orientation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

class Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    /**
     * Can help debug Node objects from {@link IIOMetadata#getAsTree(String)}.
     * Not used in production.
     *
     * @param root
     */
    static void prettyPrint(Node root) {
        displayMetadata(root, 0);
    }

    private static void indent(int level) {
        for (int i = 0; i < level; i++)
            System.out.print("    ");
    }

    private static void displayMetadata(Node node, int level) {
        // print open tag of element
        indent(level);
        System.out.print("<" + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            // print attribute values
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                System.out.print(" " + attr.getNodeName() +
                        "=\"" + attr.getNodeValue() + "\"");
            }
        }

        Node child = node.getFirstChild();
        if (child == null) {
            // no children, so close element and return
            System.out.println("/>");
            return;
        }

        // children, so close current tag
        System.out.println(">");
        while (child != null) {
            // print children recursively
            displayMetadata(child, level + 1);
            child = child.getNextSibling();
        }

        // print close tag of element
        indent(level);
        System.out.println("</" + node.getNodeName() + ">");
    }

    static Orientation orientationForExifValue(int value) {
        switch (value) {
            case 6:
                return Orientation.ROTATE_90;
            case 3:
                return Orientation.ROTATE_180;
            case 8:
                return Orientation.ROTATE_270;
            default:
                return Orientation.ROTATE_0;
        }
    }

    /**
     * Reads the orientation (tiff:Orientation) from an XMP string.
     *
     * @param xmp XMP string.
     * @return Orientation, or null if unspecified.
     */
    static Orientation readOrientation(String xmp) {
        RIOT.init();

        final Model model = ModelFactory.createDefaultModel();

        try (StringReader reader = new StringReader(xmp)) {
            model.read(reader, null, "RDF/XML");

            final NodeIterator it = model.listObjectsOfProperty(
                    model.createProperty("http://ns.adobe.com/tiff/1.0/Orientation"));
            if (it.hasNext()) {
                final int orientationValue =
                        Integer.parseInt(it.next().asLiteral().getString());
                return orientationForExifValue(orientationValue);
            }
        } catch (RiotException e) {
            // The XMP string is invalid RDF/XML. Not much we can do.
            LOGGER.info("readOrientation(String): {}", e.getMessage());
        }
        return null;
    }

    /**
     * Reads the orientation (tiff:Orientation) from EXIF data.
     *
     * @param exif EXIF data.
     * @return Orientation, or {@link Orientation#ROTATE_0} if unspecified.
     */
    static Orientation readOrientation(byte[] exif) {
        // See https://community.oracle.com/thread/1264022?start=0&tstart=0
        // for an explanation of the technique used here.
        if (exif != null) {
            final String preferredImpl =
                    new TIFFImageReader().getPreferredIIOImplementations()[0];
            final Iterator<javax.imageio.ImageReader> it =
                    ImageIO.getImageReadersByFormatName("TIFF");
            while (it.hasNext()) {
                final ImageReader reader = it.next();
                if (reader.getClass().getName().equals(preferredImpl)) {
                    try (ImageInputStream wrapper = new MemoryCacheImageInputStream(
                            new ByteArrayInputStream(exif, 6, exif.length - 6))) {
                        reader.setInput(wrapper, true, false);

                        final IIOMetadata exifMetadata = reader.getImageMetadata(0);
                        final TIFFDirectory exifDir =
                                TIFFDirectory.createFromMetadata(exifMetadata);
                        final TIFFField orientationField = exifDir.getTIFFField(274);
                        if (orientationField != null) {
                            return orientationForExifValue(orientationField.getAsInt(0));
                        }
                    } catch (IOException e) {
                        LOGGER.info("readOrientation(byte[]): {}", e.getMessage());
                    } finally {
                        reader.dispose();
                    }
                }
            }
        }
        return Orientation.ROTATE_0;
    }

    /**
     * Strips any enclosing tags or other content around the {@literal rdf:RDF}
     * element within an RDF/XML XMP string.
     */
    static String trimXMP(String xmp) {
        final int start = xmp.indexOf("<rdf:RDF");
        final int end = xmp.indexOf("</rdf:RDF");
        if (start > -1) {
            xmp = xmp.substring(start, end + 10);
        }
        return xmp;
    }

    private Util() {}

}
