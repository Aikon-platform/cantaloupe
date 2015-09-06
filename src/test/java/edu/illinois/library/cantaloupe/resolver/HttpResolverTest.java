package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.IOException;

public class HttpResolverTest extends TestCase {

    private static final String FILE = "escher_lego.jpg";

    HttpResolver instance;

    public void setUp() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("HttpResolver.url_prefix", "");
        Application.setConfiguration(config);

        instance = new HttpResolver();
    }

    public void testResolve() {
        assertNull(instance.resolve("bogus"));
        // TODO: test against a real server
    }

    public void testGetUrl() {
        BaseConfiguration config = (BaseConfiguration) Application.getConfiguration();
        // with prefix
        config.setProperty("HttpResolver.url_prefix",
                "http://example.org/prefix/");
        assertEquals("http://example.org/prefix/id",
                instance.getUrl("id").toString());
        // with suffix
        config.setProperty("HttpResolver.url_suffix", "/suffix");
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getUrl("id").toString());
    }

}
