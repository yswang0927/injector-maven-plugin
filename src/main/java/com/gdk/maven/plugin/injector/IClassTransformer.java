package com.gdk.maven.plugin.injector;

import javassist.CtClass;

/**
 * A common interface to define class transformers, java (or groovy)
 * entities that can manipulate classes during build processes,
 * right after javac compiled them.
 */
public interface IClassTransformer {

    /**
     * <p>Test if the given class is suitable for applying transformations or not.</p>
     * <p>For example, if the class is a specific type:</p>
     * <code><pre>
     * CtClass myInterface = ClassPool.getDefault().get(MyInterface.class.getName());
     * return candidateClass.subtypeOf(myInterface);
     * </pre></code>
     * Override this method to boost class transformations and discard classes you don't want
     * to transform.
     *
     * @param ctClass
     * @return {@code true} if the Class should be transformed; {@code false} otherwise.
     */
    boolean shouldTransform(CtClass ctClass);

    /**
     * <p>Concrete implementations must implement all transformations on this method.
     * You can use Javassist API to add/remove/replace methods, attributes and more.
     * Only classes approved by {@link #shouldTransform(CtClass)} are considered.</p>
     *
     * @param ctClass The class to transform.
     */
    void applyTransformations(CtClass ctClass);

}
