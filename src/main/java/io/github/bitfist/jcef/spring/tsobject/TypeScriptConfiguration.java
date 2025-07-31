package io.github.bitfist.jcef.spring.tsobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets the user configure JavaScript generation.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TypeScriptConfiguration {

	String path() default "";
}
