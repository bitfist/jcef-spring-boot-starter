package io.github.bitfist.jcef.spring.query.internal.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ✍️ Writes complete `.ts` files for each HandlerModel,
 * including imports, interfaces, and class stub methods.
 */
final class TsFileWriter {

    private final Messager log;
    private final Path baseDir;
    private final HeaderRenderer headerRenderer = new HeaderRenderer();
    private final InterfaceRenderer interfaceRenderer = new InterfaceRenderer();
    private final ClassRenderer classRenderer = new ClassRenderer();
    private final MethodRenderer methodRenderer = new MethodRenderer();

    TsFileWriter(Messager log, Path baseDir) {
        this.log = log;
        this.baseDir = baseDir;
    }

    void write(HandlerModel model) throws IOException {
        String className = model.javaType().getSimpleName().toString();
        Path dir = baseDir.resolve(model.tsPackage().replace('.', '/'));
        Path file = dir.resolve(className + ".ts");
        Files.createDirectories(dir);

        int importDepth = model.tsPackage().isEmpty() ? 0 : model.tsPackage().split("\\.").length;

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            headerRenderer.render(writer, importDepth);
            model.complexDtos().values().forEach(te -> interfaceRenderer.render(writer, te));
            classRenderer.start(writer, className);
            for (ExecutableElement m : model.handlerMethods()) {
                methodRenderer.render(writer, model.routePrefix(), m, log);
            }
            classRenderer.end(writer);
        }
    }
}
