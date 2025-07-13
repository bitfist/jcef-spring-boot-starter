package io.github.bitfist.jcef.spring.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a Java class for TypeScript code generation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface TypeScriptConfiguration {

    /**
     * Contains the package name the TypeScript class is generated in.
     *
     * @return the package name
     */
    String packageName();
}
