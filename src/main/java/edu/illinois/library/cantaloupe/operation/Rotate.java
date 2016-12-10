package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.util.StringUtil;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a rotation operation.
 */
public class Rotate implements Operation {

    private float degrees = 0f;

    /**
     * No-op constructor.
     */
    public Rotate() {}

    /**
     * @param degrees Degrees of rotation between 0 and 360
     */
    public Rotate(float degrees) {
        this.setDegrees(degrees);
    }

    /**
     * @param degrees Degrees to add.
     */
    public void addDegrees(float degrees) {
        this.degrees = (this.degrees + degrees) % 360;
    }

    /**
     * @return Degrees.
     */
    public float getDegrees() {
        return degrees;
    }

    /**
     * @param fullSize
     * @return Resulting dimensions when the scale is applied to the given full
     * size.
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        final int width = (int) Math.round(
                Math.abs(fullSize.width * Math.cos(this.getDegrees())) +
                        Math.abs(fullSize.height * Math.sin(this.getDegrees())));
        final int height = (int) Math.round(
                Math.abs(fullSize.height * Math.cos(this.getDegrees())) +
                        Math.abs(fullSize.width * Math.sin(this.getDegrees())));
        return new Dimension(width, height);
    }

    @Override
    public boolean isNoOp() {
        return (Math.abs(getDegrees()) < 0.0001f);
    }

    @Override
    public boolean isNoOp(Dimension fullSize, OperationList opList) {
        return isNoOp();
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
     * @param fullSize Ignored.
     * @return Map with a <code>degrees</code> key and a float degree value.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("degrees", getDegrees());
        return map;
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     * represent the instance.
     */
    @Override
    public String toString() {
        return StringUtil.removeTrailingZeroes(this.getDegrees());
    }

}
