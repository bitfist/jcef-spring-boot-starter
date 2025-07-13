package io.github.bitfist.jcef.spring.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as a CEF query handler send by the browser using <code>window.cefQuery(...)</code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CefQueryHandler {

    /**
     * The route of the message handler. Supports path variables in {} like Spring Web. Remember to compile with
     * '-parameters'.
     *
     * @return the route of the message handler.
     */
    String value() default "";
}
