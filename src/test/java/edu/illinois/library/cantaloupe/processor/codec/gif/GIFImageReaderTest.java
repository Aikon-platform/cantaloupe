package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageReaderTest;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GIFImageReaderTest extends AbstractImageReaderTest {

    @Override
    protected GIFImageReader newInstance() throws IOException {
        GIFImageReader reader = new GIFImageReader();
        reader.setSource(TestUtil.getImage("gif"));
        return reader;
    }

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((GIFImageReader) instance).
                getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.gif.GIFImageReader", impls[0]);
    }

    @Test
    @Override
    public void testGetCompression() throws IOException {
        assertEquals(Compression.LZW, instance.getCompression(0));
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((GIFImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((GIFImageReader) instance).
                getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((AbstractIIOImageReader) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((GIFImageReader) instance).getUserPreferredIIOImplementation());
    }

    /* read() */

    @Test
    public void testReadWithArguments() throws Exception {
        OperationList ops = new OperationList(
                new CropByPixels(10, 10, 40, 40),
                new ScaleByPixels(35, 35, ScaleByPixels.Mode.ASPECT_FIT_INSIDE));

        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = new HashSet<>();

        BufferedImage image = instance.read(ops, rf, hints);

        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
        assertEquals(0, rf.factor);
        assertFalse(hints.contains(ReaderHint.ALREADY_CROPPED));
    }

    /* readSequence() */

    @Test
    @Override
    public void testReadSequence() {}

    @Test
    public void testReadSequenceWithStaticImage() throws Exception {
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(1, seq.length());
    }

    @Test
    public void testReadSequenceWithAnimatedImage() throws Exception {
        instance = new GIFImageReader();
        instance.setSource(TestUtil.getImage("gif-animated-looping.gif"));
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(2, seq.length());
    }

}
