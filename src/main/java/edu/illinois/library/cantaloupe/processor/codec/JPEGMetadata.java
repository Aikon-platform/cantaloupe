package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadataNode;
import java.nio.charset.StandardCharsets;

/**
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html">
 *      JPEG Metadata Format Specification and Usage Notes</a>
 */
class JPEGMetadata extends IIOMetadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGMetadata.class);

    private boolean checkedForExif, checkedForIptc, checkedForXmp;

    /** Cached by getEXIF() */
    private byte[] exif;

    /** Cached by getIPTC() */
    private byte[] iptc;

    /** Cached by getOrientation() */
    private Orientation orientation;

    /** Cached by getXMP() */
    private String xmp;

    JPEGMetadata(javax.imageio.metadata.IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return EXIF data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getEXIF() {
        if (!checkedForExif) {
            checkedForExif = true;
            // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
            // identical nodes at /markerSequence/unknown[@MarkerTag=225]
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("225".equals(marker.getAttribute("MarkerTag"))) {
                    byte[] data = (byte[]) marker.getUserObject();
                    // Check the first byte to see whether it's EXIF or XMP.
                    if (data[0] == 69) {
                        exif = data;
                    }
                }
            }
        }
        return exif;
    }

    /**
     * @return Orientation from the non-XMP EXIF metadata. May be null.
     */
    Orientation getExifOrientation() {
        return Util.readOrientation(getEXIF());
    }

    /**
     * @return IPTC data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getIPTC() {
        if (!checkedForIptc) {
            checkedForIptc = true;
            // IPTC metadata appears in the IIOMetadataNode tree at
            // /markerSequence/unknown[@MarkerTag=237]
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("237".equals(marker.getAttribute("MarkerTag"))) {
                    iptc = (byte[]) marker.getUserObject();
                }
            }
        }
        return iptc;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * @return Orientation from the metadata. EXIF is checked first, then XMP.
     *         If not found, {@link Orientation#ROTATE_0} will be returned.
     */
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            // Check EXIF.
            orientation = getExifOrientation();
            if (orientation == null) {
                // Check XMP.
                if (getXMP() != null) {
                    orientation = getXmpOrientation();
                }
            }
            if (orientation == null) {
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    @Override
    public String getXMP() {
        if (!checkedForXmp) {
            checkedForXmp = true;
            // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
            // identical nodes at /markerSequence/unknown[@MarkerTag=225]
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("225".equals(marker.getAttribute("MarkerTag"))) {
                    byte[] data = (byte[]) marker.getUserObject();
                    // Check the first byte to see whether it's EXIF or XMP.
                    if (data[0] == 104) {
                        xmp = new String(data, StandardCharsets.UTF_8);
                        xmp = StringUtils.trimXMP(xmp);
                    }
                }
            }
        }
        return xmp;
    }

    /**
     * @return Orientation from the XMP metadata. May be null.
     */
    Orientation getXmpOrientation() {
        Orientation orientation = null;
        final String xmp = getXMP();
        if (xmp != null) {
            orientation = Util.readOrientation(xmp);
        }
        return orientation;
    }

}
