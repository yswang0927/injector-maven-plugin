package com.gdk.maven.plugin.injector;

import java.util.Properties;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration object for {@link ClassTransformer} class.
 *
 * <pre>
 * {@code
 *
 * <transformerClass>
 *  <className>
 *    your.package.XxxClassTransformer
 *  </className>
 *  <properties>
 *    <property>
 *      <name>prop-name</name>
 *      <value>{ javassist code-value }</value>
 *    </property>
 *  </properties>
 * </transformerClass>
 * }
 * </pre>
 *
 */
public class ClassTransformerConfiguration {

    /**
     * The full qualified class name of the transformer implementation.
     *
     * <p>
     * This class must be available via the plugin classloader.
     * </p>
     *
     * <pre>
     * {@code
     *  <className>
     *    your.package.XxxClassTransformer
     *  </className>
     * }
     * </pre>
     */
    @Parameter(property = "className", required = true)
    private String className;

    /**
     * Properties to configure the class transformer instance.
     *
     * <pre>
     * {@code
     *
     *  <properties>
     *    <property>
     *      <name>prop-name</name>
     *      <value>{ javassist code-value }</value>
     *    </property>
     *  </properties>
     * }
     * </pre>
     */
    @Parameter(property = "properties", required = false)
    private Properties properties;

    /**
     * The transformer implementation full qualified class name.
     *
     * @return maybe {@code null}
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the transformer implementation full qualified class name.
     *
     * @param className should not be {@code null}
     */
    public void setClassName(final String className) {
        this.className = className;
    }

    /**
     * Optional settings for the transformer class.
     *
     * @return never {@code null} but maybe empty.
     */
    public Properties getProperties() {
        return (null == properties) ? new Properties() : this.properties;
    }

    /**
     * Sets the optional settings for the transformer class.
     *
     * @param properties should not be {@code null}
     */
    public void setProperties(final Properties properties) {
        this.properties = (null == properties) ? new Properties() : (Properties)properties.clone();
    }

}
