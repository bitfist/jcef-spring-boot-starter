package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptConfiguration;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Generates TypeScript files for Java types.
 * Manages dependencies and avoids duplicate generation.
 */
class TypeScriptGenerator {

    private final String baseOutputPath;
    private final Elements elementUtils;
    private final TypeConverter typeConverter;
    private final Messager messager;
    private final Set<String> processedTypes = new HashSet<>();

    TypeScriptGenerator(String baseOutputPath, Elements elementUtils, Messager messager) {
        this.baseOutputPath = baseOutputPath;
        this.elementUtils = elementUtils;
        this.typeConverter = new TypeConverter();
        this.messager = messager;
    }

    void generate(TypeElement classElement) throws IOException {
        if (processedTypes.contains(classElement.getQualifiedName().toString())) {
            return;
        }

        generateTypeScriptClass(classElement);
        generateDependencies(classElement);
        processedTypes.add(classElement.getQualifiedName().toString());
    }

    /**
     * Retrieves the output path from @JavaScriptConfiguration or returns an empty Optional.
     */
    private Optional<String> getOutputPath(Element element) {
        TypeScriptConfiguration config = element.getAnnotation(TypeScriptConfiguration.class);
        return Optional.ofNullable(config).map(TypeScriptConfiguration::path);
    }

