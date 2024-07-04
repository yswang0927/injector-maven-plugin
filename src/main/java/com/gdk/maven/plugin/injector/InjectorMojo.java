package com.gdk.maven.plugin.injector;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * maven+javassist 插件，用于给编译的class文件动态插入代码。
 */
@Mojo(name = "injector", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class InjectorMojo extends AbstractMojo {

    private static final Class<IClassTransformer> TRANSFORMER_TYPE = IClassTransformer.class;

    @Parameter(property = "injector.project", defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Skips all processing performed by this goal.
     *
     * <pre>
     * {@code
     * ...
     * <configuration>
     *   <skip>false</skip>
     * </configuration>
     * ...
     * }
     * </pre>
     */
    @Parameter(property = "injector.skip", defaultValue = "false", required = false)
    private boolean skip;

    /**
     * Whether or not to include test classes to be processed by declared transformers.
     *
     * <pre>
     * {@code
     * ...
     * <configuration>
     *   <includeTestClasses>false</includeTestClasses>
     * </configuration>
     * ...
     * }
     * </pre>
     */
    @Parameter(property = "injector.includeTestClasses", defaultValue = "false", required = false)
    private Boolean includeTestClasses;

    /**
     * Configure one or more class transformer.
     *
     * <pre>
     * {@code
     * ...
     * <configuration>
     *   <transformerClasses>
     *     <transformerClass>
     *      <className>your.package.XxTransformer</className>
     *      <properties>
     *          <property>
     *              <name>prop-name</name>
     *              <value>{ prop-value }</value>
     *          </property>
     *      </properties>
     *     </transformerClass>
     *   </transformerClasses>
     * </configuration>
     * ...
     * }
     * </pre>
     */
    @Parameter(property = "injector.transformerClasses", required = true)
    private ClassTransformerConfiguration[] transformerClasses;

    /**
     * Allows to customize the build directory of the project, used for both finding classes to
     * transform and output them once transformed.
     *
     * <p>
     * The path must be either absolute or relative to project base directory.
     * </p>
     *
     * <pre>
     * {@code
     * ...
     * <configuration>
     *   <buildDir>bin/classes</buildDir>
     * </configuration>
     * ...
     * }
     * </pre>
     */
    @Parameter(property = "injector.buildDir", defaultValue = "target/classes", required = false)
    private String buildDir;

    /**
     * Allows to customize the build directory of the tests of the project, used for both finding
     * classes to transform and output them once transformed.
     *
     * <p>
     * The path must be either absolute or relative to project base directory.
     * </p>
     *
     * <pre>
     * {@code
     * ...
     * <configuration>
     *   <testBuildDir>bin/test-classes</testBuildDir>
     * </configuration>
     * ...
     * }
     * </pre>
     */
    @Parameter(property = "injector.testBuildDir", defaultValue = "target/test-classes",required = false)
    private String testBuildDir;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Injector-maven-plugin skipping executing.");
            return;
        }

        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            final List<URL> classPath = new ArrayList<URL>();

            for (final String runtimeResource : project.getRuntimeClasspathElements()) {
                classPath.add(resolveUrl(runtimeResource));
            }

            final String inputDirectory = (null == buildDir) ? project.getBuild().getOutputDirectory() : computeDir(buildDir);

            classPath.add(resolveUrl(inputDirectory));
            loadAdditionalClassPath(classPath);

            final InjectTransformerExecutor executor = new InjectTransformerExecutor(getLog());
            executor.setTransformerClasses(instantiateTransformerClasses(Thread.currentThread().getContextClassLoader(), transformerClasses));
            executor.setInputDirectory(inputDirectory);
            executor.setOutputDirectory(inputDirectory);
            executor.execute();

            if (includeTestClasses) {
                final String testInputDirectory = (null == testBuildDir) ? project.getBuild().getTestOutputDirectory() : computeDir(testBuildDir);
                classPath.add(resolveUrl(testInputDirectory));
                executor.setInputDirectory(testInputDirectory);
                executor.setOutputDirectory(testInputDirectory);
                executor.execute();
            }

        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private void loadAdditionalClassPath(final List<URL> classPath) {
        if (classPath.isEmpty()) {
            return;
        }

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final URLClassLoader pluginClassLoader = URLClassLoader.newInstance(classPath.toArray(new URL[classPath.size()]), contextClassLoader);
        Thread.currentThread().setContextClassLoader(pluginClassLoader);
    }

    private String computeDir(String dir) {
        File dirFile = new File(dir);
        if (dirFile.isAbsolute()) {
            return dirFile.getAbsolutePath();
        } else {
            return new File(project.getBasedir(), buildDir).getAbsolutePath();
        }
    }

    /**
     * Instantiates and configures the passed transformer classes.
     *
     * @param contextClassLoader maybe {@code null}
     * @param transformerClasses maybe {@code null}
     * @return array of passed transformer class name instances and never {@code null} but maybe
     *         empty.
     * @throws Exception by
     *           {@link #instantiateTransformerClass(ClassLoader, ClassTransformerConfiguration)} and
     *           {@link #configureTransformerInstance(IClassTransformer, Properties)}
     * @see #instantiateTransformerClass(ClassLoader, ClassTransformerConfiguration)
     * @see #configureTransformerInstance(IClassTransformer, Properties)
     */
    protected IClassTransformer[] instantiateTransformerClasses(final ClassLoader contextClassLoader,
            final ClassTransformerConfiguration... transformerClasses) throws Exception {

        if (null == transformerClasses || transformerClasses.length == 0) {
            throw new MojoExecutionException("[Injector-maven-plugin] Invalid transformer classes passed.");
        }

        final List<IClassTransformer> transformerInstances = new LinkedList<IClassTransformer>();
        for (ClassTransformerConfiguration transformerClass : transformerClasses) {
            final IClassTransformer transformerInstance = instantiateTransformerClass(contextClassLoader, transformerClass);
            configureTransformerInstance(transformerInstance, transformerClass.getProperties());
            transformerInstances.add(transformerInstance);
        }
        return transformerInstances.toArray(new IClassTransformer[transformerInstances.size()]);
    }

    /**
     * Instantiate the class passed by {@link ClassTransformerConfiguration} configuration object.
     *
     * @param contextClassLoader maybe {@code null}
     * @param transformerClass must not be {@code null}
     *
     * @return new instance of passed transformer class name and never {@code null}
     *
     * @throws ClassNotFoundException by {@code transformerClass} {@link Class#forName(String)}.
     * @throws InstantiationException by {@code transformerClass} {@link Class#forName(String)}.
     * @throws IllegalAccessException by {@code transformerClass} {@link Class#forName(String)}.
     * @throws MojoExecutionException if passed {@code transformerClass} is {@code null} or invalid
     *
     * @see Class#forName(String, boolean, ClassLoader)
     */
    protected IClassTransformer instantiateTransformerClass(
            final ClassLoader contextClassLoader,
            final ClassTransformerConfiguration transformerClass) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, MojoExecutionException {

        if (null == transformerClass || null == transformerClass.getClassName()
                || transformerClass.getClassName().trim().isEmpty()) {
            throw new MojoExecutionException("[Injector-maven-plugin] Invalid transformer class name passed");
        }

        final Class<?> transformerClassInstance = Class.forName(transformerClass.getClassName().trim(),
                true, contextClassLoader);

        if (TRANSFORMER_TYPE.isAssignableFrom(transformerClassInstance)) {
            return TRANSFORMER_TYPE.cast(transformerClassInstance.newInstance());
        } else {
            throw new MojoExecutionException("[Injector-maven-plugin] Transformer class must inherit from " + TRANSFORMER_TYPE.getName());
        }
    }

    /**
     * Configure the passed {@link ClassTransformer} instance using the passed {@link Properties}.
     *
     * @param transformerInstance maybe {@code null}
     * @param properties maybe {@code null} or empty
     *
     * @throws Exception by {@link ClassTransformer#configure(Properties)}
     */
    protected void configureTransformerInstance(final IClassTransformer transformerInstance,
                                                final Properties properties) throws Exception {
        if (null == transformerInstance || !(transformerInstance instanceof ClassTransformer)) {
            return;
        }

        ((ClassTransformer)transformerInstance).configure(properties);
    }

    private URL resolveUrl(final String resource) {
        try {
            return new File(resource).toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Signals whether or not to skip the plugin execution.
     *
     * @return {@code true} if configuration option is set otherwise {@code false}
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * Whether or not to include test classes in class transformation.
     *
     * @return {@code true} if configuration option is set otherwise {@code false} and never
     *         {@code null}
     */
    public Boolean getIncludeTestClasses() {
        return null == includeTestClasses ? Boolean.FALSE : includeTestClasses;
    }

    /**
     * The configured transformer classes.
     *
     * @return all configured transformer classes and never {@code null} but maybe empty.
     */
    public ClassTransformerConfiguration[] getTransformerClasses() {
        return (null == this.transformerClasses) ? new ClassTransformerConfiguration[0] : this.transformerClasses.clone();
    }

    /**
     * The build directory of the project, used for both finding classes to
     * transform and output them once transformed.
     *
     * @return never {@code null}
     */
    public String getBuildDir() {
        return buildDir;
    }

    /**
     * The build directory of the tests of the project, used for both finding
     * classes to transform and output them once transformed.
     *
     * @return never {@code null}
     */
    public String getTestBuildDir() {
        return testBuildDir;
    }

}
