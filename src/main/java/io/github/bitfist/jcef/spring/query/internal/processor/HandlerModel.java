package io.github.bitfist.jcef.spring.query.internal.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

/**
 * 📦 Model representing a controller and its metadata for TS generation.
 *
 * @param javaType       Java controller type element.
 * @param routePrefix    Route prefix from @CefQueryHandler.
 * @param tsPackage      Target TypeScript package name.
 * @param handlerMethods Methods to generate TS stubs for.
 * @param complexDtos    Map of DTO class names to type elements for interface rendering.
 */
public record HandlerModel(
        TypeElement javaType,
        String routePrefix,
        String tsPackage,
        List<ExecutableElement> handlerMethods,
        Map<String, TypeElement> complexDtos
) {
}
