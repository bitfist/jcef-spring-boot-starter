package io.github.bitfist.jcef.spring.query.internal.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

/**
 * 🛠 Annotation processor that converts @CefQueryHandler annotated Spring beans
 * into TypeScript client stubs at compile time.
 */
@SupportedAnnotationTypes("io.github.bitfist.jcef.spring.query.CefQueryHandler")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions({TypeScriptGeneratorProcessor.OPT_OUTPUT_PATH, TypeScriptGeneratorProcessor.OPT_OUTPUT_TYPE})
public class TypeScriptGeneratorProcessor extends AbstractProcessor {

    static final String OPT_OUTPUT_TYPE = "jcef.output.type";
    static final String OPT_OUTPUT_PATH = "jcef.output.path";

    private Messager log;
    private ResourceCopier copier;
    private ModelScanner scanner;
    private TsFileWriter writer;

    void initialize(ResourceCopier copier, ModelScanner scanner, TsFileWriter writer) {
        this.copier = copier;
        this.scanner = scanner;
        this.writer = writer;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.log = env.getMessager();

        String outType = env.getOptions().getOrDefault(OPT_OUTPUT_TYPE, "typescript");
        String outPath = env.getOptions().getOrDefault(OPT_OUTPUT_PATH, "");

        if (!"typescript".equalsIgnoreCase(outType)) {
            warn("Skipping TypeScript generation - unsupported output type: %s", outType);
            return;
        }
        if (outPath.isBlank()) {
            warn("Skipping TypeScript generation – " + OPT_OUTPUT_PATH + " not specified");
            return;
        }

        Path baseDir = Paths.get(outPath).toAbsolutePath();
        ResourceCopier copier = new ResourceCopier(log, baseDir);
        TsFileWriter writer = new TsFileWriter(log, baseDir);
        ModelScanner scanner = new ModelScanner(env.getElementUtils());
        initialize(copier, scanner, writer);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        // 1) copy static helper scripts once
        copier.copyStaticAssets();

        // 2) scan this round for annotated classes/methods
        Collection<HandlerModel> handlerModels = scanner.scan(round);

        // 3) generate TS for each controller
        handlerModels.forEach(this::writeHandler);
        return true;
    }

    protected void writeHandler(HandlerModel handlerModel) {
        try {
            writer.write(handlerModel);
        } catch (IOException ex) {
            error("Failed to generate TS for %s: %s", handlerModel.javaType().getSimpleName(), ex.getMessage());
        }
    }

    private void warn(String fmt, Object... args) {
        log.printMessage(Kind.WARNING, String.format(fmt, args));
    }

    private void error(String fmt, Object... args) {
        log.printMessage(Kind.ERROR, String.format(fmt, args));
    }
}