    private void generateTypeScriptClass(TypeElement classElement) throws IOException {
        Optional<String> pathOpt = getOutputPath(classElement);
        if (pathOpt.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR, "A class annotated with @JavaScriptObject must also have @JavaScriptConfiguration.", classElement);
            return;
        }
        String content = buildTypeScriptClass(classElement, pathOpt.get());
        writeFile(classElement, pathOpt.get(), content);
    }

    private void generateTypeDefinition(TypeElement typeElement) throws IOException {
        if (processedTypes.contains(typeElement.getQualifiedName().toString())) {
            return;
        }
        // For dependencies, use the configured path or fall back to the Java package name.
        String path = getOutputPath(typeElement)
                .orElse(elementUtils.getPackageOf(typeElement).getQualifiedName().toString());

        String content = buildTypeScriptDefinition(typeElement, path);
        writeFile(typeElement, path, content);
        processedTypes.add(typeElement.getQualifiedName().toString());

        // Recursively generate dependencies of this dependency
        generateDependencies(typeElement);
    }

    private String buildTypeScriptClass(TypeElement classElement, String tsPath) {
        String className = classElement.getSimpleName().toString();
        Set<TypeMirror> dependencies = new HashSet<>();

        StringBuilder methodsBuilder = new StringBuilder();
        List<ExecutableElement> methods = ElementFilter.methodsIn(elementUtils.getAllMembers(classElement))
                .stream()
                .filter(m -> m.getModifiers().contains(Modifier.PUBLIC) && !m.getModifiers().contains(Modifier.STATIC))
                .filter(m -> !m.getEnclosingElement().toString().equals("java.lang.Object"))
                .toList();

        for (ExecutableElement method : methods) {
            methodsBuilder.append(buildTypeScriptMethod(method, classElement.getQualifiedName().toString(), dependencies));
        }

        StringBuilder importsBuilder = new StringBuilder();
        String jcefImportPath = PathUtils.getRelativePath(tsPath, "jcef") + "/CefQueryService";
        importsBuilder.append("import { CefQueryService } from '").append(jcefImportPath).append("';\n");

        addImports(tsPath, dependencies, importsBuilder);

        return importsBuilder + "\n" +
                "export class " + className + " {\n" +
                methodsBuilder +
                "}\n";
    }

    private String buildTypeScriptMethod(ExecutableElement method, String className, Set<TypeMirror> dependencies) {
        String methodName = method.getSimpleName().toString();
        String params = method.getParameters().stream()
                .map(p -> {
                    collectDependencies(p.asType(), dependencies);
                    return p.getSimpleName() + ": " + typeConverter.toTypeScript(p.asType());
                })
                .collect(joining(", "));

        collectDependencies(method.getReturnType(), dependencies);
        String tsReturnType = typeConverter.toTypeScript(method.getReturnType());
        String promiseReturnType = tsReturnType.equals("void") ? "void" : "Promise<" + tsReturnType + ">";
        String cefResponseType = typeConverter.getCefResponseType(method.getReturnType());

        String paramNames = method.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .collect(joining(", "));

        return "    public static " + methodName + "(" + params + "): " + promiseReturnType + " {\n" +
                "        return CefQueryService.request('" + className + "', '" + methodName + "', {" + paramNames + "}, '" + cefResponseType + "');\n" +
                "    }\n\n";
    }

    private String buildTypeScriptDefinition(TypeElement typeElement, String tsPath) {
        String typeName = typeElement.getSimpleName().toString();
        Set<TypeMirror> dependencies = new HashSet<>();

        StringBuilder fieldsBuilder = new StringBuilder();
        List<VariableElement> fields = ElementFilter.fieldsIn(elementUtils.getAllMembers(typeElement))
                .stream()
                .filter(f -> !f.getModifiers().contains(Modifier.STATIC))
                .toList();

        for (VariableElement field : fields) {
            collectDependencies(field.asType(), dependencies);
            fieldsBuilder.append("    ")
                    .append(field.getSimpleName())
                    .append(": ")
                    .append(typeConverter.toTypeScript(field.asType()))
                    .append(";\n");
        }

        StringBuilder importsBuilder = new StringBuilder();
        addImports(tsPath, dependencies, importsBuilder);
        if (!importsBuilder.isEmpty()) importsBuilder.append("\n");


        return importsBuilder +
                "export interface " + typeName + " {\n" +
                fieldsBuilder +
                "}\n";
    }

    private void addImports(String tsPath, Set<TypeMirror> dependencies, StringBuilder importsBuilder) {
        for (TypeMirror dep : dependencies) {
            Element depElement = ((DeclaredType) dep).asElement();
            String depPath = getOutputPath(depElement).orElse(elementUtils.getPackageOf(depElement).getQualifiedName().toString());
            String depName = depElement.getSimpleName().toString();
            String importPath = PathUtils.getRelativePath(tsPath, depPath) + "/" + depName;
            importsBuilder.append("import type { ").append(depName).append(" } from '").append(importPath).append("';\n");
        }
    }

    private void generateDependencies(TypeElement typeElement) throws IOException {
        Set<TypeMirror> dependencies = new HashSet<>();
        // Logic for collecting method and field dependencies (remains unchanged)
        // ...
        List<ExecutableElement> methods = ElementFilter.methodsIn(elementUtils.getAllMembers(typeElement))
                .stream()
                .filter(m -> m.getModifiers().contains(Modifier.PUBLIC) && !m.getModifiers().contains(Modifier.STATIC))
                .toList();

        for (ExecutableElement method : methods) {
            collectDependencies(method.getReturnType(), dependencies);
            for (VariableElement parameter : method.getParameters()) {
                collectDependencies(parameter.asType(), dependencies);
            }
        }

        List<VariableElement> fields = ElementFilter.fieldsIn(elementUtils.getAllMembers(typeElement))
                .stream()
                .filter(f -> !f.getModifiers().contains(Modifier.STATIC))
                .toList();
        for (VariableElement field : fields) {
            collectDependencies(field.asType(), dependencies);
        }

        for (TypeMirror dependency : dependencies) {
            generateTypeDefinition((TypeElement) ((DeclaredType) dependency).asElement());
        }
    }

    private void collectDependencies(TypeMirror type, Set<TypeMirror> dependencies) {
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            Element element = declaredType.asElement();
            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();

            if (!packageName.startsWith("java.")) {
                dependencies.add(declaredType);
            }
            // Also check type arguments for generics
            for (TypeMirror typeArgument : declaredType.getTypeArguments()) {
                collectDependencies(typeArgument, dependencies);
            }
        } else if (type.getKind() == TypeKind.ARRAY) {
            collectDependencies(((javax.lang.model.type.ArrayType) type).getComponentType(), dependencies);
        }
    }

    private void writeFile(Element typeElement, String tsPackageName, String content) throws IOException {
        Path packagePath = Paths.get(baseOutputPath, tsPackageName.replace('.', '/'));
        Files.createDirectories(packagePath);
        Path filePath = packagePath.resolve(typeElement.getSimpleName().toString() + ".ts");
        Files.writeString(filePath, content);
    }
}