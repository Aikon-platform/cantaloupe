package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AccessDeniedException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

public class HttpResolverTest extends BaseTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private static WebServer server;

    private HttpResolver instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new WebServer();
        server.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stop();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://localhost:" + server.getPort() + "/");

        instance = new HttpResolver();
        instance.setIdentifier(IDENTIFIER);
    }

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceWithPresentReadableImage() {
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testNewStreamSourceWithMissingImage() {
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
    @Ignore // TODO: restlet bug: https://github.com/restlet/restlet-framework-java/issues/1179
    public void testNewStreamSourceWithPresentUnreadableImage()
            throws IOException {
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

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormat() throws Exception {
        assertEquals(Format.JPG, instance.getSourceFormat());
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    /* getURI() */

    @Test
    public void testGetURIWithBasicLookupStrategyWithPrefix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://example.org/prefix/");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id",
                instance.getURI().toString());
    }

    @Test
    public void testGetURIWithBasicLookupStrategyWithPrefixAndSuffix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://example.org/prefix/");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "/suffix");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getURI().toString());
    }

    @Test
    public void testGetURIWithBasicLookupStrategyWithoutPrefixOrSuffix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX, "");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "");
        instance.setIdentifier(new Identifier("http://example.org/images/image.jpg"));
        assertEquals("http://example.org/images/image.jpg",
                instance.getURI().toString());
    }

    @Test
    public void testGetURIWithScriptLookupStrategy() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals(new URI("http://example.org/bla/" + IDENTIFIER),
                instance.getURI());
    }

}
