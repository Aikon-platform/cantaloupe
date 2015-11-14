package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.restlet.data.Form;

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

    public void testGetSupportedQualities() {
        Set<Quality> expectedQualities = new HashSet<>();
        //expectedQualities.add(Quality.BITONAL);
        expectedQualities.add(Quality.COLOR);
        expectedQualities.add(Quality.DEFAULT);
        expectedQualities.add(Quality.GRAY);
        assertEquals(expectedQualities,
                instance.getSupportedQualities(getAnySupportedSourceFormat(instance)));

        expectedQualities = new HashSet<>();
        assertEquals(expectedQualities,
                instance.getSupportedQualities(SourceFormat.UNKNOWN));
    }

    public void testProcessWithFrameOption() throws Exception {
        final Parameters params = new Parameters("bla", "full", "full", "0",
                "default", "jpg");
        final SourceFormat sourceFormat = SourceFormat.MPG;

        // frame option missing
        FileProcessor proc = (FileProcessor) getProcessor();
        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
        Dimension size = proc.getSize(file, sourceFormat);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        proc.process(params, sourceFormat, size, file, outputStream);
        byte[] zeroSecondFrame = outputStream.toByteArray();

        // frame option present
        final Form urlQuery = new Form();
        urlQuery.add("time", "00:00:05");
        outputStream = new ByteArrayOutputStream();
        params.setQuery(urlQuery);
        proc.process(params, sourceFormat, size, file, outputStream);
        byte[] fiveSecondFrame = outputStream.toByteArray();

        assertFalse(Arrays.equals(zeroSecondFrame, fiveSecondFrame));
    }

}
