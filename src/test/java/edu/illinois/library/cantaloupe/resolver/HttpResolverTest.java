package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

abstract class HttpResolverTest extends BaseTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    WebServer server;

    private HttpResolver instance;
    private RequestContext context;

    /**
     * Subclasses need to override, call super, and set
     * {@link Key#HTTPRESOLVER_URL_PREFIX} to the web server URI using the
     * appropriate scheme.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context = new RequestContext();
        instance = new HttpResolver();
        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        instance.setContext(context);

        useBasicLookupStrategy();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();

        server.stop();
    }

    abstract String getScheme();

    abstract URI getServerURI();

    private void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    private void useScriptLookupStrategy() throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
    }

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithValidAuthentication()
            throws Exception {
        useBasicLookupStrategy();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME,
                WebServer.BASIC_USER);
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_SECRET,
                WebServer.BASIC_SECRET);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        assertNotNull(instance.newStreamSource());
    }

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithInvalidAuthentication()
            throws Exception {
        useBasicLookupStrategy();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME,
                WebServer.BASIC_USER);
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_SECRET,
                "bogus");

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        try {
            instance.newStreamSource();
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            // pass
        }
    }

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithPresentReadableImage() {
        useBasicLookupStrategy();
        doTestNewStreamSourceWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithMissingImage() {
        useBasicLookupStrategy();
        doTestNewStreamSourceWithMissingImage(new Identifier("bogus"));
    }

    @Test
    public void testNewStreamSourceWithUsingBasicLookupStrategyPresentUnreadableImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestNewStreamSourceWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithValidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("valid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(identifier);
        assertNotNull(instance.newStreamSource());
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithInvalidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("invalid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(identifier);
        try {
            instance.newStreamSource();
        } catch (AccessDeniedException e) {
            // pass
        }
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestNewStreamSourceWithPresentReadableImage(identifier);
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/bogus");
        doTestNewStreamSourceWithMissingImage(identifier);
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/gif");
        doTestNewStreamSourceWithPresentUnreadableImage(identifier);
    }

    private void doTestNewStreamSourceWithPresentReadableImage(
            Identifier identifier) {
        try {
            server.start();

            instance.setIdentifier(identifier);
            assertNotNull(instance.newStreamSource());
        } catch (Exception e) {
            fail();
        }
    }

    private void doTestNewStreamSourceWithPresentUnreadableImage(Identifier identifier) {
        try {
            server.start();

            File image = TestUtil.getImage("gif");
            try {
                image.setReadable(false);
                instance.setIdentifier(identifier);
                instance.newStreamSource();
                fail("Expected exception");
            } finally {
                image.setReadable(true);
            }
        } catch (AccessDeniedException e) {
            // pass
        } catch (Exception e) {
            fail("Expected AccessDeniedException");
        }
    }

    private void doTestNewStreamSourceWithMissingImage(Identifier identifier) {
        try {
            server.start();

            instance.setIdentifier(identifier);
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (Exception e) {
            fail("Expected FileNotFoundException");
        }
    }

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithValidAuthentication()
            throws Exception {
        useBasicLookupStrategy();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME,
                WebServer.BASIC_USER);
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_SECRET,
                WebServer.BASIC_SECRET);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithInvalidAuthentication()
            throws Exception {
        useBasicLookupStrategy();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME,
                WebServer.BASIC_USER);
        config.setProperty(Key.HTTPRESOLVER_BASIC_AUTH_SECRET,
                "bogus");

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        try {
            instance.newStreamSource();
        } catch (AccessDeniedException e) {
            // pass
        }
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestGetSourceFormatWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestGetSourceFormatWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestGetSourceFormatWithMissingImage(new Identifier("bogus"));
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestGetSourceFormatWithPresentReadableImage(identifier);
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/gif");
        doTestGetSourceFormatWithPresentUnreadableImage(identifier);
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        doTestGetSourceFormatWithMissingImage(new Identifier("bogus"));
    }

    @Test
    public void testGetSourceFormatFollowsRedirect() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {
                if (baseRequest.getPathInfo().startsWith("/" + PRESENT_READABLE_IDENTIFIER)) {
                    response.setStatus(301);
                    response.setHeader("Location",
                            getServerURI().resolve("/jpg-rgb-64x56x8-line.jpg").toString());
                }
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithErrorResponse() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {
                response.setStatus(500);
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try {
            instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("500"));
        }
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndContentTypeHeader()
            throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {
                response.setHeader("Content-Type", "image/jpeg");
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndNoContentTypeHeader()
            throws Exception {
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndUnrecognizedContentTypeHeaderValue()
            throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {
                response.setHeader("Content-Type", "image/bogus");
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    private void doTestGetSourceFormatWithPresentReadableImage(
            Identifier identifier) {
        try {
            server.start();

            instance.setIdentifier(identifier);
            assertEquals(Format.JPG, instance.getSourceFormat());
        } catch (Exception e) {
            fail();
        }
    }

    private void doTestGetSourceFormatWithPresentUnreadableImage(
            Identifier identifier) {
        try {
            server.start();

            File image = TestUtil.getImage("gif");
            try {
                image.setReadable(false);
                instance.setIdentifier(identifier);
                instance.getSourceFormat();
                fail("Expected exception");
            } finally {
                image.setReadable(true);
            }
        } catch (AccessDeniedException e) {
            // pass
        } catch (Exception e) {
            fail("Expected AccessDeniedException");
        }
    }

    private void doTestGetSourceFormatWithMissingImage(Identifier identifier) {
        try {
            server.start();

            assertEquals(Format.JPG, instance.getSourceFormat());
            instance.setIdentifier(identifier);
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (Exception e) {
            fail("Expected FileNotFoundException");
        }
    }

    /* getResourceInfo() */

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        useBasicLookupStrategy();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                getScheme() + "://example.org/prefix/");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id",
                instance.getResourceInfo().getURI().toString());
    }

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        useBasicLookupStrategy();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                getScheme() + "://example.org/prefix/");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "/suffix");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id/suffix",
                instance.getResourceInfo().toString());
    }

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        useBasicLookupStrategy();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX, "");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "");

        server.start();

        instance.setIdentifier(new Identifier(getScheme() + "://example.org/images/image.jpg"));
        assertEquals(getScheme() + "://example.org/images/image.jpg",
                instance.getResourceInfo().toString());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningString()
            throws Exception {
        useScriptLookupStrategy();

        server.start();

        instance.setIdentifier(new Identifier(getScheme() + "-" + PRESENT_READABLE_IDENTIFIER));
        assertEquals(new URI(getScheme() + "://example.org/bla/" + getScheme() + "-" + PRESENT_READABLE_IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyWithContextReturningString()
            throws Exception {
        useScriptLookupStrategy();

        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Proto", getScheme());
        context.setClientIP("1.2.3.4");
        context.setRequestHeaders(headers);

        server.start();

        assertEquals(new URI(getScheme() + "://other-example.org/bleh/" + PRESENT_READABLE_IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningHash()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(getScheme() + "-jpg-rgb-64x56x8-plane.jpg");
        instance.setIdentifier(identifier);

        server.start();

        HttpResolver.ResourceInfo actual = instance.getResourceInfo();
        assertEquals(new URI(getScheme() + "://example.org/bla/" + identifier),
                actual.getURI());
        assertEquals("username", actual.getUsername());
        assertEquals("secret", actual.getSecret());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningNil()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        instance.setIdentifier(identifier);

        server.start();

        try {
            instance.getResourceInfo();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
