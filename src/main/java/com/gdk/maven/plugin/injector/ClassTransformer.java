package com.gdk.maven.plugin.injector;

import java.util.Properties;

/**
 * Base class for class transformation logic.
 */
public abstract class ClassTransformer implements IClassTransformer {

    /**
     * Configure this instance by passing {@link Properties}.
     *
     * @param properties maybe {@code null} or empty
     * @throws Exception if configuration failed.
     */
    public void configure(final Properties properties) {
        //
    }

}
