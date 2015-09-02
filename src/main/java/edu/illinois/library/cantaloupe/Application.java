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

    public static Configuration getConfiguration() throws ConfigurationException {
        if (config == null) {
            PropertiesConfiguration propConfig = new PropertiesConfiguration();
            propConfig.load("cantaloupe.properties");
            config = propConfig;
        }
        return config;
    }

    public static void main(String[] args) throws Exception {
        Component component = new Component();
        Integer port = getConfiguration().getInteger("http.port", 8182);
        component.getServers().add(Protocol.HTTP, port);
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.start();
    }

}
