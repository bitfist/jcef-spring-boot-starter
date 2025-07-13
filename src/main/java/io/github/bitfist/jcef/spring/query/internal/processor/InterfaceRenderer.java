package io.github.bitfist.jcef.spring.query.internal.processor;

import lombok.SneakyThrows;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * 🖼 Generates TypeScript interfaces for complex DTO types.
 */
final class InterfaceRenderer {

    @SneakyThrows
    void render(BufferedWriter writer, TypeElement dto) {
        writer.write("export interface " + dto.getSimpleName() + " {\n");
        List<VariableElement> list = dto.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast).toList();
        list
                .forEach(f -> {
                    try {
                        writer.write("  " + f.getSimpleName() + ": " + TsTypeMapper.map(f.asType()) + ";\n");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        writer.write("}\n\n");
    }
}
