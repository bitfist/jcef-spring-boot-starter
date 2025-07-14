package io.github.bitfist.jcef.spring.jsexecution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotation used to define JavaScript code to be executed in the browser context. When applied to a method, this
 * annotation specifies the JavaScript snippet that should be deployed and executed for the corresponding functionality.
 * </p>
 *
 * <p>
 * Methods annotated with {@code @Code} are typically processed during compilation to generate a mechanism for
 * executing the associated JavaScript within a browser environment.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaScriptCode {

    /**
     * Specifies the JavaScript code to be deployed to the browser.
     *
     * @return the JavaScript code as a string
     */
    String value();
}
