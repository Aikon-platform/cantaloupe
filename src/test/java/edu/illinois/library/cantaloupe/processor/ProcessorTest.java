package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.resolver.FileInputStreamStreamSource;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest extends BaseTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    Format getAnySupportedSourceFormat(Processor processor) {
        for (Format format : Format.values()) {
            try {
                processor.setSourceFormat(format);
                return format;
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
        return null;
    }

    /**
     * @return Format aligning with that of {@link #getSupported16BitImage()}.
     */
    protected abstract Format getSupported16BitSourceFormat()
            throws IOException;

    /**
     * @return Supported 16-bit image file as returned by
     *         {@link TestUtil#getImage(String)}.
     */
    protected abstract Path getSupported16BitImage() throws IOException;

    protected abstract Processor newInstance();

    /* process() */

    @Test
    public void testProcessOnAllFixtures() throws Exception {
        final OperationList ops = new OperationList(
                new Identifier("cats"),
                Format.JPG,
                new Rotate(180),
                new Encode(Format.JPG));
        final Processor proc = newInstance();

        for (Format format : Format.values()) {
            try {
                // The processor will throw an exception if it doesn't support
                // this format, which is fine. No processor supports all
                // formats
                proc.setSourceFormat(format);

                for (Path fixture : TestUtil.getImageFixtures(format)) {
                    if (proc instanceof AbstractImageIOProcessor) { // TODO: why doesn't ImageIO like these?
                        if (fixture.getFileName().toString().equals("tif-rgba-monores-64x56x8-striped-jpeg.tif") ||
                                fixture.getFileName().toString().equals("tif-rgba-monores-64x56x8-tiled-jpeg.tif")) {
                            continue;
                        }
                    } else if (proc instanceof GraphicsMagickProcessor) { // TODO: why doesn't GraphicsMagickProcessor like this?
                        if (fixture.getFileName().toString().equals("jpg-rgb-594x522x8-baseline.jpg")) {
                            continue;
                        }
                    }

                    if (proc instanceof StreamProcessor) {
                        StreamSource source =
                                new FileInputStreamStreamSource(fixture);
                        ((StreamProcessor) proc).setStreamSource(source);
                    } else if (proc instanceof FileProcessor) {
                        ((FileProcessor) proc).setSourceFile(fixture.toFile());
                    }

                    try {
                        proc.process(ops, proc.readImageInfo(),
                                new NullOutputStream());
                    } catch (Exception e) {
                        System.err.println(format + " : " + fixture);
                        throw e;
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // OK, continue
            }
        }
    }

    @Test
    public void testProcessWithSupportedSourceFormatsAndNoOperations()
            throws Exception {
        doProcessTest(TestUtil.newOperationList());
    }

    @Test
    public void testProcessWithSupportedSourceFormatsAndNoOpOperations()
            throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        OperationList ops = new OperationList(new Identifier("cats"),
                Format.JPG);
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        doProcessTest(ops);
    }

    @Test
    public void testProcessWithCropOperation() throws Exception {
        List<Crop> crops = new ArrayList<>();
        Crop crop = new Crop();
        crop.setFull(true);
        crops.add(crop);

        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        crops.add(crop);

        crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        crops.add(crop);

        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        crops.add(crop);

        for (Crop crop_ : crops) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(crop_);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithNormalizeOperation() {
        // TODO: write this
    }

    @Test
    public void testProcessWithScaleOperation() throws Exception {
        List<Scale> scales = new ArrayList<>();
        Scale scale = new Scale();
        scales.add(scale);
        scale = new Scale(20, null, Scale.Mode.ASPECT_FIT_WIDTH);
        scales.add(scale);
        scale = new Scale(null, 20, Scale.Mode.ASPECT_FIT_HEIGHT);
        scales.add(scale);
        scale = new Scale(0.5f);
        scales.add(scale);
        scale = new Scale(20, 20, Scale.Mode.ASPECT_FIT_INSIDE);
        scales.add(scale);
        scale = new Scale(20, 20, Scale.Mode.NON_ASPECT_FILL);
        scales.add(scale);
        for (Scale scale_ : scales) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(scale_);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithTransposeOperation() throws Exception {
        List<Transpose> transposes = new ArrayList<>();
        transposes.add(Transpose.HORIZONTAL);
        // we aren't using this yet
        //transposes.add(new Transpose(Transpose.Axis.VERTICAL));
        for (Transpose transpose : transposes) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(transpose);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithRotateOperation() throws Exception {
        Rotate[] rotates = {
                new Rotate(0), new Rotate(15), new Rotate(275) };
        for (Rotate rotate : rotates) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(rotate);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithFilterOperation() throws Exception {
        for (ColorTransform transform : ColorTransform.values()) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(transform);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessOf16BitImageWithEncodeOperationLimitingTo8Bits()
            throws Exception {
        final Path fixture = getSupported16BitImage();
        assumeNotNull(fixture);

        final Format sourceFormat = getSupported16BitSourceFormat();

        final Encode encode = new Encode(Format.PNG);
        encode.setMaxSampleSize(8);

        OperationList ops = new OperationList(new Identifier("cats"),
                Format.PNG);
        ops.add(encode);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        doProcessTest(fixture, sourceFormat, ops, os);

        assertSampleSize(8, os.toByteArray());
    }

    @Test
    public void testProcessOf16BitImageWithEncodeOperationWithNoLimit()
            throws Exception {
        final Path fixture = getSupported16BitImage();
        assumeNotNull(fixture);

        final Format sourceFormat = getSupported16BitSourceFormat();

        final Encode encode = new Encode(Format.PNG);
        encode.setMaxSampleSize(null);

        OperationList ops = new OperationList(new Identifier("cats"), Format.PNG);
        ops.setOutputFormat(Format.PNG);
        ops.add(encode);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        doProcessTest(fixture, sourceFormat, ops, os);

        assertSampleSize(16, os.toByteArray());
    }

    @Test
    public void testProcessWithAllSupportedOutputFormats() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));
        Set<Format> outputFormats = proc.getAvailableOutputFormats();
        for (Format outputFormat : outputFormats) {
            OperationList ops = TestUtil.newOperationList();
            ops.setOutputFormat(outputFormat);
            doProcessTest(ops);
        }
    }

    /* getSupportedIIIF11Qualities() */

    /**
     * Tests for the presence of all available IIIF 1.1 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIIIF1Qualities() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                expectedQualities = new HashSet<>(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        assertEquals(expectedQualities, proc.getSupportedIIIF1Qualities());
    }

    /* getSupportedIIIF20Qualities() */

    /**
     * Tests for the presence of all available IIIF 2.0 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIIIF2Qualities() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                expectedQualities = new HashSet<>(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT));
        assertEquals(expectedQualities, proc.getSupportedIIIF2Qualities());
    }

    /* readImageInfo() */

    /**
     * This implementation is tile-unaware. Tile-aware processors will need to
     * override it.
     */
    @Test
    public void testReadImageInfoOnAllFixtures() throws Exception {
        final Processor proc = newInstance();

        for (Format format : Format.values()) {
            try {
                // The processor will throw an exception if it doesn't support
                // this format, which is fine. No processor supports all
                // formats.
                proc.setSourceFormat(format);

                for (Path fixture : TestUtil.getImageFixtures(format)) {
                    if (proc instanceof GraphicsMagickProcessor) { // TODO: why doesn't GraphicsMagickProcessor like this?
                        if (fixture.getFileName().toString().equals("jpg-rgb-594x522x8-baseline.jpg")) {
                            continue;
                        }
                    }

                    if (proc instanceof StreamProcessor) {
                        StreamProcessor sproc = (StreamProcessor) proc;
                        StreamSource streamSource =
                                new FileInputStreamStreamSource(fixture);
                        sproc.setStreamSource(streamSource);
                    } else if (proc instanceof FileProcessor) {
                        FileProcessor fproc = (FileProcessor) proc;
                        fproc.setSourceFile(fixture.toFile());
                    }

                    try {
                        // We don't know the dimensions of the source image and
                        // we can't get them because that would require using
                        // the method we are now testing, so the best we can do
                        // is to assert that they are nonzero.
                        final Info actualInfo = proc.readImageInfo();
                        assertEquals(format, actualInfo.getSourceFormat());
                        assertTrue(actualInfo.getSize().getWidth() > 0);
                        assertTrue(actualInfo.getSize().getHeight() > 0);
                    } catch (Exception e) {
                        System.err.println(format + " : " + fixture);
                        throw e;
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // OK, continue
            }
        }
    }

    /* setSourceFormat() */

    @Test
    public void testSetSourceFormatWithUnsupportedSourceFormat() {
        for (Format format : Format.values()) {
            try {
                final Processor proc = newInstance();
                proc.setSourceFormat(format);
                if (proc.getAvailableOutputFormats().isEmpty()) {
                    fail("Expected exception");
                }
            } catch (UnsupportedSourceFormatException e) {
                // pass
            }
        }
    }

    /**
     * Tests {@link Processor#process} for every one of the fixtures for every
     * source format the processor supports.
     */
    private void doProcessTest(OperationList ops) throws Exception {
        final Collection<File> fixtures = FileUtils.
                listFiles(TestUtil.getFixture("images").toFile(), null, false);

        for (Format sourceFormat : Format.values()) {
            try {
                if (newInstance().getAvailableOutputFormats().size() > 0) {
                    for (File fixture : fixtures) {
                        final String fixtureName = fixture.getName();
                        if (fixtureName.startsWith(sourceFormat.name().toLowerCase())) {
                            // Don't test 1x1 images as they are problematic
                            // with cropping & scaling.
                            if (fixtureName.contains("-1x1")) {
                                continue;
                            }
                            doProcessTest(fixture.toPath(), sourceFormat, ops);
                        }
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    /**
     * Instantiates a processor, configures it with the given arguments, and
     * tests that {@link Processor#process} writes a result without throwing
     * any exceptions.
     */
    private void doProcessTest(final Path fixture,
                               final Format sourceFormat,
                               final OperationList opList) throws Exception {
        final Processor proc = newConfiguredProcessor(fixture, sourceFormat);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            proc.process(opList, proc.readImageInfo(), outputStream);
            // TODO: verify that this is a valid image
            assertTrue(outputStream.toByteArray().length > 100);
        } catch (Exception e) {
            System.out.println("Fixture: " + fixture);
            System.out.println("Ops: " + opList);
            throw e;
        }
    }

    /**
     * Instantiates a processor, configures it with the given arguments, and
     * invokes {@link Processor#process} to write the result to the given
     * output stream.
     */
    private void doProcessTest(final Path fixture,
                               final Format sourceFormat,
                               final OperationList opList,
                               final OutputStream os) throws Exception {
        final Processor proc = newConfiguredProcessor(fixture, sourceFormat);
        try {
            proc.process(opList, proc.readImageInfo(), os);
        } catch (Exception e) {
            System.out.println("Fixture: " + fixture);
            System.out.println("Ops: " + opList);
            throw e;
        }
    }

    private Processor newConfiguredProcessor(Path fixture, Format sourceFormat)
            throws UnsupportedSourceFormatException {
        Processor proc = newInstance();
        proc.setSourceFormat(sourceFormat);

        if (proc instanceof FileProcessor) {
            ((FileProcessor) proc).setSourceFile(fixture.toFile());
        } else if (proc instanceof StreamProcessor) {
            StreamSource source = new FileInputStreamStreamSource(fixture);
            ((StreamProcessor) proc).setStreamSource(source);
        }
        return proc;
    }

}
