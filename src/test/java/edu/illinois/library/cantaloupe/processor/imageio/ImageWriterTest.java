package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageWriterTest extends BaseTest {

    private ImageWriter instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageWriter(null);
    }

    @Test
    public void testSupportedFormats() {
        Set<Format> outputFormats = new HashSet<>(Arrays.asList(
                Format.GIF, Format.JPG, Format.PNG, Format.TIF));
        assertEquals(outputFormats, ImageWriter.supportedFormats());
    }

    @Test
    public void testWriteImage() {
        // TODO: write this
    }

}
