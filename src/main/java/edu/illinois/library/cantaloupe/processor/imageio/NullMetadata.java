package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;

import javax.imageio.metadata.IIOMetadata;

class NullMetadata extends AbstractMetadata implements Metadata {

    /**
     * @param metadata
     * @param formatName
     */
    NullMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null.
     */
    @Override
    public Object getExif() {
        return null;
    }

    /**
     * @return Null.
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
     * @return Null.
     */
    @Override
    public byte[] getXmp() {
        return null;
    }

    /**
     * @return Null.
     */
    @Override
    public String getXmpRdf() {
        return null;
    }

}
