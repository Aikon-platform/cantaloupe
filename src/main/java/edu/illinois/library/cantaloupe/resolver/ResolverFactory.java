package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.script.ScriptUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain an instance of the {@link Resolver} defined in the
 * configuration.
 */
public abstract class ResolverFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ResolverFactory.class);

    public static final String CHOOSER_SCRIPT_CONFIG_KEY =
            "resolver.chooser_script";
    public static final String STATIC_RESOLVER_CONFIG_KEY = "resolver.static";
    private static final Set<String> SUPPORTED_SCRIPT_EXTENSIONS =
            new HashSet<>();

    static {
        SUPPORTED_SCRIPT_EXTENSIONS.add("rb");
    }

    /**
     * If {@link #CHOOSER_SCRIPT_CONFIG_KEY} is defined, uses the specified
     * script to return an instance of the appropriate resolver for the given
     * identifier. Otherwise, returns an instance of the resolver specified in
     * {@link #STATIC_RESOLVER_CONFIG_KEY}.
     *
     * @return An instance of the appropriate resolver for the given identifier
     * based on the value of {@link #CHOOSER_SCRIPT_CONFIG_KEY}.
     * @throws Exception
     * @throws FileNotFoundException If the specified chooser script is not
     * found.
     */
    public static Resolver getResolver(Identifier identifier) throws Exception {
        final String scriptValue = Application.getConfiguration().
                getString(CHOOSER_SCRIPT_CONFIG_KEY);
        if (scriptValue != null) {
            final File script = ScriptUtil.findScript(scriptValue);
            if (!script.exists()) {
                throw new FileNotFoundException("Does not exist: " +
                        script.getAbsolutePath());
            }
            final String resolverName = (String) executeLookupScript(identifier,
                    script);
            return newResolver(resolverName);
        }
        return getStaticResolver();
    }

    /**
     * @return An instance of the current resolver based on the
     * <code>resolver</code> setting in the configuration.
     * @throws Exception
     * @throws ConfigurationException If there is no resolver specified in the
     * configuration.
     */
    private static Resolver getStaticResolver() throws Exception {
        String resolverName = Application.getConfiguration().
                getString(STATIC_RESOLVER_CONFIG_KEY);
        if (resolverName != null) {
            return newResolver(resolverName);
        } else {
            throw new ConfigurationException("No resolver specified in the " +
                    "configuration. (Check the \"resolver\" key.)");
        }
    }

    private static Resolver newResolver(String name) throws Exception {
        Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + name);
        return (Resolver) class_.newInstance();
    }

    /**
     * Passes the given identifier to a function in the given script.
     *
     * @param identifier
     * @param script
     * @return Pathname of the image file corresponding to the given identifier,
     * as reported by the lookup script, or null.
     * @throws IOException If the lookup script configuration key is undefined
     * @throws ScriptException If the script failed to execute
     * @throws ScriptException If the script is of an unsupported type
     */
    private static Object executeLookupScript(Identifier identifier, File script)
            throws ScriptException, IOException {
        final String extension = FilenameUtils.getExtension(script.getName());

        if (SUPPORTED_SCRIPT_EXTENSIONS.contains(extension)) {
            logger.debug("Using lookup script: {}", script);
            switch (extension) {
                case "rb":
                    final ScriptEngine engine = ScriptEngineFactory.
                            getScriptEngine("jruby");
                    final long msec = System.currentTimeMillis();
                    engine.load(FileUtils.readFileToString(script));
                    final String functionName = "get_resolver";
                    final String[] args = { identifier.toString() };
                    final Object result = engine.invoke(functionName, args);
                    logger.debug("{} load+exec time: {} msec",
                            functionName, System.currentTimeMillis() - msec);
                    return result;
            }
        }
        throw new ScriptException("Unsupported script type: " + extension);
    }

}
