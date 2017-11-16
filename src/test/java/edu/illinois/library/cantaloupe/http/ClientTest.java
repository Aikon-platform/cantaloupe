package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class ClientTest extends BaseTest {

    private Client instance;
    private WebServer webServer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        instance = new Client();
        instance.setTrustAll(true);

        webServer = new WebServer();

        assertEquals(Method.GET, instance.getMethod());
        assertEquals(Transport.HTTP1_1, instance.getTransport());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        instance.stop();
        if (webServer != null) {
            webServer.stop();
        }
    }

    private URI getValidHTTPURI() {
        return webServer.getHTTPURI().resolve("/jpg");
    }

    private URI getValidHTTPSURI() {
        return webServer.getHTTPSURI().resolve("/jpg");
    }

    /* builder() */

    @Test
    public void testBuilder() throws Exception {
        URI uri = new URI("http://example.org/cats");
        instance.builder().uri(uri);
        assertEquals(uri, instance.getURI());
    }

    /* send() */

    @Test
    public void testSendWithValidBasicAuthCredentialsAndCorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm(WebServer.BASIC_REALM);
        instance.setUsername(WebServer.BASIC_USER);
        instance.setSecret(WebServer.BASIC_SECRET);

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        ContentResponse response = instance.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSendWithValidBasicAuthCredentialsAndIncorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm("bogus");
        instance.setUsername(WebServer.BASIC_USER);
        instance.setSecret(WebServer.BASIC_SECRET);

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try {
            instance.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testSendWithInvalidBasicAuthCredentialsAndCorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm(WebServer.BASIC_REALM);
        instance.setUsername("bogus");
        instance.setSecret("bogus");

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try {
            instance.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testSendWithInvalidBasicAuthCredentialsAndIncorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm("bogus");
        instance.setUsername("bogus");
        instance.setSecret("bogus");

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try {
            instance.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testSendWorksWithInsecureHTTP1_1() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP1_1);
        instance.setURI(getValidHTTPURI());

        ContentResponse response = instance.send();
        assertEquals(200, response.getStatus());
        assertEquals(HttpVersion.HTTP_1_1, response.getVersion());
    }

    @Test
    public void testSendWorksWithInsecureHTTP2() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP2_0);
        instance.setURI(getValidHTTPURI());

        ContentResponse response = instance.send();
        assertEquals(200, response.getStatus());
        assertEquals(HttpVersion.HTTP_2, response.getVersion());
    }

    @Test
    public void testSendWorksWithSecureHTTP1_1() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP1_1);
        instance.setURI(getValidHTTPSURI());

        ContentResponse response = instance.send();
        assertEquals(200, response.getStatus());
        assertEquals(HttpVersion.HTTP_1_1, response.getVersion());
    }

    @Test
    public void testSendWorksWithSecureHTTP2() throws Exception {
        assumeTrue(SystemUtils.isALPNAvailable());

        webServer.start();

        instance.setTransport(Transport.HTTP2_0);
        instance.setURI(getValidHTTPSURI());

        ContentResponse response = instance.send();
        assertEquals(200, response.getStatus());
        assertEquals(HttpVersion.HTTP_2, response.getVersion());
    }

}
