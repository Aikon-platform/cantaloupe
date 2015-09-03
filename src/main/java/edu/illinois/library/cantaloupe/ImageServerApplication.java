package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.resource.ImageResource;
import edu.illinois.library.cantaloupe.resource.InformationResource;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsFilter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by alexd on 9/1/15.
 */
public class ImageServerApplication extends Application {

    /**
     * Overrides the built-in Restlet status pages.
     */
    private class CustomStatusService extends StatusService {

        @Override
        public Representation toRepresentation(Status status, Request request,
                                               Response response) {
            Throwable cause = status.getThrowable().getCause();
            String msg = String.format(
                    "<html><head></head><body><h1>%s %s</h1><p>%s</p></body></html>",
                    Integer.toString(status.getCode()),
                    status.getReasonPhrase(),
                    cause.getMessage());
            return new StringRepresentation(msg, MediaType.TEXT_HTML,
                    Language.ENGLISH, CharacterSet.UTF_8);
        }

        @Override
        public Status getStatus(Throwable t, Request request,
                                Response response) {
            Status status;
            Throwable cause = t.getCause();
            if (cause instanceof IllegalArgumentException) {
                status = new Status(Status.CLIENT_ERROR_BAD_REQUEST, t);
            } else if (cause instanceof FileNotFoundException) {
                status = new Status(Status.CLIENT_ERROR_NOT_FOUND, t);
            } else {
                status = new Status(Status.SERVER_ERROR_INTERNAL, t);
            }
            return status;
        }

    }

    public ImageServerApplication() {
        super();
        this.setStatusService(new CustomStatusService());
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        CorsFilter corsFilter = new CorsFilter(getContext(), router);
        corsFilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
        corsFilter.setAllowedCredentials(true);

        // 2.1 Image Request
        // http://iiif.io/api/image/2.0/#image-request-uri-syntax
        // {scheme}://{server}{/prefix}/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
        router.attach("/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                ImageResource.class);

        // 5 Information Request
        // http://iiif.io/api/image/2.0/#information-request
        // {scheme}://{server}{/prefix}/{identifier}/info.json
        router.attach("/{identifier}/info", InformationResource.class);

        // landing page
        router.attach("/{uri}", LandingResource.class).
                setMatchingMode(Template.MODE_STARTS_WITH);

        return corsFilter;
    }

}