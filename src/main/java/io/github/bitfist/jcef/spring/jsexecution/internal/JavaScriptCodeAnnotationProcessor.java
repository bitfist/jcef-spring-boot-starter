package io.github.bitfist.jcef.spring.jsexecution.internal;

import io.github.bitfist.jcef.spring.jsexecution.JavaScriptCode;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("io.github.bitfist.jcef.spring.jsexecution.JavaScriptCode")
public class JavaScriptCodeAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private int generatedMethods = 0;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.filer = env.getFiler();
        this.messager = env.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var interfacesAndMethods = new HashMap<TypeElement, List<ExecutableElement>>();
        for (Element e : roundEnv.getElementsAnnotatedWith(JavaScriptCode.class)) {
            if (e.getKind() != ElementKind.METHOD) continue;
            var m = (ExecutableElement) e;
            var iface = (TypeElement) m.getEnclosingElement();
            interfacesAndMethods.computeIfAbsent(iface, k -> new ArrayList<>()).add(m);
        }

        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : interfacesAndMethods.entrySet()) {
            var interfaceType = entry.getKey();
            List<ExecutableElement> methods = entry.getValue();
            var pkg = processingEnv.getElementUtils().getPackageOf(interfaceType).getQualifiedName().toString();
            var interfaceName = interfaceType.getSimpleName().toString();
            var className = interfaceName + "Impl";

            var buffer = new StringBuilder();

            generateClassCode(buffer, pkg, className, interfaceName);
            for (ExecutableElement method : methods) {
                if (method.getReturnType().getKind() != TypeKind.VOID) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "JavaScriptCode annotation is only supported on void methods.");
                    return false;
                }
                generateMethodCode(method, buffer);
            }
            finishClass(buffer);

            try {
                var file = filer.createSourceFile(pkg + "." + className);
                var w = file.openWriter();
                w.write(buffer.toString());
                w.close();
            } catch (IOException ex) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write " + className + ": " + ex.getMessage());
            }
        }
        return true;
    }

    private static void generateClassCode(StringBuilder buffer, String pkg, String className, String interfaceName) {
        buffer.append("package ").append(pkg).append(";\n\n");

        buffer.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
        buffer.append("import io.github.bitfist.jcef.spring.jsexecution.JavaScriptExecutor;\n\n");

        buffer.append("import java.util.logging.Logger;\n\n");

        buffer.append("@org.springframework.stereotype.Component\n");
        buffer.append("class ").append(className).append(" implements ").append(interfaceName).append(" {\n\n");

        buffer.append("    private static final Logger log = Logger.getLogger(").append(className).append(".class.getName());\n\n");

        buffer.append("    private final JavaScriptExecutor executor;\n");
        buffer.append("    private final ObjectMapper objectMapper;\n\n");

        buffer.append("    public ").append(className)
                .append("(JavaScriptExecutor executor, @org.springframework.beans.factory.annotation.Qualifier(\"cefBrowserObjectMapper\") ObjectMapper objectMapper) {\n");
        buffer.append("        this.executor = executor;\n");
        buffer.append("        this.objectMapper = objectMapper;\n");
        buffer.append("    }\n\n");
    }

    private void generateMethodCode(ExecutableElement method, StringBuilder buffer) {
        var methodName = method.getSimpleName().toString();
        var code = method.getAnnotation(JavaScriptCode.class).value().replace("\"", "\\\"");
        var codeField = methodName.toUpperCase() + "_CODE_" + generatedMethods;
        generatedMethods++;

        buffer.append("    private static final String ").append(codeField)
                .append(" = \"\"\"\n    ").append(code).append("\n    \"\"\";\n\n");

        // method signature
        buffer.append("    @Override\n");
        buffer.append("    public void ").append(methodName).append("(");
        List<? extends VariableElement> parameters = method.getParameters();
        for (var i = 0; i < parameters.size(); i++) {
            var p = parameters.get(i);
            buffer.append(p.asType().toString()).append(" ").append(p.getSimpleName().toString());
            if (i < parameters.size() - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(") {\n");

        buffer.append("        String code = ").append(codeField).append(";\n");
        if (!parameters.isEmpty()) {
            buffer.append("        // region Variable replacements\n");
        }
        for (VariableElement parameter : parameters) {
            var parameterName = parameter.getSimpleName().toString();
            if (parameter.asType().getKind().isPrimitive()) {
                buffer.append("        code = code.replace(\":").append(parameterName)
                        .append("\", String.valueOf(").append(parameterName).append("));\n");
            } else {
                buffer.append("        try {\n");
                buffer.append("            code = code.replace(\":").append(parameterName)
                        .append("\", objectMapper.writeValueAsString(").append(parameterName)
                        .append("));\n");
                buffer.append("        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {\n");
                buffer.append("            throw new RuntimeException(e);\n");
                buffer.append("        }\n");
            }
        }
        if (!parameters.isEmpty()) {
            buffer.append("        // endregion\n");
        }
        buffer.append("        code = code.trim();\n");
        buffer.append("        log.fine(\"Executing code\\n\" + code);\n");
        buffer.append("        executor.execute(code);\n");
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            buffer.append("        return null;\n");
        }
        buffer.append("    }\n\n");
    }

    private static void finishClass(StringBuilder buffer) {
        buffer.append("}\n");
    }
}