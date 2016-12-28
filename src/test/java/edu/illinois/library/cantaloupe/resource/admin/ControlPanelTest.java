package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.WebServer;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.*;

public class ControlPanelTest extends BaseTest {

    private static final int SLEEP_AFTER_SUBMIT = 600;
    private static final String username = "admin";
    private static final String secret = "secret";

    private static WebDriver webDriver;
    private static WebServer webServer;

    private WebElement css(String selector) {
        return webDriver.findElement(By.cssSelector(selector));
    }

    private String getAdminUri() {
        return String.format("http://%s:%s@localhost:%d%s",
                username, secret,
                webServer.getHttpPort(),
                WebApplication.ADMIN_PATH);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, true);
        config.setProperty(WebApplication.ADMIN_SECRET_CONFIG_KEY, secret);

        webServer = StandaloneEntry.getWebServer();
        webServer.setHttpEnabled(true);
        webServer.setHttpPort(TestUtil.getOpenPort());
        webServer.start();

        System.setProperty("webdriver.gecko.driver",
                TestUtil.getTestConfig().getString(ConfigurationConstants.GECKO_WEBDRIVER.getKey()));
        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
        capabilities.setCapability("marionette", true);
        webDriver = new FirefoxDriver(capabilities);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        webServer.stop();
        webDriver.close();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, true);
        config.setProperty(WebApplication.ADMIN_SECRET_CONFIG_KEY, secret);
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");

        webDriver.get(getAdminUri());
    }

    @Test
    public void testServerSection() throws Exception {
        css("#cl-http-button").click();

        // Fill in the form
        css("[name=\"http.enabled\"]").click();
        css("[name=\"http.host\"]").sendKeys("1.2.3.4");
        css("[name=\"http.port\"]").sendKeys("8989");
        css("[name=\"https.enabled\"]").click();
        css("[name=\"https.host\"]").sendKeys("2.3.4.5");
        css("[name=\"https.port\"]").sendKeys("8990");
        css("[name=\"https.key_store_type\"]").sendKeys("PKCS12");
        css("[name=\"https.key_store_path\"]").sendKeys("/something");
        css("[name=\"https.key_store_password\"]").sendKeys("cats");
        css("[name=\"auth.basic.enabled\"]").click();
        css("[name=\"auth.basic.username\"]").sendKeys("dogs");
        css("[name=\"auth.basic.secret\"]").sendKeys("foxes");
        css("[name=\"base_uri\"]").sendKeys("http://bla/bla/");
        css("[name=\"slash_substitute\"]").sendKeys("^");
        css("[name=\"print_stack_trace_on_error_pages\"]").click();

        // Submit the form
        css("#cl-http input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertTrue(config.getBoolean("http.enabled"));
        assertEquals("1.2.3.4", config.getString("http.host"));
        assertEquals(8989, config.getInt("http.port"));
        assertTrue(config.getBoolean("https.enabled"));
        assertEquals("2.3.4.5", config.getString("https.host"));
        assertEquals(8990, config.getInt("https.port"));
        assertEquals("PKCS12", config.getString("https.key_store_type"));
        assertEquals("/something", config.getString("https.key_store_path"));
        assertEquals("cats", config.getString("https.key_store_password"));
        assertTrue(config.getBoolean("auth.basic.enabled"));
        assertEquals("dogs", config.getString("auth.basic.username"));
        assertEquals("foxes", config.getString("auth.basic.secret"));
        assertEquals("http://bla/bla/", config.getString("base_uri"));
        assertEquals("^", config.getString("slash_substitute"));
        assertTrue(config.getBoolean("print_stack_trace_on_error_pages"));
    }

    @Test
    public void testEndpointsSection() throws Exception {
        css("#cl-endpoints-button").click();

        // Fill in the form
        css("[name=\"max_pixels\"]").sendKeys("5000");
        new Select(css("[name=\"endpoint.iiif.content_disposition\"]")).
                selectByValue("attachment");
        css("[name=\"endpoint.iiif.min_tile_size\"]").sendKeys("250");
        css("[name=\"endpoint.iiif.1.enabled\"]").click();
        css("[name=\"endpoint.iiif.2.enabled\"]").click();
        css("[name=\"endpoint.iiif.2.restrict_to_sizes\"]").click();
        css("[name=\"endpoint.api.enabled\"]").click();
        css("[name=\"endpoint.api.username\"]").sendKeys("cats");
        css("[name=\"endpoint.api.secret\"]").sendKeys("dogs");

        // Submit the form
        css("#cl-endpoints input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertEquals(5000, config.getInt("max_pixels"));
        assertEquals("attachment",
                config.getString("endpoint.iiif.content_disposition"));
        assertEquals(250, config.getInt("endpoint.iiif.min_tile_size"));
        assertTrue(config.getBoolean("endpoint.iiif.1.enabled"));
        assertTrue(config.getBoolean("endpoint.iiif.2.enabled"));
        assertTrue(config.getBoolean("endpoint.iiif.2.restrict_to_sizes"));
        assertTrue(config.getBoolean("endpoint.api.enabled"));
        assertEquals("cats", config.getString("endpoint.api.username"));
        assertEquals("dogs", config.getString("endpoint.api.secret"));
    }

    @Test
    public void testResolverSection() throws Exception {
        css("#cl-resolver-button").click();

        // Fill in the form
        new Select(css("[name=\"resolver.delegate\"]")).selectByValue("false");
        new Select(css("[name=\"resolver.static\"]")).
                selectByVisibleText("FilesystemResolver");
        // AmazonS3Resolver section
        css("#cl-resolver li > a[href=\"#AmazonS3Resolver\"]").click();
        css("[name=\"AmazonS3Resolver.access_key_id\"]").sendKeys("123");
        css("[name=\"AmazonS3Resolver.secret_key\"]").sendKeys("456");
        css("[name=\"AmazonS3Resolver.bucket.name\"]").sendKeys("cats");
        css("[name=\"AmazonS3Resolver.bucket.region\"]").sendKeys("antarctica");
        new Select(css("[name=\"AmazonS3Resolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        // AzureStorageResolver
        css("#cl-resolver li > a[href=\"#AzureStorageResolver\"]").click();
        css("[name=\"AzureStorageResolver.account_name\"]").sendKeys("bla");
        css("[name=\"AzureStorageResolver.account_key\"]").sendKeys("cats");
        css("[name=\"AzureStorageResolver.container_name\"]").sendKeys("bucket");
        new Select(css("[name=\"AzureStorageResolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        // FilesystemResolver
        css("#cl-resolver li > a[href=\"#FilesystemResolver\"]").click();
        new Select(css("[name=\"FilesystemResolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        css("[name=\"FilesystemResolver.BasicLookupStrategy.path_prefix\"]").
                sendKeys("/prefix");
        css("[name=\"FilesystemResolver.BasicLookupStrategy.path_suffix\"]").
                sendKeys("/suffix");
        // HttpResolver
        css("#cl-resolver li > a[href=\"#HttpResolver\"]").click();
        new Select(css("[name=\"HttpResolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        css("[name=\"HttpResolver.BasicLookupStrategy.url_prefix\"]").
                sendKeys("http://prefix/");
        css("[name=\"HttpResolver.BasicLookupStrategy.url_suffix\"]").
                sendKeys("/suffix");
        css("[name=\"HttpResolver.auth.basic.username\"]").sendKeys("username");
        css("[name=\"HttpResolver.auth.basic.secret\"]").sendKeys("password");
        // JdbcResolver
        css("#cl-resolver li > a[href=\"#JdbcResolver\"]").click();
        css("[name=\"JdbcResolver.url\"]").sendKeys("cats://dogs");
        css("[name=\"JdbcResolver.user\"]").sendKeys("user");
        css("[name=\"JdbcResolver.password\"]").sendKeys("password");
        css("[name=\"JdbcResolver.connection_timeout\"]").sendKeys("5");

        // Submit the form
        css("#cl-resolver input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertFalse(config.getBoolean("resolver.delegate"));
        assertEquals("FilesystemResolver", config.getString("resolver.static"));
        // AmazonS3Resolver
        assertEquals("123", config.getString("AmazonS3Resolver.access_key_id"));
        assertEquals("456", config.getString("AmazonS3Resolver.secret_key"));
        assertEquals("cats", config.getString("AmazonS3Resolver.bucket.name"));
        assertEquals("antarctica",
                config.getString("AmazonS3Resolver.bucket.region"));
        assertEquals("BasicLookupStrategy",
                config.getString("AmazonS3Resolver.lookup_strategy"));
        // AzureStorageResolver
        assertEquals("bla", config.getString("AzureStorageResolver.account_name"));
        assertEquals("cats", config.getString("AzureStorageResolver.account_key"));
        assertEquals("bucket", config.getString("AzureStorageResolver.container_name"));
        assertEquals("BasicLookupStrategy",
                config.getString("AzureStorageResolver.lookup_strategy"));
        // FilesystemResolver
        assertEquals("BasicLookupStrategy",
                config.getString("FilesystemResolver.lookup_strategy"));
        assertEquals("/prefix",
                config.getString("FilesystemResolver.BasicLookupStrategy.path_prefix"));
        assertEquals("/suffix",
                config.getString("FilesystemResolver.BasicLookupStrategy.path_suffix"));
        // HttpResolver
        assertEquals("BasicLookupStrategy",
                config.getString("HttpResolver.lookup_strategy"));
        assertEquals("http://prefix/",
                config.getString("HttpResolver.BasicLookupStrategy.url_prefix"));
        assertEquals("/suffix",
                config.getString("HttpResolver.BasicLookupStrategy.url_suffix"));
        assertEquals("username",
                config.getString("HttpResolver.auth.basic.username"));
        assertEquals("password",
                config.getString("HttpResolver.auth.basic.secret"));
        // JdbcResolver
        assertEquals("cats://dogs", config.getString("JdbcResolver.url"));
        assertEquals("user", config.getString("JdbcResolver.user"));
        assertEquals("password", config.getString("JdbcResolver.password"));
        assertEquals("5", config.getString("JdbcResolver.connection_timeout"));
    }

    @Test
    public void testProcessorsSection() throws Exception {
        css("#cl-processors-button").click();

        // Fill in the form
        css("#cl-processors li > a[href=\"#cl-image-assignments\"]").click();
        new Select(css("[name=\"processor.gif\"]")).
                selectByVisibleText("Java2dProcessor");
        new Select(css("[name=\"processor.fallback\"]")).
                selectByVisibleText("JaiProcessor");
        new Select(css("[name=\"StreamProcessor.retrieval_strategy\"]")).
                selectByValue("StreamStrategy");
        // FfmpegProcessor
        css("#cl-processors li > a[href=\"#FfmpegProcessor\"]").click();
        css("[name=\"FfmpegProcessor.path_to_binaries\"]").sendKeys("/ffpath");
        css("[name=\"FfmpegProcessor.sharpen\"]").sendKeys("0.2");
        // GraphicsMagickProcessor
        css("#cl-processors li > a[href=\"#GraphicsMagickProcessor\"]").click();
        css("[name=\"GraphicsMagickProcessor.path_to_binaries\"]").sendKeys("/gmpath");
        new Select(css("[name=\"GraphicsMagickProcessor.background_color\"]")).
                selectByValue("black");
        css("[name=\"GraphicsMagickProcessor.normalize\"]").click();
        css("[name=\"GraphicsMagickProcessor.sharpen\"]").sendKeys("0.2");
        // ImageMagickProcessor
        css("#cl-processors li > a[href=\"#ImageMagickProcessor\"]").click();
        css("[name=\"ImageMagickProcessor.path_to_binaries\"]").sendKeys("/impath");
        new Select(css("[name=\"ImageMagickProcessor.background_color\"]")).
                selectByValue("white");
        css("[name=\"ImageMagickProcessor.normalize\"]").click();
        css("[name=\"ImageMagickProcessor.sharpen\"]").sendKeys("0.2");
        // JaiProcessor
        css("#cl-processors li > a[href=\"#JaiProcessor\"]").click();
        css("[name=\"JaiProcessor.normalize\"]").click();
        css("[name=\"JaiProcessor.sharpen\"]").sendKeys("0.2");
        css("[name=\"JaiProcessor.jpg.quality\"]").sendKeys("0.55");
        new Select(css("[name=\"JaiProcessor.tif.compression\"]")).
                selectByVisibleText("PackBits");
        // Java2dProcessor
        css("#cl-processors li > a[href=\"#Java2dProcessor\"]").click();
        new Select(css("[name=\"Java2dProcessor.downscale_filter\"]")).
                selectByVisibleText("Mitchell");
        new Select(css("[name=\"Java2dProcessor.upscale_filter\"]")).
                selectByVisibleText("Triangle");
        css("[name=\"Java2dProcessor.normalize\"]").click();
        css("[name=\"Java2dProcessor.sharpen\"]").sendKeys("0.2");
        css("[name=\"Java2dProcessor.jpg.quality\"]").sendKeys("0.55");
        new Select(css("[name=\"Java2dProcessor.tif.compression\"]")).
                selectByVisibleText("PackBits");
        // KakaduProcessor
        css("#cl-processors li > a[href=\"#KakaduProcessor\"]").click();
        css("[name=\"KakaduProcessor.path_to_binaries\"]").sendKeys("/kpath");
        new Select(css("[name=\"KakaduProcessor.downscale_filter\"]")).
                selectByVisibleText("Mitchell");
        new Select(css("[name=\"KakaduProcessor.upscale_filter\"]")).
                selectByVisibleText("Triangle");
        css("[name=\"KakaduProcessor.normalize\"]").click();
        css("[name=\"KakaduProcessor.sharpen\"]").sendKeys("0.2");
        // OpenJpegProcessor
        css("#cl-processors li > a[href=\"#OpenJpegProcessor\"]").click();
        css("[name=\"OpenJpegProcessor.path_to_binaries\"]").sendKeys("/ojpath");
        new Select(css("[name=\"OpenJpegProcessor.downscale_filter\"]")).
                selectByVisibleText("Mitchell");
        new Select(css("[name=\"OpenJpegProcessor.upscale_filter\"]")).
                selectByVisibleText("Triangle");
        css("[name=\"OpenJpegProcessor.normalize\"]").click();
        css("[name=\"OpenJpegProcessor.sharpen\"]").sendKeys("0.2");
        // PdfBoxProcessor
        css("#cl-processors li > a[href=\"#PdfBoxProcessor\"]").click();
        css("[name=\"PdfBoxProcessor.dpi\"]").sendKeys("300");
        new Select(css("[name=\"PdfBoxProcessor.downscale_filter\"]")).
                selectByVisibleText("Mitchell");
        new Select(css("[name=\"PdfBoxProcessor.upscale_filter\"]")).
                selectByVisibleText("Triangle");
        css("[name=\"PdfBoxProcessor.sharpen\"]").sendKeys("0.2");

        // Submit the form
        css("#cl-processors input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertEquals("Java2dProcessor", config.getString("processor.gif"));
        assertEquals("JaiProcessor", config.getString("processor.fallback"));
        assertEquals("StreamStrategy",
                config.getString("StreamProcessor.retrieval_strategy"));
        // FfmpegProcessor
        assertEquals("/ffpath",
                config.getString("FfmpegProcessor.path_to_binaries"));
        assertEquals("0.2", config.getString("FfmpegProcessor.sharpen"));
        // GraphicsMagickProcessor
        assertEquals("/gmpath",
                config.getString("GraphicsMagickProcessor.path_to_binaries"));
        assertEquals("black",
                config.getString("GraphicsMagickProcessor.background_color"));
        assertTrue(config.getBoolean("GraphicsMagickProcessor.normalize"));
        assertEquals("0.2",
                config.getString("GraphicsMagickProcessor.sharpen"));
        // ImageMagickProcessor
        assertEquals("/impath",
                config.getString("ImageMagickProcessor.path_to_binaries"));
        assertEquals("white",
                config.getString("ImageMagickProcessor.background_color"));
        assertTrue(config.getBoolean("ImageMagickProcessor.normalize"));
        assertEquals("0.2", config.getString("ImageMagickProcessor.sharpen"));
        // JaiProcessor
        assertTrue(config.getBoolean("JaiProcessor.normalize"));
        assertEquals("0.2", config.getString("JaiProcessor.sharpen"));
        assertEquals("0.55", config.getString("JaiProcessor.jpg.quality"));
        assertEquals("PackBits",
                config.getString("JaiProcessor.tif.compression"));
        // Java2dProcessor
        assertTrue(config.getBoolean("Java2dProcessor.normalize"));
        assertEquals("0.2", config.getString("Java2dProcessor.sharpen"));
        assertEquals("0.55",
                config.getString("Java2dProcessor.jpg.quality"));
        assertEquals("PackBits",
                config.getString("Java2dProcessor.tif.compression"));
        // KakaduProcessor
        assertEquals("/kpath",
                config.getString("KakaduProcessor.path_to_binaries"));
        assertTrue(config.getBoolean("KakaduProcessor.normalize"));
        assertEquals("0.2", config.getString("KakaduProcessor.sharpen"));
        // OpenJpegProcessor
        assertEquals("/ojpath",
                config.getString("OpenJpegProcessor.path_to_binaries"));
        assertTrue(config.getBoolean("OpenJpegProcessor.normalize"));
        assertEquals("0.2", config.getString("KakaduProcessor.sharpen"));
        // PdfBoxProcessor
        assertEquals(300, config.getInt("PdfBoxProcessor.dpi"));
        assertEquals("0.2", config.getString("PdfBoxProcessor.sharpen"));
    }

    @Test
    public void testCachesSection() throws Exception {
        css("#cl-caches-button").click();

        // Fill in the form
        css("[name=\"cache.client.enabled\"]").click();
        css("[name=\"cache.client.max_age\"]").sendKeys("250");
        css("[name=\"cache.client.shared_max_age\"]").sendKeys("220");
        css("[name=\"cache.client.public\"]").click();
        css("[name=\"cache.client.private\"]").click();
        css("[name=\"cache.client.no_cache\"]").click();
        css("[name=\"cache.client.no_store\"]").click();
        css("[name=\"cache.client.must_revalidate\"]").click();
        css("[name=\"cache.client.proxy_revalidate\"]").click();
        css("[name=\"cache.client.no_transform\"]").click();
        new Select(css("[name=\"cache.source\"]")).selectByValue("");
        new Select(css("[name=\"cache.derivative\"]")).
                selectByVisibleText("FilesystemCache");
        css("[name=\"" + Cache.PURGE_MISSING_CONFIG_KEY + "\"]").click();
        css("[name=\"" + Cache.RESOLVE_FIRST_CONFIG_KEY + "\"]").click();
        css("[name=\"" + Cache.TTL_CONFIG_KEY + "\"]").sendKeys("10");
        css("[name=\"cache.server.worker.enabled\"]").click();
        css("[name=\"cache.server.worker.interval\"]").sendKeys("25");
        // AmazonS3Cache
        css("#cl-caches li > a[href=\"#AmazonS3Cache\"]").click();
        css("[name=\"AmazonS3Cache.access_key_id\"]").sendKeys("cats");
        css("[name=\"AmazonS3Cache.secret_key\"]").sendKeys("dogs");
        css("[name=\"AmazonS3Cache.bucket.name\"]").sendKeys("bucket");
        css("[name=\"AmazonS3Cache.bucket.region\"]").sendKeys("greenland");
        css("[name=\"AmazonS3Cache.object_key_prefix\"]").sendKeys("obj");
        // AzureStorageCache
        css("#cl-caches li > a[href=\"#AzureStorageCache\"]").click();
        css("[name=\"AzureStorageCache.account_name\"]").sendKeys("bees");
        css("[name=\"AzureStorageCache.account_key\"]").sendKeys("birds");
        css("[name=\"AzureStorageCache.container_name\"]").sendKeys("badger");
        css("[name=\"AzureStorageCache.object_key_prefix\"]").sendKeys("obj");
        // FilesystemCache
        css("#cl-caches li > a[href=\"#FilesystemCache\"]").click();
        css("[name=\"FilesystemCache.pathname\"]").sendKeys("/path");
        css("[name=\"FilesystemCache.dir.depth\"]").sendKeys("8");
        css("[name=\"FilesystemCache.dir.name_length\"]").sendKeys("4");
        // JdbcCache
        css("#cl-caches li > a[href=\"#JdbcCache\"]").click();
        css("[name=\"JdbcCache.url\"]").sendKeys("jdbc://dogs");
        css("[name=\"JdbcCache.user\"]").sendKeys("person");
        css("[name=\"JdbcCache.password\"]").sendKeys("cats");
        css("[name=\"JdbcCache.max_pool_size\"]").sendKeys("92");
        css("[name=\"JdbcCache.connection_timeout\"]").sendKeys("9");
        css("[name=\"JdbcCache.derivative_image_table\"]").sendKeys("hula");
        css("[name=\"JdbcCache.info_table\"]").sendKeys("box");

        // Submit the form
        css("#cl-caches input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertTrue(config.getBoolean("cache.client.enabled"));
        assertEquals("250", config.getString("cache.client.max_age"));
        assertEquals("220", config.getString("cache.client.shared_max_age"));
        assertTrue(config.getBoolean("cache.client.public"));
        assertTrue(config.getBoolean("cache.client.private"));
        assertTrue(config.getBoolean("cache.client.no_cache"));
        assertTrue(config.getBoolean("cache.client.no_store"));
        assertTrue(config.getBoolean("cache.client.must_revalidate"));
        assertTrue(config.getBoolean("cache.client.proxy_revalidate"));
        assertTrue(config.getBoolean("cache.client.no_transform"));
        assertEquals("", config.getString("cache.source"));
        assertEquals("FilesystemCache", config.getString("cache.derivative"));
        //assertTrue(config.getBoolean(Cache.PURGE_MISSING_CONFIG_KEY)); TODO: why does this not work?
        assertTrue(config.getBoolean(Cache.RESOLVE_FIRST_CONFIG_KEY));
        assertEquals(10, config.getInt(Cache.TTL_CONFIG_KEY));
        assertTrue(config.getBoolean("cache.server.worker.enabled"));
        assertEquals(25, config.getInt("cache.server.worker.interval"));
        // AmazonS3Cache
        assertEquals("cats", config.getString("AmazonS3Cache.access_key_id"));
        assertEquals("dogs", config.getString("AmazonS3Cache.secret_key"));
        assertEquals("bucket", config.getString("AmazonS3Cache.bucket.name"));
        assertEquals("greenland", config.getString("AmazonS3Cache.bucket.region"));
        assertEquals("obj", config.getString("AmazonS3Cache.object_key_prefix"));
        // AzureStorageCache
        assertEquals("bees", config.getString("AzureStorageCache.account_name"));
        assertEquals("birds", config.getString("AzureStorageCache.account_key"));
        assertEquals("badger", config.getString("AzureStorageCache.container_name"));
        assertEquals("obj", config.getString("AzureStorageCache.object_key_prefix"));
        // FilesystemCache
        assertEquals("/path", config.getString("FilesystemCache.pathname"));
        assertEquals("8", config.getString("FilesystemCache.dir.depth"));
        assertEquals("4", config.getString("FilesystemCache.dir.name_length"));
        // JdbcCache
        assertEquals("jdbc://dogs", config.getString("JdbcCache.url"));
        assertEquals("person", config.getString("JdbcCache.user"));
        assertEquals("cats", config.getString("JdbcCache.password"));
        assertEquals("92", config.getString("JdbcCache.max_pool_size"));
        assertEquals("9", config.getString("JdbcCache.connection_timeout"));
        assertEquals("hula", config.getString("JdbcCache.derivative_image_table"));
        assertEquals("box", config.getString("JdbcCache.info_table"));
    }

    @Test
    public void testOverlaysSection() throws Exception {
        css("#cl-overlays-button").click();

        // Fill in the form
        css("[name=\"overlays.enabled\"]").click();
        new Select(css("[name=\"overlays.strategy\"]")).
                selectByValue("BasicStrategy");
        new Select(css("[name=\"overlays.BasicStrategy.position\"]")).
                selectByValue("bottom left");
        css("[name=\"overlays.BasicStrategy.inset\"]").sendKeys("35");
        css("[name=\"overlays.BasicStrategy.output_width_threshold\"]").sendKeys("5");
        css("[name=\"overlays.BasicStrategy.output_height_threshold\"]").sendKeys("6");
        new Select(css("[name=\"overlays.BasicStrategy.type\"]")).
                selectByValue("string");
        css("[name=\"overlays.BasicStrategy.image\"]").sendKeys("/image.png");
        css("[name=\"overlays.BasicStrategy.string\"]").sendKeys("cats");
        new Select(css("[name=\"overlays.BasicStrategy.string.font\"]")).
                selectByVisibleText("Helvetica");
        css("[name=\"overlays.BasicStrategy.string.font.size\"]").sendKeys("13");
        css("[name=\"overlays.BasicStrategy.string.color\"]").sendKeys("#d0d0d0");
        css("[name=\"overlays.BasicStrategy.string.stroke.color\"]").sendKeys("#e0e0e0");
        css("[name=\"overlays.BasicStrategy.string.stroke.width\"]").sendKeys("5");
        css("[name=\"redaction.enabled\"]").click();

        // Submit the form
        css("#cl-overlays input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertTrue(config.getBoolean("overlays.enabled"));
        assertEquals("BasicStrategy", config.getString("overlays.strategy"));
        assertEquals("bottom left",
                config.getString("overlays.BasicStrategy.position"));
        assertEquals("35",
                config.getString("overlays.BasicStrategy.inset"));
        assertEquals("5",
                config.getString("overlays.BasicStrategy.output_width_threshold"));
        assertEquals("6",
                config.getString("overlays.BasicStrategy.output_height_threshold"));
        assertEquals("string",
                config.getString("overlays.BasicStrategy.type"));
        assertEquals("/image.png",
                config.getString("overlays.BasicStrategy.image"));
        assertEquals("cats",
                config.getString("overlays.BasicStrategy.string"));
        assertEquals("Helvetica",
                config.getString("overlays.BasicStrategy.string.font"));
        assertEquals("13",
                config.getString("overlays.BasicStrategy.string.font.size"));
        assertEquals("#d0d0d0",
                config.getString("overlays.BasicStrategy.string.color"));
        assertEquals("#e0e0e0",
                config.getString("overlays.BasicStrategy.string.stroke.color"));
        assertEquals("5",
                config.getString("overlays.BasicStrategy.string.stroke.width"));
        assertTrue(config.getBoolean("redaction.enabled"));
    }

    @Test
    public void testMetadataSection() throws Exception {
        css("#cl-metadata-button").click();

        // Fill in the form
        css("[name=\"metadata.preserve\"]").click();

        // Submit the form
        css("#cl-metadata input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertTrue(config.getBoolean("metadata.preserve"));
    }

    @Test
    public void testDelegateScriptSection() throws Exception {
        css("#cl-delegate-script-button").click();

        // Fill in the form
        css("[name=\"delegate_script.enabled\"]").click();
        css("[name=\"delegate_script.pathname\"]").sendKeys("file");
        css("[name=\"delegate_script.cache.enabled\"]").click();
        css("[name=\"delegate_script.cache.max_size\"]").sendKeys("12345");

        // Submit the form
        css("#cl-delegate-script input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertTrue(config.getBoolean("delegate_script.enabled"));
        assertEquals("file", config.getString("delegate_script.pathname"));
        assertTrue(config.getBoolean("delegate_script.cache.enabled"));
        assertEquals("12345", config.getString("delegate_script.cache.max_size"));
    }

    @Test
    public void testLoggingSection() throws Exception {
        css("#cl-logging-button").click();

        // Fill in the form
        new Select(css("[name=\"log.application.level\"]")).
                selectByValue("warn");
        css("[name=\"log.application.ConsoleAppender.enabled\"]").click();
        css("[name=\"log.application.FileAppender.enabled\"]").click();
        css("[name=\"log.application.FileAppender.pathname\"]").
                sendKeys("/path1");
        css("[name=\"log.application.RollingFileAppender.enabled\"]").click();
        css("[name=\"log.application.RollingFileAppender.pathname\"]").
                sendKeys("/path2");
        css("[name=\"log.application.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern\"]").
                sendKeys("pattern");
        css("[name=\"log.application.RollingFileAppender.TimeBasedRollingPolicy.max_history\"]").
                sendKeys("15");
        css("[name=\"log.application.SyslogAppender.enabled\"]").click();
        css("[name=\"log.application.SyslogAppender.host\"]").sendKeys("host");
        css("[name=\"log.application.SyslogAppender.port\"]").sendKeys("555");
        css("[name=\"log.application.SyslogAppender.facility\"]").
                sendKeys("cats");
        css("[name=\"log.access.ConsoleAppender.enabled\"]").click();
        css("[name=\"log.access.FileAppender.enabled\"]").click();
        css("[name=\"log.access.FileAppender.pathname\"]").
                sendKeys("/path3");
        css("[name=\"log.access.RollingFileAppender.enabled\"]").click();
        css("[name=\"log.access.RollingFileAppender.pathname\"]").
                sendKeys("/path4");
        css("[name=\"log.access.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern\"]").
                sendKeys("dogs");
        css("[name=\"log.access.RollingFileAppender.TimeBasedRollingPolicy.max_history\"]").
                sendKeys("531");
        css("[name=\"log.access.SyslogAppender.enabled\"]").click();
        css("[name=\"log.access.SyslogAppender.host\"]").sendKeys("host2");
        css("[name=\"log.access.SyslogAppender.port\"]").sendKeys("251");
        css("[name=\"log.access.SyslogAppender.facility\"]").sendKeys("foxes");

        // Submit the form
        css("#cl-logging input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = ConfigurationFactory.getInstance();
        assertEquals("warn", config.getString("log.application.level"));
        assertTrue(config.getBoolean("log.application.ConsoleAppender.enabled"));

        assertTrue(config.getBoolean("log.application.FileAppender.enabled"));
        assertEquals("/path1",
                config.getString("log.application.FileAppender.pathname"));

        assertTrue(config.getBoolean("log.application.RollingFileAppender.enabled"));
        assertEquals("/path2",
                config.getString("log.application.RollingFileAppender.pathname"));
        assertEquals("pattern",
                config.getString("log.application.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern"));
        assertEquals("15",
                config.getString("log.application.RollingFileAppender.TimeBasedRollingPolicy.max_history"));

        assertTrue(config.getBoolean("log.application.SyslogAppender.enabled"));
        assertEquals("host",
                config.getString("log.application.SyslogAppender.host"));
        assertEquals("555",
                config.getString("log.application.SyslogAppender.port"));
        assertEquals("cats",
                config.getString("log.application.SyslogAppender.facility"));

        assertTrue(config.getBoolean("log.access.ConsoleAppender.enabled"));

        assertTrue(config.getBoolean("log.access.FileAppender.enabled"));
        assertEquals("/path3",
                config.getString("log.access.FileAppender.pathname"));

        assertTrue(config.getBoolean("log.access.RollingFileAppender.enabled"));
        assertEquals("/path4",
                config.getString("log.access.RollingFileAppender.pathname"));
        assertEquals("dogs",
                config.getString("log.access.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern"));
        assertEquals("531",
                config.getString("log.access.RollingFileAppender.TimeBasedRollingPolicy.max_history"));

        assertTrue(config.getBoolean("log.access.SyslogAppender.enabled"));
        assertEquals("host2",
                config.getString("log.access.SyslogAppender.host"));
        assertEquals("251",
                config.getString("log.access.SyslogAppender.port"));
        assertEquals("foxes",
                config.getString("log.access.SyslogAppender.facility"));
    }

}
