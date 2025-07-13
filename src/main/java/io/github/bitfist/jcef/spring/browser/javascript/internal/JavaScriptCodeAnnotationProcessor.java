package io.github.bitfist.jcef.spring.browser.javascript.internal;

import io.github.bitfist.jcef.spring.browser.javascript.JavaScriptCode;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@SupportedAnnotationTypes("io.github.bitfist.jcef.spring.browser.javascript.JavaScriptCode")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JavaScriptCodeAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.filer = env.getFiler();
        this.messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, List<ExecutableElement>> interfacesAndMethods = new HashMap<>();
        for (Element e : roundEnv.getElementsAnnotatedWith(JavaScriptCode.class)) {
            if (e.getKind() != ElementKind.METHOD) continue;
            ExecutableElement m = (ExecutableElement) e;
            TypeElement iface = (TypeElement) m.getEnclosingElement();
            interfacesAndMethods.computeIfAbsent(iface, k -> new ArrayList<>()).add(m);
        }

        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : interfacesAndMethods.entrySet()) {
            TypeElement iface = entry.getKey();
            List<ExecutableElement> methods = entry.getValue();
            String pkg = processingEnv.getElementUtils().getPackageOf(iface).getQualifiedName().toString();
            String interfaceName = iface.getSimpleName().toString();
            String className = interfaceName + "Impl";

            StringBuilder buffer = new StringBuilder();
            buffer.append("package ").append(pkg).append(";\n\n");

            buffer.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
            buffer.append("import io.github.bitfist.jcef.spring.browser.javascript.JavaScriptExecutor;\n\n");

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

            for (ExecutableElement method : methods) {
                String methodName = method.getSimpleName().toString();
                String code = method.getAnnotation(JavaScriptCode.class).value().replace("\"", "\\\"");
                String codeField = methodName.toUpperCase() + "_CODE_" + method.hashCode();

                buffer.append("    private static final String ").append(codeField)
                        .append(" = \"\"\"\n    ").append(code).append("\n    \"\"\";\n\n");

                // method signature
                buffer.append("    @Override\n");
                buffer.append("    public void ").append(methodName).append("(");
                List<? extends VariableElement> parameters = method.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement p = parameters.get(i);
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
                    String parameterName = parameter.getSimpleName().toString();
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

            buffer.append("}\n");

            try {
                JavaFileObject file = filer.createSourceFile(pkg + "." + className);
                Writer w = file.openWriter();
                w.write(buffer.toString());
                w.close();
            } catch (IOException ex) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write " + className + ": " + ex.getMessage());
            }
        }
        return true;
    }
}