package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.HashMap;

import static org.junit.Assert.*;

public class AuthorizerTest extends BaseTest {

    private Authorizer instance;

    @Before
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        instance = new Authorizer("http://localhost/", "127.0.0.1",
                new HashMap<>(), new HashMap<>());
    }

    @Test
    public void testAuthorizeWithDelegateScriptDisabled() throws Exception {
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);

        AuthInfo info = instance.authorize(
                new OperationList(), new Dimension(500, 500));

        assertTrue(info.isAuthorized());
        assertNull(info.getRedirectURI());
        assertNull(info.getRedirectStatus());
    }

    @Test
    public void testAuthorizeWithTrueReturnValue() throws Exception {
        AuthInfo info = instance.authorize(
                new OperationList(new Identifier("cats")),
                new Dimension(500, 500));

        assertTrue(info.isAuthorized());
        assertNull(info.getRedirectURI());
        assertNull(info.getRedirectStatus());
    }

    @Test
    public void testAuthorizeWithFalseReturnValue() throws Exception {
        OperationList opList = new OperationList(new Identifier("forbidden.jpg"));
        AuthInfo info = instance.authorize(opList, new Dimension(500, 500));

        assertFalse(info.isAuthorized());
        assertNull(info.getRedirectURI());
        assertNull(info.getRedirectStatus());
    }

    @Test
    public void testAuthorizeWithHashReturnValue() throws Exception {
        OperationList opList = new OperationList(new Identifier("redirect.jpg"));
        AuthInfo info = instance.authorize(opList, new Dimension(500, 500));

        assertFalse(info.isAuthorized());
        assertEquals(303, info.getRedirectStatus().intValue());
        assertEquals("http://example.org/", info.getRedirectURI().toString());
    }

}
