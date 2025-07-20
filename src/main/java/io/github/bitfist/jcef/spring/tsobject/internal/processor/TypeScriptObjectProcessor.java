package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptObject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SupportedAnnotationTypes({
        "io.github.bitfist.jcef.spring.tsobject.TypeScriptObject",
        "io.github.bitfist.jcef.spring.tsobject.TypeScriptConfiguration"
})
@SupportedOptions({
        TypeScriptObjectProcessor.JCEF_OUTPUT_PATH_OPTION,
        TypeScriptObjectProcessor.JCEF_WEB_COMMUNICATION_ENABLED_OPTION,
        TypeScriptObjectProcessor.JCEF_WEB_BACKEND_URI_OPTION
})
public class TypeScriptObjectProcessor extends AbstractProcessor {

    static final String JCEF_OUTPUT_PATH_OPTION = "jcef.output.path";
    static final String JCEF_WEB_COMMUNICATION_ENABLED_OPTION = "jcef.web.communication.enabled";
    static final String JCEF_WEB_BACKEND_URI_OPTION = "jcef.web.backend.uri";

    static final String DEFAULT_WEB_BACKEND_URI = "http://localhost:8080";
    static final String BACKEND_URI_PLACEHOLDER = "$backendUri";

    private Messager messager;
    private TypeScriptGenerator typeScriptGenerator;
    private boolean supportFilesCopied = false;
    private String outputPath;
    private boolean webCommunicationEnabled = false;
    private String webBackendUri = DEFAULT_WEB_BACKEND_URI;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        var sourceVersions = SourceVersion.values();
        // return latest version
        return sourceVersions[sourceVersions.length - 1];
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        
        initializeOptions();

        this.typeScriptGenerator = new TypeScriptGenerator(outputPath, processingEnv.getElementUtils());
    }
    
    private void initializeOptions() {
        outputPath = processingEnv.getOptions().get(JCEF_OUTPUT_PATH_OPTION);
        webCommunicationEnabled = Boolean.parseBoolean(processingEnv.getOptions().getOrDefault(JCEF_WEB_COMMUNICATION_ENABLED_OPTION, "false"));
        webBackendUri = processingEnv.getOptions().getOrDefault(JCEF_WEB_BACKEND_URI_OPTION, DEFAULT_WEB_BACKEND_URI);
    }
    
    private boolean optionsAreInvalid() {
        var invalid = false;

        if (isBlank(outputPath)) {
            messager.printError("Required option " + JCEF_OUTPUT_PATH_OPTION + " is not set.");
            invalid = true;
        }

        return invalid;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (optionsAreInvalid()) {
            return false;
        }

        var javascriptObjects = roundEnv.getElementsAnnotatedWith(TypeScriptObject.class);
        if (javascriptObjects.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE, "No @TypeScriptObject annotations found.");
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
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing @TypeScriptObject on " + element.getSimpleName());
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

        // Copy service file
        Path cefServiceDest = Path.of(this.outputPath, "jcef", TypeScriptGenerator.CEF_COMMUNICATION_SERVICE_NAME + ".ts");
        if (webCommunicationEnabled) {
            Function<String, String> processor = content -> {
                content = content.replace(BACKEND_URI_PLACEHOLDER, webBackendUri);
                return content;
            };
            copyFileFromClasspath("generator/templates/CefRestService.ts", cefServiceDest, processor);
        } else {
            copyFileFromClasspath("generator/templates/CefQueryService.ts", cefServiceDest, Function.identity());
        }

        copyFileFromClasspath("generator/templates/ResponseValueConverter.ts", Path.of(this.outputPath, "jcef", "ResponseValueConverter.ts"), Function.identity());
        copyFileFromClasspath("generator/templates/ResponseType.ts", Path.of(this.outputPath, "jcef", "ResponseType.ts"), Function.identity());
        copyFileFromClasspath("generator/templates/cef.d.ts", Path.of(this.outputPath, "types", "cef.d.ts"), Function.identity());
    }

    /**
     * Copy a resource from the classpath to a destination path, processing its content on the fly.
     *
     * @param sourceClasspath the classpath resource location
     * @param destinationPath the file system destination
     * @param processor       a Function that takes the file content as input and returns processed content
     * @throws IOException if the resource is missing or writing fails
     */
    public static void copyFileFromClasspath(String sourceClasspath, Path destinationPath, Function<String, String> processor) throws IOException {
        // Ensure the parent directory exists
        Files.createDirectories(destinationPath.getParent());

        try (var inputStream = TypeScriptObjectProcessor.class.getClassLoader().getResourceAsStream(sourceClasspath)) {
            if (inputStream == null) {
                throw new IOException("Cannot find resource '" + sourceClasspath + "' on the classpath.");
            }

            // Read all bytes and convert to string
            var original = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // Process content
            var processed = processor.apply(original);

            // Write processed content
            Files.writeString(
                    destinationPath,
                    processed,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }
}