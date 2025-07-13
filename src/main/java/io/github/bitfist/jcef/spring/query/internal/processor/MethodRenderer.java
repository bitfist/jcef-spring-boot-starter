package io.github.bitfist.jcef.spring.query.internal.processor;

import io.github.bitfist.jcef.spring.query.CefQueryHandler;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 🔄 Generates static TypeScript methods for each handler annotated with @CefQueryHandler.
 */
final class MethodRenderer {

    private static final Pattern PATH_PARAM = Pattern.compile("\\{(\\w+)}");

    @SuppressWarnings("unchecked")
    void render(BufferedWriter writer,
                String classRoute,
                ExecutableElement executableElement,
                Messager log) throws IOException {

        // 1) build full route
        String route = executableElement.getAnnotation(CefQueryHandler.class).value();
        String full = (classRoute.endsWith("/") || route.startsWith("/"))
                ? classRoute + route
                : classRoute + "/" + route;

        // 2) detect route placeholders
        Matcher matcher = PATH_PARAM.matcher(full);
        Set<String> pathParams = new LinkedHashSet<>();
        while (matcher.find()) {
            pathParams.add(matcher.group(1));
        }

        // 3) build TS template (back-tick)
        String routeExpr = pathParams.isEmpty()
                ? "\"" + full + "\""
                : "`" + pathParams.stream()
                .reduce(full, (acc, p) -> acc.replace("{" + p + "}", "${" + p + "}"))
                + "`";

        // 4) split parameters
        List<VariableElement> params = (List<VariableElement>) executableElement.getParameters();
        List<VariableElement> payload = params.stream()
                .filter(p -> !pathParams.contains(p.getSimpleName().toString()))
                .toList();
        if (payload.size() > 1) {
            log.printMessage(
                    Kind.ERROR,
                    "Method %s has more than one payload parameter".formatted(
                            executableElement.getSimpleName()
                    )
            );
        }
        VariableElement body = payload.isEmpty() ? null : payload.getFirst();

        // 5) signature + return type
        String signature = params.stream()
                .map(p -> p.getSimpleName() + ": " + TsTypeMapper.map(p.asType()))
                .collect(Collectors.joining(", "));
        String ret = TsTypeMapper.map(executableElement.getReturnType());

        // 6) determine the literal string for responseType
        String responseTypeLiteral = switch (ret) {
            case "boolean" -> "\"boolean\"";
            case "number" -> "\"number\"";
            case "string" -> "\"string\"";
            default -> "\"object\"";
        };
        responseTypeLiteral = responseTypeLiteral + " as ResponseType";

        // --- emit ------------------------------------------------------------
        writer.write("  /** route: " + full + " */\n");
        writer.write("  static " + executableElement.getSimpleName() + "("
                + signature
                + "): Promise<" + ret + "> {\n");

        // build argument list: route, [body], and the inferred responseType string
        String args = (body == null
                ? routeExpr
                : routeExpr + ", " + body.getSimpleName())
                + ", " + responseTypeLiteral;

        writer.write("    return CefQueryService.request<" + ret + ">(" + args + ");\n");
        writer.write("  }\n\n");
    }
}
