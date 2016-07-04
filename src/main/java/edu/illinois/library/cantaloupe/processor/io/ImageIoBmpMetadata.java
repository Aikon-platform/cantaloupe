package edu.illinois.library.cantaloupe.processor.io;

import javax.imageio.metadata.IIOMetadata;

class ImageIoBmpMetadata extends AbstractImageIoMetadata
        implements ImageIoMetadata {

    /**
     * @param metadata
     * @param formatName
     */
    public ImageIoBmpMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null, as BMP does not support EXIF.
     */
    @Override
    public Object getExif() {
        return null;
    }

    /**
     * @return Null, as BMP does not support IPTC.
     */
    @Override
    public Object getIptc() {
        return null;
    }

    /**
     * @return {@link Orientation#ROTATE_0}
     */
    @Override
    public Orientation getOrientation() {
        return Orientation.ROTATE_0;
    }

    /**
     * @return Null, as BMP does not support XMP.
     */
    @Override
    public Object getXmp() {
        return null;
    }

}
