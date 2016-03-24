package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheFactoryTest {

    @Test
    public void testGetAllDerivativeCaches() {
        assertEquals(4, CacheFactory.getAllDerivativeCaches().size());
    }

    @Test
    public void testGetAllSourceCaches() {
        assertEquals(1, CacheFactory.getAllSourceCaches().size());
    }

    @Test
    public void testGetInstance() throws Exception {
        Configuration config = Configuration.getInstance();
        config.clear();

        final String key = CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY;

        config.setProperty(key, "FilesystemCache");
        assertTrue(CacheFactory.getDerivativeCache() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertNull(CacheFactory.getDerivativeCache());

        config.setProperty(key, null);
        assertNull(CacheFactory.getDerivativeCache());

        config.setProperty(key, "bogus");
        assertNull(CacheFactory.getDerivativeCache());
    }

}
