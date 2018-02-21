package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.ApplicationServer;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeInputStreamCache;
import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeOutputStreamCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.http.Transport;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resolver.PathStreamSource;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Collection of tests shareable between major versions of IIIF Image and
 * Information endpoints.
 */
public class ImageAPIResourceTester {

    static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private static final String BASIC_AUTH_USER = "user";
    private static final String BASIC_AUTH_SECRET = "secret";

    Client newClient(URI uri) {
        return new Client().builder().uri(uri).build();
    }

    public void testBasicAuthWithNoCredentials(ApplicationServer appServer,
                                               URI uri) throws Exception {
        initializeBasicAuth(appServer);
        try {
            assertStatus(401, uri);
        } finally {
            uninitializeBasicAuth(appServer);
        }
    }

    public void testBasicAuthWithInvalidCredentials(ApplicationServer appServer,
                                                    URI uri) throws Exception {
        initializeBasicAuth(appServer);
        try {
            Client client = newClient(uri);
            client.setRealm(RestletApplication.PUBLIC_REALM);
            client.setUsername("invalid");
            client.setSecret("invalid");
            try {
                client.send();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(401, e.getStatusCode());
            } finally {
                client.stop();
            }
        } finally {
            uninitializeBasicAuth(appServer);
        }
    }

    public void testBasicAuthWithValidCredentials(ApplicationServer appServer,
                                                  URI uri) throws Exception {
        initializeBasicAuth(appServer);
        try {
            Client client = newClient(uri);
            client.setRealm(RestletApplication.PUBLIC_REALM);
            client.setUsername(BASIC_AUTH_USER);
            client.setSecret(BASIC_AUTH_SECRET);
            try {
                Response response = client.send();
                assertEquals(200, response.getStatus());
            } finally {
                client.stop();
            }
        } finally {
            uninitializeBasicAuth(appServer);
        }
    }

    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(URI uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try {
            Response response = client.send();

            String header = response.getHeaders().getFirstValue("Cache-Control");
            assertTrue(header.contains("max-age=1234"));
            assertTrue(header.contains("s-maxage=4567"));
            assertTrue(header.contains("public"));
            assertTrue(header.contains("no-transform"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(URI uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertNull(e.getResponse().getHeaders().get("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    /**
     * Tests that there is no Cache-Control header returned when
     * cache.httpClient.enabled = true but a cache=false argument is present in the
     * URL query.
     */
    public void testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(URI uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertNull(response.getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsDisabled(URI uri)
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, false);

        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertNull(response.getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    public void testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(URI uri)
            throws Exception {
        Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);

        // request an info
        Client client = newClient(uri);
        try {
            client.send();

            Thread.sleep(1000); // it may write asynchronously

            // assert that neither the image nor the info exists in the
            // derivative cache
            assertRecursiveFileCount(cacheDir, 0);

            // assert that the info does NOT exist in the info cache
            assertEquals(0, InfoService.getInstance().getInfoCache().size());
        } finally {
            client.stop();
        }
    }

    public void testHTTP2(URI uri) throws Exception {
        Client client = newClient(uri);
        try {
            client.setTransport(Transport.HTTP2_0);
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    public void testHTTPS1_1(URI uri) throws Exception {
        Client client = newClient(uri);
        try {
            client.setTrustAll(true);
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    public void testHTTPS2(URI uri) throws Exception {
        assumeTrue(SystemUtils.isALPNAvailable());
        Client client = newClient(uri);
        try {
            client.setTransport(Transport.HTTP2_0);
            client.setTrustAll(true);
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    public void testNotFound(URI uri) {
        assertStatus(404, uri);
    }

    /**
     * Tests recovery from an exception thrown by
     * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache#newDerivativeImageInputStream}.
     */
    public void testRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException(URI uri)
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE,
                MockBrokenDerivativeInputStreamCache.class.getSimpleName());
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        client.send();
    }

    /**
     * Tests recovery from an exception thrown by
     * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache#newDerivativeImageInputStream}.
     */
    public void testRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException(URI uri)
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE,
                MockBrokenDerivativeOutputStreamCache.class.getSimpleName());
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        client.send();
    }

    /**
     * Used by {@link #testResolverCheckAccessNotCalledWithSourceCacheHit}.
     */
    public static class NotCheckingAccessResolver implements StreamResolver {

        @Override
        public void checkAccess() throws IOException {
            throw new IOException("checkAccess called!");
        }

        @Override
        public Format getSourceFormat() {
            return Format.JPG;
        }

        @Override
        public StreamSource newStreamSource() throws IOException {
            return new PathStreamSource(TestUtil.getImage("jpg"));
        }

        @Override
        public void setIdentifier(Identifier identifier) {}

        @Override
        public void setContext(RequestContext context) {}

    }

    public void testResolverCheckAccessNotCalledWithSourceCacheHit(Identifier identifier,
                                                                   URI uri) throws Exception {
        // Set up the environment to use the source cache, not resolve first,
        // and use a non-FileResolver.
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
        config.setProperty(Key.RESOLVER_STATIC,
                NotCheckingAccessResolver.class.getName());
        config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
        config.setProperty(Key.SOURCE_CACHE_TTL, 10);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                Files.createTempDirectory("test").toString());
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        // Put an image in the source cache.
        Path image = TestUtil.getImage("jpg");
        SourceCache sourceCache = CacheFactory.getSourceCache();

        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }

        Client client = newClient(uri);
        try {
            client.send();
            // We are expecting NonCheckingAccessResolver.checkAccess() to not
            // throw an exception, which would cause a 500 response.
        } finally {
            client.stop();
        }
    }

    /**
     * Used by {@link #testResolverGetSourceFormatNotCalledWithSourceCacheHit(Identifier, URI)}.
     */
    public static class NotReadingSourceFormatResolver implements StreamResolver {

        @Override
        public void checkAccess() {}

        @Override
        public Format getSourceFormat() throws IOException {
            throw new IOException("getSourceFormat() called!");
        }

        @Override
        public StreamSource newStreamSource() throws IOException {
            return new PathStreamSource(TestUtil.getImage("jpg"));
        }

        @Override
        public void setIdentifier(Identifier identifier) {}

        @Override
        public void setContext(RequestContext context) {}

    }

    public void testResolverGetSourceFormatNotCalledWithSourceCacheHit(Identifier identifier,
                                                                       URI uri) throws Exception {
        // Set up the environment to use the source cache, not resolve first,
        // and use a non-FileResolver.
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
        config.setProperty(Key.RESOLVER_STATIC,
                NotReadingSourceFormatResolver.class.getName());
        config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
        config.setProperty(Key.SOURCE_CACHE_TTL, 10);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                Files.createTempDirectory("test").toString());
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        // Put an image in the source cache.
        Path image = TestUtil.getImage("jpg");
        SourceCache sourceCache = CacheFactory.getSourceCache();

        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }

        Client client = newClient(uri);
        try {
            client.send();
            // We are expecting NotReadingSourceFormatResolver.getSourceFormat()
            // to not throw an exception, which would cause a 500 response.
        } finally {
            client.stop();
        }
    }

    /**
     * Tests that the server responds with HTTP 500 when a non-
     * {@link edu.illinois.library.cantaloupe.resolver.FileResolver} is
     * used with a non-
     * {@link edu.illinois.library.cantaloupe.processor.StreamProcessor}.
     */
    public void testResolverProcessorCompatibility(URI uri,
                                                   String appServerHost,
                                                   int appServerPort) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                appServerHost + ":" + appServerPort + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty(Key.PROCESSOR_FALLBACK, "KakaduProcessor");

        assertStatus(500, uri);
    }

    /**
     * @param uri URI containing <code>CATS</code> as the slash substitute.
     */
    public void testSlashSubstitution(URI uri) {
        Configuration.getInstance().setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        assertStatus(200, uri);
    }

    public void testUnavailableSourceFormat(URI uri) {
        assertStatus(501, uri);
    }

    private void enableCacheControlHeaders() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "true");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");
    }

    private void initializeBasicAuth(ApplicationServer appServer)
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASIC_AUTH_ENABLED, true);
        config.setProperty(Key.BASIC_AUTH_USERNAME, BASIC_AUTH_USER);
        config.setProperty(Key.BASIC_AUTH_SECRET, BASIC_AUTH_SECRET);
        // To enable auth, the app server needs to be restarted.
        // It will need to be restarted again to disable it.
        appServer.stop();
        appServer.start();
    }

    private void uninitializeBasicAuth(ApplicationServer appServer)
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASIC_AUTH_ENABLED, false);
        appServer.stop();
        appServer.start();
    }

    Path initializeFilesystemCache() throws IOException {
        Path cacheDir = Files.createTempDirectory("test");

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME, cacheDir.toString());
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 10);

        assertRecursiveFileCount(cacheDir, 0);

        return cacheDir;
    }

}
