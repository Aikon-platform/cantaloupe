package edu.illinois.library.cantaloupe;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * Main application class.
 */
public class Application {

    private static Configuration config;

    public static void main(String[] args) throws Exception {
        startRestlet();
    }

    /**
     * @return The application-wide Configuration object.
     */
    public static Configuration getConfiguration() {
        if (config == null) {
            try {
                PropertiesConfiguration propConfig = new PropertiesConfiguration();
                propConfig.load(System.getProperty("cantaloupe.config"));
                config = propConfig;
            } catch (ConfigurationException e) {
                // TODO: log fatal error
            }
        }
        return config;
    }

    /**
     * Overrides the configuration, mainly for testing purposes.
     */
    public static void setConfiguration(Configuration c) {
        config = c;
    }

    public static void startRestlet() throws Exception {
        Component component = new Component();
        Integer port = getConfiguration().getInteger("http.port", 8182);
        component.getServers().add(Protocol.HTTP, port);
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.start();
    }

}
