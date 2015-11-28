package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.image.Transpose;

/**
 * Encapsulates the "rotation" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#rotation">IIIF Image API 2.0</a>
 */
class Rotation implements Comparable<Object> {

    private float degrees = 0f;
    private boolean mirror = false;

    public static Rotation fromUri(String rotationUri)
            throws IllegalArgumentException {
        Rotation rotation = new Rotation();
        try {
            if (rotationUri.startsWith("!")) {
                rotation.setMirror(true);
                rotation.setDegrees(Float.parseFloat(rotationUri.substring(1)));
            } else {
                rotation.setMirror(false);
                rotation.setDegrees(Float.parseFloat(rotationUri));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid rotation");
        }
        return rotation;
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof edu.illinois.library.cantaloupe.image.Rotation) {
            edu.illinois.library.cantaloupe.image.Rotation rotation =
                    (edu.illinois.library.cantaloupe.image.Rotation) object;
            if (this.getDegrees() == rotation.getDegrees()) {
                return 0;
            }
        } else if (object instanceof Transpose) {
            Transpose flip = (Transpose) object;
            if (this.shouldMirror() && flip.getAxis() == Transpose.Axis.HORIZONTAL) {
                return 0;
            }
        } else if (object instanceof Rotation) {
            Rotation rotation = (Rotation) object;
            if (rotation.shouldMirror() == this.shouldMirror()) {
                return new Float(this.getDegrees()).
                        compareTo(rotation.getDegrees());
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        return this.compareTo(obj) == 0;
    }

    public float getDegrees() {
        return degrees;
    }

    @Override
    public int hashCode(){
        return ("" + this.getDegrees() + this.shouldMirror()).hashCode();
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalArgumentException
     */
    public void setDegrees(float degrees) throws IllegalArgumentException {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalArgumentException("Degrees must be between 0 and 360");
        }
        this.degrees = degrees;
    }

    /**
     * @param mirror Whether the request should be mirrored before being
     *               rotated.
     */
    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    /**
     * @return Whether the request should be mirrored before being rotated.
     */
    public boolean shouldMirror() {
        return mirror;
    }

    /**
     * @return Transpose, or null if there is no transposition.
     */
    public Transpose toTranspose() {
        if (shouldMirror()) {
            Transpose flip = new Transpose();
            flip.setAxis(Transpose.Axis.HORIZONTAL);
            return flip;
        }
        return null;
    }

    public edu.illinois.library.cantaloupe.image.Rotation toRotation() {
        edu.illinois.library.cantaloupe.image.Rotation rotation =
                new edu.illinois.library.cantaloupe.image.Rotation();
        rotation.setDegrees(this.getDegrees());
        return rotation;
    }

    /**
     * @return Value compatible with the rotation component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (this.shouldMirror()) {
            str += "!";
        }
        str += NumberUtil.formatForUrl(this.getDegrees());
        return str;
    }

}
