package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.*;

public class ProcessorUtilTest {

    @Test
    public void testGetReductionFactor() {
        assertEquals(new ReductionFactor(0), ProcessorUtil.getReductionFactor(0.75f, 5));
        assertEquals(new ReductionFactor(1), ProcessorUtil.getReductionFactor(0.5f, 5));
        assertEquals(new ReductionFactor(1), ProcessorUtil.getReductionFactor(0.45f, 5));
        assertEquals(new ReductionFactor(2), ProcessorUtil.getReductionFactor(0.25f, 5));
        assertEquals(new ReductionFactor(2), ProcessorUtil.getReductionFactor(0.2f, 5));
        assertEquals(new ReductionFactor(3), ProcessorUtil.getReductionFactor(0.125f, 5));
        assertEquals(new ReductionFactor(4), ProcessorUtil.getReductionFactor(0.0625f, 5));
        assertEquals(new ReductionFactor(5), ProcessorUtil.getReductionFactor(0.03125f, 5));
        // max
        assertEquals(new ReductionFactor(1), ProcessorUtil.getReductionFactor(0.2f, 1));
    }

    @Test
    public void testGetScale() {
        final double fudge = 0.0000001f;
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(0))) - Math.abs(1.0f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(1))) - Math.abs(0.5f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(2))) - Math.abs(0.25f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(3))) - Math.abs(0.125f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(4))) - Math.abs(0.0625f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(5))) - Math.abs(0.03125f) < fudge);
    }

    @Test
    public void testGetSizeWithFile() throws Exception {
        Dimension expected = new Dimension(64, 56);
        Dimension actual = ProcessorUtil.getSize(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"),
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSizeWithInputStream() throws Exception {
        Dimension expected = new Dimension(64, 56);
        StreamSource streamSource = new TestStreamSource(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        Dimension actual = ProcessorUtil.getSize(streamSource,
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetWatermarkOpacity() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        final float fudge = 0.00000001f;
        // null value
        config.setProperty(Processor.WATERMARK_OPACITY_CONFIG_KEY, null);
        assertEquals(1, ProcessorUtil.getWatermarkOpacity(), fudge);
        // empty value
        config.setProperty(Processor.WATERMARK_OPACITY_CONFIG_KEY, "");
        assertEquals(1, ProcessorUtil.getWatermarkOpacity(), fudge);
        // invalid value
        config.setProperty(Processor.WATERMARK_OPACITY_CONFIG_KEY, "bogus");
        assertEquals(1, ProcessorUtil.getWatermarkOpacity(), fudge);
        // valid value
        config.setProperty(Processor.WATERMARK_OPACITY_CONFIG_KEY, "0.4f");
        assertEquals(0.4f, ProcessorUtil.getWatermarkOpacity(), fudge);
    }

    @Test
    public void testGetWatermarkPosition() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // null value
        config.setProperty(Processor.WATERMARK_POSITION_CONFIG_KEY, null);
        assertNull(ProcessorUtil.getWatermarkPosition());
        // empty value
        config.setProperty(Processor.WATERMARK_POSITION_CONFIG_KEY, "");
        assertNull(ProcessorUtil.getWatermarkPosition());
        // invalid value
        config.setProperty(Processor.WATERMARK_POSITION_CONFIG_KEY, "bogus");
        assertNull(ProcessorUtil.getWatermarkPosition());
        // valid value
        config.setProperty(Processor.WATERMARK_POSITION_CONFIG_KEY, "top left");
        assertEquals(Position.TOP_LEFT, ProcessorUtil.getWatermarkPosition());
    }

}
