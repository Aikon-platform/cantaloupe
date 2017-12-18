package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class LookupStrategyTest extends BaseTest {

    @Test
    public void testFromKeyWithBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        LookupStrategy strategy =
                LookupStrategy.fromKey(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY);
        assertEquals(LookupStrategy.BASIC, strategy);
    }

    @Test
    public void testFromKeyWithDelegateScriptStrategy() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        LookupStrategy strategy =
                LookupStrategy.fromKey(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY);
        assertEquals(LookupStrategy.DELEGATE_SCRIPT, strategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromKeyWithIllegalStrategy() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY, "bogus");
        LookupStrategy.fromKey(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY);
    }

}
