package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AccessDeniedException;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.data.Reference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpResolverTest extends BaseTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private HttpResolver instance;
    private WebServer server;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        server = new WebServer();
        server.start();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY,
                "http://localhost:" + server.getPort() + "/");

        instance = new HttpResolver();
        instance.setIdentifier(IDENTIFIER);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    public void testNewStreamSourceWithPresentReadableImage() throws IOException {
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testNewStreamSourceWithMissingImage() throws IOException {
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    @Ignore // TODO: possible restlet bug: https://github.com/restlet/restlet-framework-java/issues/1179
    public void testNewStreamSourceWithPresentUnreadableImage() throws IOException {
        File image = TestUtil.getFixture("gif");
        try {
            image.setReadable(false);
            instance.setIdentifier(new Identifier("gif"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            // pass
        } finally {
            image.setReadable(true);
        }
    }

    @Test
    public void testGetStreamWithHttpsSource() throws IOException {
        // TODO: write this
    }

    @Test
    public void testGetSourceFormat() throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetUrlWithBasicLookupStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();

        // with prefix
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY,
                "http://example.org/prefix/");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id",
                instance.getUrl().toString());
        // with suffix
        config.setProperty(HttpResolver.URL_SUFFIX_CONFIG_KEY, "/suffix");
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getUrl().toString());
        // without prefix or suffix
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY, "");
        config.setProperty(HttpResolver.URL_SUFFIX_CONFIG_KEY, "");
        instance.setIdentifier(new Identifier("http://example.org/images/image.jpg"));
        assertEquals("http://example.org/images/image.jpg",
                instance.getUrl().toString());
    }

    @Test
    public void testGetUrlWithScriptLookupStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals(new Reference("http://example.org/bla/" + IDENTIFIER),
                instance.getUrl());
    }

}
