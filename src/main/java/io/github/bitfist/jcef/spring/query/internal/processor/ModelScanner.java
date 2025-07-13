package io.github.bitfist.jcef.spring.query.internal.processor;

import io.github.bitfist.jcef.spring.query.TypeScriptConfiguration;
import io.github.bitfist.jcef.spring.query.CefQueryHandler;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 🔍 Scans the annotation processing environment to discover handler classes
 * and complex DTO types for TypeScript generation.
 */
final class ModelScanner {

    private final Elements elements;

    ModelScanner(Elements utils) {
        this.elements = utils;
    }

    Collection<HandlerModel> scan(RoundEnvironment round) {
        Map<TypeElement, List<ExecutableElement>> grouped = new LinkedHashMap<>();

        round.getElementsAnnotatedWith(CefQueryHandler.class).forEach(e -> {
            if (e.getKind() == ElementKind.CLASS) {
                grouped.put((TypeElement) e, new ArrayList<>());
            } else if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement m = (ExecutableElement) e;
                grouped.computeIfAbsent((TypeElement) m.getEnclosingElement(),
                        $ -> new ArrayList<>()).add(m);
            }
        });

        List<HandlerModel> result = new ArrayList<>();
        grouped.entrySet().stream()
                .map(entry -> build(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .forEach(result::add);
        return result;
    }

    private HandlerModel build(TypeElement clazz, List<ExecutableElement> methods) {
        // route prefix
        CefQueryHandler queryHandlerAnnotation = clazz.getAnnotation(CefQueryHandler.class);
        if (queryHandlerAnnotation == null) {
            return null;
        }
        String route = queryHandlerAnnotation.value();

        // TS package override
        String pkg = Optional.ofNullable(clazz.getAnnotation(TypeScriptConfiguration.class))
                .map(TypeScriptConfiguration::packageName)
                .filter(p -> !p.isBlank())
                .orElse(elements.getPackageOf(clazz).getQualifiedName().toString());

        // discover complex DTOs
        Map<String, TypeElement> complex = new LinkedHashMap<>();
        methods.forEach(m -> {
            addIfComplex(complex, m.getReturnType());
            m.getParameters().forEach(p -> addIfComplex(complex, p.asType()));
        });

        return new HandlerModel(clazz, route, pkg, List.copyOf(methods), complex);
    }

    private static boolean isJavaLangPrimitive(String fqn) {
        return switch (fqn) {
            case "java.lang.String", "java.lang.Integer", "java.lang.Long", "java.lang.Double",
                 "java.lang.Float", "java.lang.Short", "java.lang.Byte", "java.lang.Boolean" -> true;
            default -> false;
        };
    }

    private static void addIfComplex(Map<String, TypeElement> map, TypeMirror t) {
        if (t.getKind() != TypeKind.DECLARED) return;
        DeclaredType dt = (DeclaredType) t;
        TypeElement te = (TypeElement) dt.asElement();
        String fqn = te.getQualifiedName().toString();
        if (!isJavaLangPrimitive(fqn)) {
            map.putIfAbsent(fqn, te);
        }
    }
}
