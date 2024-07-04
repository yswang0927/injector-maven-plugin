package com.gdk.maven.plugin.injector;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for class transformation logic.
 */
public abstract class ClassTransformer implements IClassTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassTransformer.class);

    /**
     * Configure this instance by passing {@link Properties}.
     *
     * @param properties maybe {@code null} or empty
     * @throws Exception if configuration failed.
     */
    public void configure(final Properties properties) {
        //
    }

    /**
     * Returns the logger.
     *
     * @return never {@code null}
     */
    protected static Logger getLogger() {
        return LOGGER;
    }

}
