package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptObject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@SupportedAnnotationTypes({
        "io.github.bitfist.jcef.spring.tsobject.TypeScriptObject",
        "io.github.bitfist.jcef.spring.tsobject.TypeScriptConfiguration"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions(TypeScriptObjectProcessor.JCEF_OUTPUT_PATH_OPTION)
public class TypeScriptObjectProcessor extends AbstractProcessor {

    public static final String JCEF_OUTPUT_PATH_OPTION = "jcef.output.path";

    private Messager messager;
    private TypeScriptGenerator typeScriptGenerator;
    private boolean supportFilesCopied = false;
    private String outputPath;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        outputPath = processingEnv.getOptions().get(JCEF_OUTPUT_PATH_OPTION);

        if (outputPath == null || outputPath.isBlank()) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Required option " + JCEF_OUTPUT_PATH_OPTION + " is not set.");
            return;
        }

        this.typeScriptGenerator = new TypeScriptGenerator(outputPath, processingEnv.getElementUtils(), messager);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (typeScriptGenerator == null) {
            return false;
        }

        Set<? extends Element> javascriptObjects = roundEnv.getElementsAnnotatedWith(TypeScriptObject.class);

        if (javascriptObjects.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE, "No @JavaScriptObject annotations found.");
            return false;
        }

        // Check if any annotated elements have methods and copy support files if they do.
        if (!this.supportFilesCopied) {
            try {
                copySupportFiles();
                this.supportFilesCopied = true;
                messager.printMessage(Diagnostic.Kind.NOTE, "Successfully copied JCEF support files.");
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to copy JCEF support files: " + e.getMessage());
                return false; // Stop processing if support files fail to copy
            }
        }

        for (Element element : javascriptObjects) {
            if (element instanceof TypeElement) {
                try {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing @JavaScriptObject on " + element.getSimpleName());
                    typeScriptGenerator.generate((TypeElement) element);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate TypeScript file for " + element.getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        return false;
    }

    /**
     * Copies required TypeScript support files from the classpath to the output directory.
     */
    private void copySupportFiles() throws IOException {
        messager.printMessage(Diagnostic.Kind.NOTE, "Copying CefQueryService.ts and cef.d.ts to output path.");

        // Define source and destination paths
        Path cefQueryServiceDest = Path.of(this.outputPath, "jcef", "CefQueryService.ts");
        Path cefDtsDest = Path.of(this.outputPath, "types", "cef.d.ts");

        // Copy files
        copyFileFromClasspath("generator/templates/CefQueryService.ts", cefQueryServiceDest);
        copyFileFromClasspath("generator/templates/cef.d.ts", cefDtsDest);
    }

    /**
     * Helper method to copy a single file from the classpath to a destination path.
     *
     * @param sourceClasspath The path to the resource on the classpath.
     * @param destinationPath The path to the destination file.
     * @throws IOException if the resource is not found or the file cannot be written.
     */
    private void copyFileFromClasspath(String sourceClasspath, Path destinationPath) throws IOException {
        // Ensure the parent directory exists
        Files.createDirectories(destinationPath.getParent());

        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(sourceClasspath)) {
            if (inputStream == null) {
                throw new IOException("Cannot find resource '" + sourceClasspath + "' on the classpath.");
            }
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}