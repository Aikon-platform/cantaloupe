package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * This class is divided into two sections: one for ImageInfo and one for
 * ImageInfo.Image.
 */
public class ImageInfoTest {

    private ImageInfo instance;

    @Before
    public void setUp() {
        instance = new ImageInfo(100, 80, Format.JPG);
        instance.getImages().get(0).setOrientation(Orientation.ROTATE_270);
    }

    /************************ ImageInfo tests ****************************/

    @Test
    public void testFromJsonWithFile() throws Exception {
        // TODO: write this
    }

    @Test
    public void testFromJsonWithInputStream() throws Exception {
        String json = instance.toJson();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        ImageInfo info = ImageInfo.fromJson(inputStream);
        assertEquals(info.toString(), instance.toString());
    }

    @Test
    public void testFromJsonWithString() throws Exception {
        String json = instance.toJson();
        ImageInfo info = ImageInfo.fromJson(json);
        assertEquals(info.toString(), instance.toString());
    }

    @Test
    public void testEquals() {
        // equal
        ImageInfo info1 = new ImageInfo(100, 80, 50, 40, Format.JPG);
        ImageInfo info2 = new ImageInfo(100, 80, 50, 40, Format.JPG);
        assertTrue(info1.equals(info2));
        // not equal
        info2 = new ImageInfo(99, 80, 50, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 79, 50, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, 49, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, 50, 39, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, 50, 40, Format.TIF);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, Format.JPG);
        assertFalse(info1.equals(info2));
    }

    @Test
    public void testGetImages() {
        assertEquals(1, instance.getImages().size());
        assertEquals(0, new ImageInfo().getImages().size());
    }

    @Test
    public void testGetOrientation() {
        assertEquals(Orientation.ROTATE_270, instance.getOrientation());
    }

    @Test
    public void testGetOrientationSize() {
        ImageInfo.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), instance.getOrientationSize());
    }

    @Test
    public void testGetSize() {
        assertEquals(new Dimension(100, 80), instance.getSize());
    }

    @Test
    public void testGetSizeWithIndex() {
        ImageInfo.Image image = new ImageInfo.Image();
        image.width = 50;
        image.height = 40;
        instance.getImages().add(image);

        image = new ImageInfo.Image();
        image.width = 25;
        image.height = 20;
        instance.getImages().add(image);

        assertEquals(new Dimension(25, 20), instance.getSize(2));
    }

    @Test
    public void testGetSourceFormat() {
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    @Test
    public void testToJson() {
        // tested in testFromJson()
    }

    @Test
    public void testToString() throws Exception {
        assertEquals(instance.toJson(), instance.toString());
    }

    @Test
    public void testWriteAsJson() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJson(baos);
        assertTrue(Arrays.equals(baos.toByteArray(),
                instance.toJson().getBytes()));
    }

    /********************* ImageInfo.Image tests *************************/

    @Test
    public void testImageGetOrientationSize() {
        ImageInfo.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationSize());
    }

    @Test
    public void testImageGetOrientationTileSize() {
        ImageInfo.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationTileSize());
    }

}
