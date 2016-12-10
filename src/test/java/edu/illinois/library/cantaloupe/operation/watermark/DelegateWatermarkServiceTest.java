package edu.illinois.library.cantaloupe.operation.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DelegateWatermarkServiceTest {

    private DelegateWatermarkService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY, true);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

        instance = new DelegateWatermarkService();
    }

    @Test
    public void testGetWatermarkReturningImageWatermark() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("image"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        final ImageWatermark watermark =
                (ImageWatermark) instance.getWatermark(
                        opList, fullSize, requestUrl, requestHeaders, clientIp,
                        cookies);
        assertEquals(new File("/dev/cats"), watermark.getImage());
        assertEquals((long) 5, watermark.getInset());
        assertEquals(Position.BOTTOM_LEFT, watermark.getPosition());
    }

    @Test
    public void testGetWatermarkReturningStringWatermark() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("string"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        final StringWatermark watermark =
                (StringWatermark) instance.getWatermark(
                        opList, fullSize, requestUrl, requestHeaders, clientIp,
                        cookies);
        assertEquals("dogs\ndogs", watermark.getString());
        assertEquals((long) 5, watermark.getInset());
        assertEquals(Position.BOTTOM_LEFT, watermark.getPosition());
        assertEquals(Color.red, watermark.getColor());
        assertEquals(Color.blue, watermark.getStrokeColor());
        assertEquals(3, watermark.getStrokeWidth(), 0.00001f);
    }

    @Test
    public void testGetWatermarkReturningFalse() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("bogus"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = instance.getWatermark(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertNull(watermark);
    }

}
