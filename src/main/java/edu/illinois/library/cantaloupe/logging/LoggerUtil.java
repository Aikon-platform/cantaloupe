package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;

public abstract class LoggerUtil {

    /**
     * Reloads the Logback configuration from logback.xml.
     */
    public static synchronized void reloadConfiguration() {
        Configuration appConfig = ConfigurationFactory.getInstance();
        if (appConfig != null) {
            // Reset the logger context
            LoggerContext loggerContext = (LoggerContext)
                    LoggerFactory.getILoggerFactory();
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(loggerContext);
            loggerContext.reset();
            // Then copy logging-related configuration key/values into logger
            // context properties...
            Iterator it = appConfig.getKeys();
            while (it.hasNext()) {
                String key = (String) it.next();
                if (key.startsWith("log.")) {
                    loggerContext.putProperty(key, appConfig.getString(key));
                }
            }
            // Finally, reload the Logback configuration.
            try {
                InputStream stream = Application.class.getClassLoader().
                        getResourceAsStream("logback.xml");
                jc.doConfigure(stream);
            } catch (JoranException je) {
                je.printStackTrace();
            }
            StatusPrinter.printIfErrorsOccured(loggerContext);
        }
    }

}
