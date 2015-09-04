package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.io.InputStream;

public class HttpResolver extends AbstractResolver implements Resolver {

    private static Client client = new Client(new Context(), Protocol.HTTP);

    public InputStream resolve(String identifier) {
        try {
            Configuration config = Application.getConfiguration();
            Reference url = getUrl(identifier);
            ClientResource resource = new ClientResource(url);
            resource.setNext(client);

            // set up HTTP Basic authentication
            String username = config.getString("HttpResolver.username");
            String password = config.getString("HttpResolver.password");
            if (username.length() > 0 && password.length() > 0) {
                resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                        username, password);
            }

            return resource.get().getStream();
        } catch (Exception e) {
            return null;
        }
    }

    private Reference getUrl(String identifier) {
        Configuration config = Application.getConfiguration();
        return new Reference(config.getString("HttpResolver.url_prefix") +
                identifier + config.getString("HttpResolver.url_suffix"));
    }

}
