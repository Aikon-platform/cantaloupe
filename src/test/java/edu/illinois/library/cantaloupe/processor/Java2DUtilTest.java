package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.operation.overlay.StringOverlay;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.illinois.library.cantaloupe.test.Assert.*;
import static org.junit.Assert.*;

public class Java2DUtilTest extends BaseTest {

    @Test
    public void testApplyRedactions() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);
        final ReductionFactor rf = new ReductionFactor(0);

        int pixel = baseImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(106, red);
        assertEquals(90, green);
        assertEquals(60, blue);

        // create some Redactions
        List<Redaction> redactions = new ArrayList<>();
        redactions.add(new Redaction(new Rectangle(0, 0, 20, 20)));
        redactions.add(new Redaction(new Rectangle(20, 20, 20, 20)));
        final Crop crop = new Crop(0, 0, baseImage.getWidth(),
                baseImage.getTileHeight());

        // apply them
        final BufferedImage redactedImage = Java2DUtil.applyRedactions(
                baseImage, crop, rf, redactions);

        // test for the first one
        assertRGBA(redactedImage.getRGB(0, 0), 0, 0, 0, 255);

        // test for the second one
        assertRGBA(redactedImage.getRGB(25, 25), 0, 0, 0, 255);
    }

    @Test
    public void testApplyOverlayWithImageOverlay() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);

        int pixel = baseImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(106, red);
        assertEquals(90, green);
        assertEquals(60, blue);

        // create a Overlay
        final ImageOverlay overlay = new ImageOverlay(
                TestUtil.getImage("png-rgb-1x1x8.png"),
                Position.TOP_LEFT, 0);

        // apply it
        final BufferedImage overlaidImage = Java2DUtil.applyOverlay(
                baseImage, overlay);

        assertRGBA(overlaidImage.getRGB(0, 0), 0, 0, 0, 255);
    }

    @Test
    public void testApplyOverlayWithImageOverlayAndInset() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);

        int pixel = baseImage.getRGB(
                baseImage.getWidth() - 2, baseImage.getHeight() - 2);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(231, red);
        assertEquals(222, green);
        assertEquals(203, blue);

        // create a Overlay
        final int inset = 2;
        final ImageOverlay overlay = new ImageOverlay(
                TestUtil.getImage("png-rgb-1x1x8.png"), Position.BOTTOM_RIGHT,
                inset);

        // apply it
        final BufferedImage overlaidImage = Java2DUtil.applyOverlay(
                baseImage, overlay);

        pixel = overlaidImage.getRGB(
                baseImage.getWidth() - inset - 1,
                baseImage.getHeight() - inset - 1);
        assertRGBA(pixel, 0, 0, 0, 255);
    }

    @Test
    public void testApplyOverlayWithStringOverlay() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);

        // create a StringOverlay
        final StringOverlay overlay = new StringOverlay(
                "X", Position.TOP_LEFT, 0,
                new Font("Helvetica", Font.PLAIN, 12), 11,
                Color.WHITE, Color.BLACK, Color.WHITE, 0f);

        // apply it
        final BufferedImage overlaidImage = Java2DUtil.applyOverlay(
                baseImage, overlay);

        // Test the background color
        assertRGBA(overlaidImage.getRGB(2, 2), 0, 0, 0, 255);

        // Test the font color
        int pixel = overlaidImage.getRGB(8, 8);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertTrue(red > 240);
        assertTrue(green > 240);
        assertTrue(blue > 240);
    }

    @Test
    public void testCropImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        BufferedImage outImage = Java2DUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // square
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertEquals(100, outImage.getWidth());
        assertEquals(100, outImage.getHeight());

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertEquals(100, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testCropImageWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // full crop
        Crop crop = new Crop();
        crop.setFull(true);
        ReductionFactor rf = new ReductionFactor(1);
        BufferedImage outImage = Java2DUtil.cropImage(inImage, crop, rf);
        assertSame(inImage, outImage);

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.cropImage(inImage, crop, rf);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.cropImage(inImage, crop, rf);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    public void testFilterImage() {
        // TODO: write this
    }

    @Test
    public void testGetOverlayImage() throws Exception {
        ImageOverlay overlay = new ImageOverlay(
                TestUtil.getImage("png"), Position.BOTTOM_RIGHT, 0);
        assertNotNull(Java2DUtil.getOverlayImage(overlay));
    }

    @Test
    public void testReduceTo8Bits() throws IOException {
        // assert that an 8-bit image is untouched
        BufferedImage image = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        BufferedImage result = Java2DUtil.reduceTo8Bits(image);
        assertSame(image, result);

        // assert that a 16-bit image is downsampled
        // TODO: write this
    }

    /* removeAlpha(BufferedImage) */

    @Test
    public void testRemoveAlphaOnImageWithAlpha() throws IOException {
        File file = TestUtil.getImage("png-rgba-64x56x8.png");
        BufferedImage image = ImageIO.read(file);
        assertTrue(image.getColorModel().hasAlpha());

        image = Java2DUtil.removeAlpha(image);
        assertFalse(image.getColorModel().hasAlpha());
    }

    @Test
    public void testRemoveAlphaOnImageWithoutAlpha() throws IOException {
        File file = TestUtil.getImage("png-rgb-64x56x8.png");
        BufferedImage inImage = ImageIO.read(file);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertSame(inImage, outImage);
    }

    /* removeAlpha(BufferedImage, Color) */

    @Test
    public void testRemoveAlphaOnImageWithAlphaWithBackgroundColor()
            throws IOException {
        File file = TestUtil.getImage("png-rgba-64x56x8.png");
        BufferedImage inImage = ImageIO.read(file);
        assertTrue(inImage.getColorModel().hasAlpha());

        int[] rgba = { 0 };
        inImage.getAlphaRaster().setPixel(0, 0, rgba);

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage, Color.RED);

        int[] expected = new int[] {255, 0, 0, 0};
        int[] actual = new int[4];
        assertArrayEquals(expected, outImage.getRaster().getPixel(0, 0, actual));
    }

    @Test
    public void testRotateImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();

        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        final double radians = Math.toRadians(rotate.getDegrees());
        final int expectedWidth = (int) Math.round(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        final int expectedHeight = (int) Math.round(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, outImage.getWidth());
        assertEquals(expectedHeight, outImage.getHeight());
    }

    @Test
    public void testScaleImage() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.FULL
        Scale scale = new Scale();
        BufferedImage outImage = Java2DUtil.scaleImage(inImage, scale);
        assertSame(inImage, outImage);

        // Scale.Mode.ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.NON_ASPECT_FILL
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(80);
        scale.setHeight(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(80, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // scale-by percent
        scale = new Scale(0.25f);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    public void testScaleImageWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.ASPECT_FIT_WIDTH
        Scale scale = new Scale(50, null, Scale.Mode.ASPECT_FIT_WIDTH);
        ReductionFactor rf = new ReductionFactor(1);
        BufferedImage outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale = new Scale(null, 50, Scale.Mode.ASPECT_FIT_HEIGHT);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale = new Scale(50, 50, Scale.Mode.ASPECT_FIT_INSIDE);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // scale-by-percent
        scale = new Scale(0.25f);
        rf = new ReductionFactor(2);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(100, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    public void testSharpenImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Sharpen sharpen = new Sharpen(0.1f);
        BufferedImage outImage = Java2DUtil.sharpenImage(inImage, sharpen);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    public void testStretchContrast() {
        BufferedImage image = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        final Rectangle leftHalf = new Rectangle(0, 0, 50, 100);
        final Rectangle rightHalf = new Rectangle(50, 0, 50, 100);

        final Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.DARK_GRAY);
        g2d.fill(leftHalf);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(rightHalf);

        image = Java2DUtil.stretchContrast(image);

        assertEquals(-16777216, image.getRGB(10, 10));
        assertEquals(-1, image.getRGB(90, 90));
    }

    @Test
    public void testTransposeImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Transpose transpose = Transpose.HORIZONTAL;
        BufferedImage outImage = Java2DUtil.transposeImage(inImage, transpose);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

}
