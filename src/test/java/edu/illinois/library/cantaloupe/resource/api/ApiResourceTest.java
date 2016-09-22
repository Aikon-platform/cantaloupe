package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;

/**
 * Functional test of ApiResource.
 */
public class ApiResourceTest extends ResourceTest {

    private static final String identifier = "jpg";
    private static final String username = "admin";
    private static final String secret = "secret";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        StandaloneEntry.getWebServer().stop();

        Configuration config = Configuration.getInstance();
        resetConfiguration();
        config.setProperty(ApiResource.ENABLED_CONFIG_KEY, true);
        config.setProperty(WebApplication.API_USERNAME_CONFIG_KEY, username);
        config.setProperty(WebApplication.API_SECRET_CONFIG_KEY, secret);

        StandaloneEntry.getWebServer().start();
    }

    @Test
    public void testDoPurgeWithEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(ApiResource.ENABLED_CONFIG_KEY, false);

        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithNoCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithInvalidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "invalid", "invalid"));
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithValidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        client.delete();
        assertEquals(Status.SUCCESS_NO_CONTENT, client.getStatus());

        // TODO: assert that relevant cache files have been deleted
    }

}
