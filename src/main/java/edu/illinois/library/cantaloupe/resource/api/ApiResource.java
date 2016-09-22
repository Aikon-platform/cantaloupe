package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.ResourceException;

public class ApiResource extends AbstractResource {

    static final String API_ENABLED_CONFIG_KEY = "admin.api.enabled";

    @Override
    protected void doInit() throws ResourceException {
        if (!Configuration.getInstance().
                getBoolean(API_ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

    /**
     * @throws Exception
     */
    @Delete
    public Representation doPurge() throws Exception {
        final Cache cache = CacheFactory.getDerivativeCache();
        if (cache != null) {
            final String idStr = (String) this.getRequest().getAttributes().
                    get("identifier");
            final Identifier identifier = new Identifier(idStr);

            cache.purgeImage(identifier);
        }
        return new EmptyRepresentation();
    }

}
