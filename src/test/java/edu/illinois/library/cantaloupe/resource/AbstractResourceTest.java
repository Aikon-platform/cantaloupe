package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.overlay.BasicStringOverlayServiceTest;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionServiceTest;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class AbstractResourceTest extends BaseTest {

    private static class TestResource extends AbstractResource {
    }

    private TestResource resource = new TestResource();

    @Test
    public void testAddNonEndpointOperations() throws IOException {
        final Configuration config = Configuration.getInstance();

        //////////////////////////// Setup ////////////////////////////////

        // redactions
        RedactionServiceTest.setUpConfiguration();
        // overlay
        BasicStringOverlayServiceTest.setUpConfiguration();
        // scale filters
        config.setProperty(Processor.DOWNSCALE_FILTER_CONFIG_KEY, "bicubic");
        config.setProperty(Processor.UPSCALE_FILTER_CONFIG_KEY, "triangle");
        // sharpening
        config.setProperty(Processor.SHARPEN_CONFIG_KEY, 0.2f);
        // metadata copies
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        // background color
        config.setProperty(Processor.BACKGROUND_COLOR_CONFIG_KEY, "white");
        // JPEG quality
        config.setProperty(Processor.JPG_QUALITY_CONFIG_KEY, 50);
        // TIFF compression
        config.setProperty(Processor.TIF_COMPRESSION_CONFIG_KEY, "LZW");

        ///////////////////////////// Test ////////////////////////////////

        final OperationList opList = TestUtil.newOperationList();
        opList.add(new Scale(0.5f));
        final Dimension fullSize = new Dimension(2000,1000);

        resource.addNonEndpointOperations(opList, fullSize);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
        assertTrue(it.next() instanceof Sharpen);
        assertTrue(it.next() instanceof MetadataCopy);

        assertEquals("white",
                opList.getOptions().get(Processor.BACKGROUND_COLOR_CONFIG_KEY));
        assertEquals(50,
                opList.getOptions().get(Processor.JPG_QUALITY_CONFIG_KEY));
        assertEquals("LZW",
                opList.getOptions().get(Processor.TIF_COMPRESSION_CONFIG_KEY));
    }

    @Test
    public void testAddNonEndpointOperationsFreezesOperationList()
            throws IOException {
        final OperationList opList = TestUtil.newOperationList();
        final Dimension fullSize = new Dimension(2000,1000);

        resource.addNonEndpointOperations(opList, fullSize);

        try {
            opList.iterator().remove();
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }

        try {
            opList.getOptions().put("test", "test");
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

}
