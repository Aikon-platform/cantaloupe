package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.Assert.*;

public class CropTest {

    private static final float DELTA = 0.0000001f;

    private Crop crop;

    @Before
    public void setUp() {
        crop = new Crop();
        assertEquals(Crop.Unit.PIXELS, crop.getUnit());
        assertEquals(0f, crop.getX(), DELTA);
        assertEquals(0f, crop.getY(), DELTA);
        assertEquals(0f, crop.getWidth(), DELTA);
        assertEquals(0f, crop.getHeight(), DELTA);
    }

    @Test
    public void testFromRectangle() {
        Rectangle rect = new Rectangle(25, 25, 75, 75);
        Crop crop = Crop.fromRectangle(rect);
        assertEquals(crop.getX(), rect.x, DELTA);
        assertEquals(crop.getY(), rect.y, DELTA);
        assertEquals(crop.getWidth(), rect.width, DELTA);
        assertEquals(crop.getHeight(), rect.height, DELTA);
    }

    @Test
    public void testGetRectangle() {
        Dimension fullSize = new Dimension(200, 200);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals(new Rectangle(0, 0, 200, 200), crop.getRectangle(fullSize));
        // pixels
        crop = new Crop();
        crop.setX(20f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        assertEquals(new Rectangle(20, 20, 50, 50), crop.getRectangle(fullSize));
        // percentage
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        assertEquals(new Rectangle(40, 40, 100, 100), crop.getRectangle(fullSize));
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(200, 200);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals(new Dimension(200, 200), crop.getResultingSize(fullSize));
        // pixels
        crop = new Crop();
        crop.setX(20f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        assertEquals(new Dimension(50, 50), crop.getResultingSize(fullSize));
        // percentage
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        assertEquals(new Dimension(100, 100), crop.getResultingSize(fullSize));
    }

    @Test
    public void testIsNull() {
        // new instance
        Crop crop = new Crop();
        assertFalse(crop.isNoOp());
        // 100% crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(1f);
        crop.setHeight(1f);
        assertTrue(crop.isNoOp());
        // <100% crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.8f);
        crop.setHeight(0.8f);
        assertFalse(crop.isNoOp());
        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        assertFalse(crop.isNoOp());
    }

    @Test
    public void testSetHeight() {
        float height = 50f;
        this.crop.setHeight(height);
        assertEquals(height, this.crop.getHeight(), DELTA);
    }

    @Test
    public void testSetNegativeHeight() {
        try {
            this.crop.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroHeight() {
        try {
            crop.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetMoreThan100PercentHeight() {
        try {
            crop.setUnit(Crop.Unit.PERCENT);
            crop.setHeight(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height percentage must be <= 1", e.getMessage());
        }
    }

    @Test
    public void testSetWidth() {
        Float width = 50f;
        crop.setWidth(width);
        assertEquals(width, this.crop.getWidth(), DELTA);
    }

    @Test
    public void testSetNegativeWidth() {
        try {
            crop.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroWidth() {
        try {
            crop.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetMoreThan100PercentWidth() {
        try {
            crop.setUnit(Crop.Unit.PERCENT);
            crop.setWidth(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width percentage must be <= 1", e.getMessage());
        }
    }

    @Test
    public void testSetX() {
        float x = 50f;
        crop.setX(x);
        assertEquals(x, this.crop.getX(), DELTA);
    }

    @Test
    public void testSetNegativeX() {
        try {
            crop.setX(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    @Test
    public void testSetMoreThan100PercentX() {
        try {
            crop.setUnit(Crop.Unit.PERCENT);
            crop.setX(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X percentage must be <= 1", e.getMessage());
        }
    }

    @Test
    public void testSetY() {
        float y = 50f;
        crop.setY(y);
        assertEquals(y, this.crop.getY(), DELTA);
    }

    @Test
    public void testSetNegativeY() {
        try {
            crop.setY(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    @Test
    public void testSetMoreThan100PercentY() {
        try {
            crop.setUnit(Crop.Unit.PERCENT);
            crop.setY(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y percentage must be <= 1", e.getMessage());
        }
    }

    @Test
    public void testToString() {
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals("none", crop.toString());

        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(40f);
        assertEquals("0,0,50,40", crop.toString());

        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.5f);
        crop.setHeight(0.4f);
        assertEquals("0%,0%,50%,40%", crop.toString());
    }

}
