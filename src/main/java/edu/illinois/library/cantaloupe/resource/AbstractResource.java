package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.MetadataCopy;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.redaction.RedactionService;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.restlet.Request;
import org.restlet.data.CacheDirective;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AbstractResource extends ServerResource {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractResource.class);

    public static final String BASE_URI_CONFIG_KEY = "base_uri";
    public static final String CLIENT_CACHE_ENABLED_CONFIG_KEY =
            "cache.client.enabled";
    public static final String CLIENT_CACHE_MAX_AGE_CONFIG_KEY =
            "cache.client.max_age";
    public static final String CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY =
            "cache.client.must_revalidate";
    public static final String CLIENT_CACHE_NO_CACHE_CONFIG_KEY =
            "cache.client.no_cache";
    public static final String CLIENT_CACHE_NO_STORE_CONFIG_KEY =
            "cache.client.no_store";
    public static final String CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY =
            "cache.client.no_transform";
    public static final String CLIENT_CACHE_PRIVATE_CONFIG_KEY =
            "cache.client.private";
    public static final String CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY =
            "cache.client.proxy_revalidate";
    public static final String CLIENT_CACHE_PUBLIC_CONFIG_KEY =
            "cache.client.public";
    public static final String CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY =
            "cache.client.shared_max_age";
    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "endpoint.iiif.content_disposition";
    public static final String MAX_PIXELS_CONFIG_KEY = "max_pixels";
    public static final String PRESERVE_METADATA_CONFIG_KEY =
            "metadata.preserve";
    public static final String SLASH_SUBSTITUTE_CONFIG_KEY =
            "slash_substitute";

    /**
     * @return Map of template variables common to most or all views, such as
     * variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars(Request request) {
        Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        vars.put("baseUri", getPublicRootRef(request).toString());
        return vars;
    }

    /**
     * @param request
     * @return A root reference usable in public, respecting the
     * <code>base_uri</code> option in the application configuration.
     */
    public static Reference getPublicRootRef(final Request request) {
        Reference rootRef = new Reference(request.getRootRef());

        final String baseUri = ConfigurationFactory.getInstance().
                getString(BASE_URI_CONFIG_KEY);
        if (baseUri != null && baseUri.length() > 0) {
            final Reference baseRef = new Reference(baseUri);
            rootRef.setScheme(baseRef.getScheme());
            rootRef.setHostDomain(baseRef.getHostDomain());
            // if the "port" is a local socket, Reference will serialize it as
            // -1.
            if (baseRef.getHostPort() == -1) {
                rootRef.setHostPort(null);
            } else {
                rootRef.setHostPort(baseRef.getHostPort());
            }
            rootRef.setPath(StringUtils.stripEnd(baseRef.getPath(), "/"));
        } else {
            final Series<Header> headers = request.getHeaders();
            final String protocolStr = headers.getFirstValue("X-Forwarded-Proto",
                    true, "HTTP");
            final String hostStr = headers.getFirstValue("X-Forwarded-Host",
                    true, null);
            final String portStr = headers.getFirstValue("X-Forwarded-Port",
                    true, "80");
            final String pathStr = headers.getFirstValue("X-Forwarded-Path",
                    true, null);
            if (hostStr != null) {
                logger.debug("Assembling base URI from X-Forwarded headers. " +
                                "Proto: {}; Host: {}; Port: {}; Path: {}",
                        protocolStr, hostStr, portStr, pathStr);

                rootRef.setHostDomain(hostStr);
                rootRef.setPath(pathStr);

                final Protocol protocol = protocolStr.toUpperCase().equals("HTTPS") ?
                        Protocol.HTTPS : Protocol.HTTP;
                rootRef.setProtocol(protocol);

                Integer port = Integer.parseInt(portStr);
                if ((port == 80 && protocol.equals(Protocol.HTTP)) ||
                        (port == 443 && protocol.equals(Protocol.HTTPS))) {
                    port = null;
                }
                rootRef.setHostPort(port);
            }
        }
        return rootRef;
    }

    /**
     * @param identifier
     * @param outputFormat
     * @return A content disposition based on the setting of
     * {@link #CONTENT_DISPOSITION_CONFIG_KEY} in the application configuration.
     * If it is set to <code>attachment</code>, the disposition will have a
     * filename set to a reasonable value based on the given identifier and
     * output format.
     */
    public static Disposition getRepresentationDisposition(
            Identifier identifier, Format outputFormat) {
        Disposition disposition = new Disposition();
        switch (ConfigurationFactory.getInstance().
                getString(CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(
                                ImageRepresentation.FILENAME_CHARACTERS, "_") +
                                "." + outputFormat.getPreferredExtension());
                break;
        }
        return disposition;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        getResponse().getHeaders().add("X-Powered-By",
                "Cantaloupe/" + Application.getVersion());
        logger.info("doInit(): handling {} {}", getMethod(), getReference());
    }

    /**
     * Most image-processing operations (crop, scale, etc.) are specified in
     * a client request to an endpoint. This method adds any operations that
     * endpoints have nothing to do with.
     *
     * @param opList Operation list to add the operations to.
     * @param fullSize Full size of the source image.
     */
    protected void addNonEndpointOperations(final OperationList opList,
                                            final Dimension fullSize) {
        // Redactions
        try {
            if (RedactionService.isEnabled()) {
                List<Redaction> redactions = RedactionService.redactionsFor(
                        opList.getIdentifier(),
                        getRequest().getHeaders().getValuesMap(),
                        getCanonicalClientIpAddress(),
                        getRequest().getCookies().getValuesMap());
                for (Redaction redaction : redactions) {
                    opList.add(redaction);
                }
            } else {
                logger.debug("addNonEndpointOperations(): redactions are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            // no problem
            logger.debug("addNonEndpointOperations(): delegate script is " +
                    "disabled; skipping redactions.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // Watermark
        try {
            final WatermarkService service = new WatermarkService();
            if (service.isEnabled()) {
                final Watermark watermark = service.newWatermark(
                        opList, fullSize, getReference().toUrl(),
                        getRequest().getHeaders().getValuesMap(),
                        getCanonicalClientIpAddress(),
                        getRequest().getCookies().getValuesMap());
                opList.add((Operation) watermark);
            } else {
                logger.debug("addNonEndpointOperations(): watermarking is " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            // no problem
            logger.debug("addNonEndpointOperations(): delegate script is " +
                    "disabled; skipping watermark.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // Metadata copies
        if (ConfigurationFactory.getInstance().
                getBoolean(PRESERVE_METADATA_CONFIG_KEY, false)) {
            opList.add(new MetadataCopy());
        }
    }

    /**
     * Checks the given operation list against the given image size.
     *
     * @param opList
     * @param fullSize
     * @throws EmptyPayloadException
     */
    protected final void checkRequest(final OperationList opList,
                                      final Dimension fullSize)
            throws EmptyPayloadException {
        final Dimension resultingSize = opList.getResultingSize(fullSize);
        if (resultingSize.width < 1 || resultingSize.height < 1) {
            throw new EmptyPayloadException();
        }
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URLs.
     * This method enables the use of an alternate string to represent a slash
     * via {@link #SLASH_SUBSTITUTE_CONFIG_KEY}.
     *
     * @param uriPathComponent Path component (a part of the path before,
     *                         after, or between slashes)
     * @return Path component with slashes decoded
     */
    protected final String decodeSlashes(final String uriPathComponent) {
        final String substitute = ConfigurationFactory.getInstance().
                getString(SLASH_SUBSTITUTE_CONFIG_KEY, "");
        if (substitute.length() > 0) {
            return StringUtils.replace(uriPathComponent, substitute, "/");
        }
        return uriPathComponent;
    }

    protected final Identifier decodeSlashes(final Identifier identifier) {
        return new Identifier(decodeSlashes(identifier.toString()));
    }

    protected final List<CacheDirective> getCacheDirectives() {
        List<CacheDirective> directives = new ArrayList<>();
        try {
            final Configuration config = ConfigurationFactory.getInstance();
            final boolean enabled = config.getBoolean(
                    CLIENT_CACHE_ENABLED_CONFIG_KEY, false);
            if (enabled) {
                final String maxAge = config.getString(
                        CLIENT_CACHE_MAX_AGE_CONFIG_KEY);
                if (maxAge != null && maxAge.length() > 0) {
                    directives.add(CacheDirective.maxAge(Integer.parseInt(maxAge)));
                }
                String sMaxAge = config.getString(
                        CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY);
                if (sMaxAge != null && sMaxAge.length() > 0) {
                    directives.add(CacheDirective.
                            sharedMaxAge(Integer.parseInt(sMaxAge)));
                }
                if (config.getBoolean(CLIENT_CACHE_PUBLIC_CONFIG_KEY, true)) {
                    directives.add(CacheDirective.publicInfo());
                } else if (config.getBoolean(CLIENT_CACHE_PRIVATE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.privateInfo());
                }
                if (config.getBoolean(CLIENT_CACHE_NO_CACHE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.noCache());
                }
                if (config.getBoolean(CLIENT_CACHE_NO_STORE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.noStore());
                }
                if (config.getBoolean(CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.mustRevalidate());
                }
                if (config.getBoolean(CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.proxyMustRevalidate());
                }
                if (config.getBoolean(CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.noTransform());
                }
            }
        } catch (NoSuchElementException e) {
            logger.warn("Cache-Control headers are invalid: {}",
                    e.getMessage());
        }
        return directives;
    }

    /**
     * @return The client IP address, respecting the X-Forwarded-For header,
     * if present.
     */
    protected String getCanonicalClientIpAddress() {
        final List<String> forwardedIps = getRequest().getClientInfo().
                getForwardedAddresses();
        if (forwardedIps.size() > 0) {
            return forwardedIps.get(forwardedIps.size() - 1);
        }
        return getRequest().getClientInfo().getAddress();
    }

    protected ImageRepresentation getRepresentation(OperationList ops,
                                                    Format format,
                                                    Disposition disposition,
                                                    Processor proc)
            throws IOException, ProcessorException, CacheException {
        // Max allowed size is ignored when the processing is a no-op.
        final long maxAllowedSize = (ops.isNoOp(format)) ?
                0 : ConfigurationFactory.getInstance().getLong(MAX_PIXELS_CONFIG_KEY, 0);

        final ImageInfo imageInfo = getOrReadInfo(ops.getIdentifier(), proc);
        final Dimension effectiveSize = ops.getResultingSize(imageInfo.getSize());
        if (maxAllowedSize > 0 &&
                effectiveSize.width * effectiveSize.height > maxAllowedSize) {
            throw new PayloadTooLargeException();
        }

        return new ImageRepresentation(imageInfo, proc, ops, disposition,
                isBypassingCache());
    }

    /**
     * Gets the image info corresponding to the given identifier, first by
     * checking the cache and then, if necessary, by reading it from the image
     * and caching the result.
     *
     * @param identifier
     * @param proc
     * @return ImageInfo for the image with the given identifier, retrieved
     *         from the given processor.
     * @throws ProcessorException
     * @throws CacheException
     */
    protected final ImageInfo getOrReadInfo(final Identifier identifier,
                                            final Processor proc)
            throws ProcessorException, CacheException {
        ImageInfo info = null;
        if (!isBypassingCache()) {
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            if (cache != null) {
                final Stopwatch watch = new Stopwatch();
                info = cache.getImageInfo(identifier);
                if (info != null) {
                    logger.debug("Retrieved dimensions of {} from cache in {} msec",
                            identifier, watch.timeElapsed());
                } else {
                    info = readInfo(identifier, proc);
                    cache.putImageInfo(identifier, info);
                }
            }
        }
        if (info == null) {
            info = readInfo(identifier, proc);
        }
        return info;
    }

    /**
     * Invokes a delegate script method to determine whether the request is
     * authorized.
     *
     * @param opList
     * @param fullSize
     * @return
     * @throws IOException
     * @throws ScriptException
     */
    protected final boolean isAuthorized(final OperationList opList,
                                         final Dimension fullSize)
            throws IOException, ScriptException {
        final Map<String,Integer> fullSizeArg = new HashMap<>();
        fullSizeArg.put("width", fullSize.width);
        fullSizeArg.put("height", fullSize.height);

        final Dimension resultingSize = opList.getResultingSize(fullSize);
        final Map<String,Integer> resultingSizeArg = new HashMap<>();
        resultingSizeArg.put("width", resultingSize.width);
        resultingSizeArg.put("height", resultingSize.height);

        final Map opListMap = opList.toMap(fullSize);

        try {
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            final String method = "authorized?";
            return (boolean) engine.invoke(method,
                    opList.getIdentifier().toString(),         // identifier
                    fullSizeArg,                               // full_size
                    opListMap.get("operations"),               // operations
                    resultingSizeArg,                          // resulting_size
                    opListMap.get("output_format"),            // output_format
                    getReference().toString(),                 // request_uri
                    getRequest().getHeaders().getValuesMap(),  // request_headers
                    getCanonicalClientIpAddress(),             // client_ip
                    getRequest().getCookies().getValuesMap()); // cookies
        } catch (DelegateScriptDisabledException e) {
            logger.debug("isAuthorized(): delegate script is disabled; allowing.");
            return true;
        }
    }

    /**
     * @return Whether there is a <var>cache</var> query parameter set to
     *         <code>false</code> in the URI.
     */
    private boolean isBypassingCache() {
        boolean bypassingCache = false;
        Parameter cacheParam = getReference().getQueryAsForm().getFirst("cache");
        if (cacheParam != null) {
            bypassingCache = "false".equals(cacheParam.getValue());
        }
        return bypassingCache;
    }

    /**
     * Reads the information of the source image.
     *
     * @param identifier
     * @param proc
     * @return
     * @throws ProcessorException
     */
    private ImageInfo readInfo(final Identifier identifier,
                               final Processor proc) throws ProcessorException {
        final Stopwatch watch = new Stopwatch();
        final ImageInfo info = proc.getImageInfo();
        logger.debug("Read info of {} in {} msec", identifier,
                watch.timeElapsed());
        return info;
    }

}
