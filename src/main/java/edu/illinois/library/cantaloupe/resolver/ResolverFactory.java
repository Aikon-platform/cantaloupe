package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain an instance of a {@link Resolver} defined in the
 * configuration, or returned by a delegate method.
 */
public class ResolverFactory {

    /**
     * How resolvers are chosen by {@link #newResolver}.
     */
    public enum SelectionStrategy {

        /**
         * A global resolver is specified using the {@link Key#RESOLVER_STATIC}
         * configuration key.
         */
        STATIC,

        /**
         * A resolver specific to the request is acquired from the {@link
         * DelegateMethod#RESOLVER} delegate method.
         */
        DELEGATE_SCRIPT

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ResolverFactory.class);

    /**
     * @return Set of instances of each unique resolver.
     */
    public static Set<Resolver> getAllResolvers() {
        return new HashSet<>(Arrays.asList(
                new AzureStorageResolver(),
                new FilesystemResolver(),
                new HttpResolver(),
                new JdbcResolver(),
                new S3Resolver()));
    }

    /**
     * If {@link Key#RESOLVER_STATIC} is null or undefined, uses a delegate
     * script method to return an instance of the appropriate resolver for the
     * given identifier. Otherwise, returns an instance of the resolver
     * specified in {@link Key#RESOLVER_STATIC}.
     *
     * @param identifier Identifier of the source image.
     * @param proxy      Delegate proxy. May be {@literal null} if
     *                   {@link #getSelectionStrategy()} returns {@link
     *                   SelectionStrategy#STATIC}.
     * @return Instance of the appropriate resolver for the given identifier,
     *         with identifier already set.
     * @throws IllegalArgumentException if the {@literal proxy} argument is
     *                                  {@literal null} while using {@link
     *                                  SelectionStrategy#DELEGATE_SCRIPT}.
     */
    public Resolver newResolver(Identifier identifier,
                                DelegateProxy proxy) throws Exception {
        final Configuration config = Configuration.getInstance();

        switch (getSelectionStrategy()) {
            case DELEGATE_SCRIPT:
                if (proxy == null) {
                    throw new IllegalArgumentException("The " +
                            DelegateProxy.class.getSimpleName() +
                            " argument must be non-null when using " +
                            getSelectionStrategy() + ".");
                }
                Resolver resolver = newDynamicResolver(identifier, proxy);
                LOGGER.info("{}() returned a {} for {}",
                        DelegateMethod.RESOLVER,
                        resolver.getClass().getSimpleName(),
                        identifier);
                return resolver;
            default:
                final String resolverName =
                        config.getString(Key.RESOLVER_STATIC);
                if (resolverName != null) {
                    return newResolver(resolverName, identifier, proxy);
                } else {
                    throw new ConfigurationException(Key.RESOLVER_STATIC +
                            " is not set to a valid resolver.");
                }
        }
    }

    /**
     * @return How resolvers are chosen by {@link #newResolver}.
     */
    public SelectionStrategy getSelectionStrategy() {
        final Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.RESOLVER_DELEGATE, false) ?
                SelectionStrategy.DELEGATE_SCRIPT : SelectionStrategy.STATIC;
    }

    private Resolver newResolver(String name,
                                 Identifier identifier,
                                 DelegateProxy proxy) throws Exception {
        Class<?> class_ = Class.forName(
                ResolverFactory.class.getPackage().getName() + "." + name);
        Resolver resolver = (Resolver) class_.newInstance();
        resolver.setIdentifier(identifier);
        resolver.setDelegateProxy(proxy);
        return resolver;
    }

    /**
     * @param identifier Identifier to return a resolver for.
     * @param proxy      Delegate proxy from which to acquire the resolver
     *                   name.
     * @return           Resolver as returned from the given delegate proxy.
     * @throws IOException     if the lookup script configuration key is
     *                         undefined.
     * @throws ScriptException if the delegate method failed to execute.
     */
    private Resolver newDynamicResolver(Identifier identifier,
                                        DelegateProxy proxy) throws Exception {
        return newResolver(proxy.getResolver(), identifier, proxy);
    }

}
