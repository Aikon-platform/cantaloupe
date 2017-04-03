package edu.illinois.library.cantaloupe.config;

import java.io.IOException;

public abstract class ConfigurationFactory {

    public static final String CONFIG_VM_ARGUMENT = "cantaloupe.config";

    private static volatile Configuration instance;
    private static final Object lock = new Object();

    public static synchronized void clearInstance() {
        if (instance != null) {
            instance.stopWatching();
            instance = null;
        }
    }

    /**
     * @return Shared application configuration instance.
     */
    public static Configuration getInstance() {
        Configuration config = instance;
        if (config == null) {
            synchronized (lock) {
                config = instance;
                if (config == null) {
                    final String configArg = System.getProperty(CONFIG_VM_ARGUMENT);
                    if (configArg != null) {
                        if (configArg.equals("memory")) {
                            config = new MemoryConfiguration();
                        } else {
                            config = new HeritablePropertiesConfiguration();
                        }
                        try {
                            config.reload();
                        } catch (Exception e) {
                            System.err.println("ConfigurationFactory.getInstance(): " +
                                    e.getMessage());
                        }
                        instance = config;
                    } else {
                        System.err.println("ConfigurationFactory.getInstance(): " +
                                "missing " + CONFIG_VM_ARGUMENT + " VM option.");
                    }
                }
            }
        }
        return config;
    }

}
