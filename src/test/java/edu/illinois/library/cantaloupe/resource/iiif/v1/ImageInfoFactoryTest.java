package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageInfoFactoryTest {

    private String imageUri;
    private ImageInfo info;
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        Configuration config = new BaseConfiguration();
        config.setProperty("processor.fallback", "Java2dProcessor");
        config.setProperty("endpoint.iiif.default_tile_size", 256);
        Application.setConfiguration(config);

        imageUri = "http://example.org/bla";
        processor = ProcessorFactory.getProcessor(SourceFormat.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));
        info = ImageInfoFactory.newImageInfo(imageUri, processor);
    }

    @Test
    public void testNewImageInfoContext() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/context.json", info.context);
    }

    @Test
    public void testNewImageInfoId() {
        assertEquals("http://example.org/bla", info.id);
    }

    @Test
    public void testNewImageInfoWidth() {
        assertEquals(594, (long) info.width);
    }

    @Test
    public void testNewImageInfoHeight() {
        assertEquals(522, (long) info.height);
    }

    @Test
    public void testNewImageInfoScaleFactors() {
        assertEquals(5, info.scaleFactors.size());
        assertEquals(1, (long) info.scaleFactors.get(0));
        assertEquals(2, (long) info.scaleFactors.get(1));
        assertEquals(4, (long) info.scaleFactors.get(2));
        assertEquals(8, (long) info.scaleFactors.get(3));
        assertEquals(16, (long) info.scaleFactors.get(4));
    }

    @Test
    public void testNewImageInfoTileWidthWithUntiledImage() {
        assertEquals(297, (long) info.tileWidth);
    }

    @Test
    public void testNewImageInfoTileWidthWithTiledImage() throws Exception {
        processor.setSourceFormat(SourceFormat.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"));
        info = ImageInfoFactory.newImageInfo(imageUri, processor);

        assertEquals(16, (long) info.tileWidth);
    }

    @Test
    public void testNewImageInfoTileHeightWithUntiledImage() {
        assertEquals(261, (long) info.tileHeight);
    }

    @Test
    public void testNewImageInfoTileHeightWithTiledImage() throws Exception {
        processor.setSourceFormat(SourceFormat.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"));
        info = ImageInfoFactory.newImageInfo(imageUri, processor);

        assertEquals(16, (long) info.tileHeight);
    }

    @Test
    public void testNewImageInfoFormats() {
        assertTrue(info.formats.contains("jpg"));
    }

    @Test
    public void testNewImageInfoQualities() {
        assertTrue(info.qualities.contains("color"));
    }

    @Test
    public void testNewImageInfoProfile() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                info.profile);
    }

}
