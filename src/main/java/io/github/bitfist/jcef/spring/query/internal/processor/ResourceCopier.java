package io.github.bitfist.jcef.spring.query.internal.processor;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * 📋 Copies static template files (TS helper classes) from the processor JAR
 * into the configured output directory.
 */
final class ResourceCopier {

    private static final Map<String, String> RESOURCES = Map.of(
            "/generator/templates/CefQueryService.ts", "cef/CefQueryService.ts",
            "/generator/templates/cef.d.ts", "types/cef.d.ts"
    );

    private final Messager log;
    private final Path baseDir;
    private boolean copied = false;

    ResourceCopier(Messager log, Path baseDir) {
        this.log = log;
        this.baseDir = baseDir;
    }

    void copyStaticAssets() {
        if (copied) return;          // only once per compilation
        RESOURCES.forEach((src, dst) -> {
            try (InputStream in = ResourceCopier.class.getResourceAsStream(src)) {
                if (in == null) throw new IllegalStateException("Missing resource: " + src);
                Path target = baseDir.resolve(dst);
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                log.printMessage(Kind.ERROR, "Copying %s failed: %s".formatted(src, ex.getMessage()));
            }
        });
        copied = true;
    }
}
