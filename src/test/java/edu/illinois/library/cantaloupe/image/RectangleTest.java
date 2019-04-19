package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RectangleTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private Rectangle instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Rectangle(10, 5, 1000, 800);
    }

    @Test
    void testDoubleConstructor() {
        instance = new Rectangle(10.2, 5.2, 1000.2, 800.2);
        assertEquals(10.2, instance.x(), DELTA);
        assertEquals(5.2, instance.y(), DELTA);
        assertEquals(1000.2, instance.width(), DELTA);
        assertEquals(800.2, instance.height(), DELTA);
    }

    @Test
    void testIntegerConstructor() {
        assertEquals(10, instance.x(), DELTA);
        assertEquals(5, instance.y(), DELTA);
        assertEquals(1000, instance.width(), DELTA);
        assertEquals(800, instance.height(), DELTA);
    }

    @Test
    void testCopyConstructor() {
        Rectangle other = new Rectangle(instance);
        assertEquals(other, instance);
    }

    @Test
    void testEqualsWithEqualInstances() {
        assertEquals(instance, new Rectangle(10, 5, 1000, 800));
    }

    @Test
    void testEqualsWithUnequalInstances() {
        assertNotEquals(instance, new Rectangle(11, 5, 1000, 800));
        assertNotEquals(instance, new Rectangle(10, 6, 1000, 800));
        assertNotEquals(instance, new Rectangle(10, 5, 1001, 800));
        assertNotEquals(instance, new Rectangle(10, 5, 1000, 801));
    }

    @Test
    void testGrowWidth() {
        final double initialWidth = instance.width();
        instance.growWidth(3.2);
        Rectangle expected = new Rectangle(
                instance.x(), instance.y(),
                initialWidth + 3.2, instance.height());
        assertEquals(expected, instance);
    }

    @Test
    void testGrowHeight() {
        final double initialHeight = instance.height();
        instance.growHeight(3.2);
        Rectangle expected = new Rectangle(
                instance.x(), instance.y(),
                instance.width(), initialHeight + 3.2);
        assertEquals(expected, instance);
    }

    @Test
    void testHashCode() {
        int[] codes = {
                Double.hashCode(instance.x()),
                Double.hashCode(instance.y()),
                Double.hashCode(instance.width()),
                Double.hashCode(instance.height())
        };
        int expected = Arrays.hashCode(codes);
        assertEquals(expected, instance.hashCode());
    }

    @Test
    void testIntX() {
        instance.setX(5.4);
        assertEquals(5, instance.intX());
        instance.setX(5.6);
        assertEquals(6, instance.intX());
    }

    @Test
    void testIntY() {
        instance.setY(5.4);
        assertEquals(5, instance.intY());
        instance.setY(5.6);
        assertEquals(6, instance.intY());
    }

    @Test
    void testIntWidth() {
        instance.setWidth(5.4);
        assertEquals(5, instance.intWidth());
        instance.setWidth(5.6);
        assertEquals(6, instance.intWidth());
    }

    @Test
    void testIntHeight() {
        instance.setHeight(5.4);
        assertEquals(5, instance.intHeight());
        instance.setHeight(5.6);
        assertEquals(6, instance.intHeight());
    }

    @Test
    void testIntersectsWithIntersectingInstance() {
        Rectangle other = new Rectangle(instance);
        assertTrue(other.intersects(instance));

        other.setX(0);
        other.setY(0);
        assertTrue(other.intersects(instance));

        other.setX(500);
        other.setY(500);
        assertTrue(other.intersects(instance));
    }

    @Test
    void testIntersectsWithNonIntersectingInstance() {
        // too far N
        Rectangle other = new Rectangle(10, 0, 1000, 5);
        assertFalse(other.intersects(instance));

        // too far E
        other = new Rectangle(1100, 0, 1000, 800);
        assertFalse(other.intersects(instance));

        // too far S
        other = new Rectangle(10, 900, 1000, 800);
        assertFalse(other.intersects(instance));

        // too far W
        other = new Rectangle(0, 0, 4, 800);
        assertFalse(other.intersects(instance));
    }

    @Test
    void testIsEmptyWithNonEmptyInstance() {
        assertFalse(instance.isEmpty());
    }

    @Test
    void testIsEmptyWithEmptyWidth() {
        instance.setWidth(0.4);
        assertTrue(instance.isEmpty());
    }

    @Test
    void testIsEmptyWithEmptyHeight() {
        instance.setHeight(0.4);
        assertTrue(instance.isEmpty());
    }

    @Test
    void testMoveLeft() {
        final double initialX = instance.x();
        instance.moveLeft(3.5);
        Rectangle expected = new Rectangle(
                initialX - 3.5, instance.y(),
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    void testMoveRight() {
        final double initialX = instance.x();
        instance.moveRight(3.5);
        Rectangle expected = new Rectangle(
                initialX + 3.5, instance.y(),
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    void testMoveUp() {
        final double initialY = instance.y();
        instance.moveUp(3.5);
        Rectangle expected = new Rectangle(
                instance.x(), initialY - 3.5,
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    void testMoveDown() {
        final double initialY = instance.y();
        instance.moveDown(3.5);
        Rectangle expected = new Rectangle(
                instance.x(), initialY + 3.5,
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    void testScaleX() {
        instance.scaleX(0.8);
        assertEquals(8, instance.x(), DELTA);
        assertEquals(5, instance.y(), DELTA);
        assertEquals(800, instance.width(), DELTA);
        assertEquals(800, instance.height(), DELTA);
    }

    @Test
    void testScaleXWithZeroArgument() {
        assertThrows(IllegalArgumentException.class, () -> instance.scaleX(0));
    }

    @Test
    void testScaleXWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.scaleX(-0.5));
    }

    @Test
    void testScaleY() {
        instance.scaleY(0.8);
        assertEquals(10, instance.x(), DELTA);
        assertEquals(4, instance.y(), DELTA);
        assertEquals(1000, instance.width(), DELTA);
        assertEquals(640, instance.height(), DELTA);
    }

    @Test
    void testScaleYWithZeroArgument() {
        assertThrows(IllegalArgumentException.class, () -> instance.scaleY(0));
    }

    @Test
    void testScaleYWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class, () -> instance.scaleY(-0.5));
    }

    @Test
    void testSize() {
        assertEquals(new Dimension(1000, 800), instance.size());
    }

    @Test
    void testSetDimension() {
        Dimension size = new Dimension(50, 40);
        instance.setDimension(size);
        assertEquals(size, instance.size());
    }

    @Test
    void testSetDoubleX() {
        instance.setX(5.2);
        assertEquals(5.2, instance.x(), DELTA);
    }

    @Test
    void testSetIntegerX() {
        instance.setX(5);
        assertEquals(5, instance.intX());
    }

    @Test
    void testSetDoubleY() {
        instance.setY(5.2);
        assertEquals(5.2, instance.y(), DELTA);
    }

    @Test
    void testSetIntegerY() {
        instance.setY(5);
        assertEquals(5, instance.intY());
    }

    @Test
    void testSetDoubleWidth() {
        instance.setWidth(5.2);
        assertEquals(5.2, instance.width(), DELTA);
    }

    @Test
    void testSetDoubleWidthWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-5.2));
    }

    @Test
    void testSetIntegerWidth() {
        instance.setWidth(5);
        assertEquals(5, instance.intWidth());
    }

    @Test
    void testSetIntegerWidthWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-5));
    }

    @Test
    void testSetDoubleHeight() {
        instance.setHeight(5.2);
        assertEquals(5.2, instance.height(), DELTA);
    }

    @Test
    void testSetDoubleHeightWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-5.2));
    }

    @Test
    void testSetIntegerHeight() {
        instance.setHeight(5);
        assertEquals(5, instance.intHeight());
    }

    @Test
    void testSetIntegerHeightWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-5));
    }

    @Test
    void testToAWTRectangle() {
        java.awt.Rectangle expected = new java.awt.Rectangle(10, 5, 1000, 800);
        assertEquals(expected, instance.toAWTRectangle());
    }

    @Test
    void testToString() {
        String expected = String.format("%f,%f/%fx%f",
                instance.x(), instance.y(),
                instance.width(), instance.height());
        assertEquals(expected, instance.toString());
    }

}
