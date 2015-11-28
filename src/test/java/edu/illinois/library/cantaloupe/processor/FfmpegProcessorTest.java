package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotation;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.test.TestUtil;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * For this to work, the ffmpeg and ffprobe binaries must be on the PATH.
 */
public class FfmpegProcessorTest extends ProcessorTest {

    FfmpegProcessor instance = new FfmpegProcessor();

    protected Processor getProcessor() {
        return instance;
    }

    public void testGetAvailableOutputFormats() throws IOException {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> expectedFormats = new HashSet<>();
            if (sourceFormat.getType() != null &&
                    sourceFormat.getType().equals(SourceFormat.Type.VIDEO)) {
                expectedFormats.add(OutputFormat.JPG);
            }
            assertEquals(expectedFormats,
                    instance.getAvailableOutputFormats(sourceFormat));
        }
    }

    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    public void testGetSupportedFeatures() {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        //expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(getAnySupportedSourceFormat(instance)));

        expectedFeatures = new HashSet<>();
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(SourceFormat.UNKNOWN));
    }

    public void testProcessWithFrameOption() throws Exception {
        Identifier identifier = new Identifier("bla");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;

        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        final SourceFormat sourceFormat = SourceFormat.MPG;

        // time option missing
        FileProcessor proc = (FileProcessor) getProcessor();
        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
        Dimension size = proc.getSize(file, sourceFormat);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        proc.process(ops, sourceFormat, size, file, outputStream);
        byte[] zeroSecondFrame = outputStream.toByteArray();

        // time option present
        ops.getOptions().put("time", "00:00:05");
        outputStream = new ByteArrayOutputStream();
        proc.process(ops, sourceFormat, size, file, outputStream);
        byte[] fiveSecondFrame = outputStream.toByteArray();

        assertFalse(Arrays.equals(zeroSecondFrame, fiveSecondFrame));
    }

    @Override
    public void testGetSupportedIiif11Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.GRAY);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.NATIVE);
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif1_1Qualities(getAnySupportedSourceFormat(getProcessor())));

        expectedQualities = new HashSet<>();
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif1_1Qualities(SourceFormat.UNKNOWN));
    }

    @Override
    public void testGetSupportedIiif20Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.DEFAULT);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.GRAY);
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif2_0Qualities(getAnySupportedSourceFormat(getProcessor())));

        expectedQualities = new HashSet<>();
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif1_1Qualities(SourceFormat.UNKNOWN));
    }

}
