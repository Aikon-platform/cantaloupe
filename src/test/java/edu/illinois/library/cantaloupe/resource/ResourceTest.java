package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebServer;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

public abstract class ResourceTest extends BaseTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";
    protected static final Integer PORT = TestUtil.getOpenPort();

    protected static WebServer webServer;

    protected Client client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();

        webServer = StandaloneEntry.getWebServer();
        webServer.setHttpEnabled(true);
        webServer.setHttpPort(PORT);
        webServer.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        webServer.stop();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty("FilesystemResolver.lookup_strategy",
                "BasicLookupStrategy");
        config.setProperty("FilesystemResolver.BasicLookupStrategy.path_prefix",
                TestUtil.getFixturePath() + "/images/");

        client = new Client(new Context(), Protocol.HTTP);
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        client.stop();
    }

    protected ClientResource getClientForUriPath(String path) {
        Reference url = new Reference("http://localhost:" + PORT + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

}
