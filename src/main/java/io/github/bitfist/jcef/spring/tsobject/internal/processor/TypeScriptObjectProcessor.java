package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptObject;
import org.jspecify.annotations.Nullable;

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
        TypeScriptObjectProcessor.JCEF_OUTPUT_PATH_OPTION, TypeScriptObjectProcessor.JCEF_SERVICE_TYPE_OPTION,
        TypeScriptObjectProcessor.JCEF_WEB_HOST_OPTION, TypeScriptObjectProcessor.JCEF_WEB_PORT_OPTION
})
public class TypeScriptObjectProcessor extends AbstractProcessor {

    public static final String JCEF_OUTPUT_PATH_OPTION = "jcef.output.path";
    public static final String JCEF_SERVICE_TYPE_OPTION = "jcef.output.service.type";
    public static final String JCEF_WEB_HOST_OPTION = "jcef.output.web.host";
    public static final String JCEF_WEB_PORT_OPTION = "jcef.output.web.port";

    public static final String JCEF_SERVICE_TYPE_WEB = "web";
    public static final String JCEF_SERVICE_TYPE_QUERY = "query";
    public static final String JCEF_WEB_HOST = "http://localhost";
    public static final String JCEF_WEB_PORT = "8080";

    private Messager messager;
    private TypeScriptGenerator typeScriptGenerator;
    private boolean supportFilesCopied = false;
    private String outputPath;
    private ServiceType serviceType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();

        outputPath = processingEnv.getOptions().get(JCEF_OUTPUT_PATH_OPTION);
        if (outputPath == null || outputPath.isBlank()) {
            messager.printError("Required option " + JCEF_OUTPUT_PATH_OPTION + " is not set.");
            return;
        }

        serviceType = getServiceType(processingEnv);
        if (serviceType == null) {
            return;
        }

        this.typeScriptGenerator = new TypeScriptGenerator(outputPath, serviceType, processingEnv.getElementUtils());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        var sourceVersions = SourceVersion.values();
        return sourceVersions[sourceVersions.length - 1];
    }

    private @Nullable ServiceType getServiceType(ProcessingEnvironment processingEnv) {
        var serviceType = processingEnv.getOptions().get(JCEF_SERVICE_TYPE_OPTION);
        if (isBlank(serviceType)) {
            messager.printError("No service type defined. Must be one of " + JCEF_SERVICE_TYPE_WEB + " or " + JCEF_SERVICE_TYPE_QUERY);
            return null;
        }
        try {
            return ServiceType.valueOf(serviceType.toUpperCase());
        } catch (IllegalArgumentException exception) {
            messager.printError("Invalid service type: " + serviceType + ". Must be one of " + JCEF_SERVICE_TYPE_WEB + " or " + JCEF_SERVICE_TYPE_QUERY);
            return null;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
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

        String service = serviceType == ServiceType.WEB ? "CefRestService" : "CefQueryService";


        // Copy service file
        Path cefQueryServiceDest = Path.of(this.outputPath, "jcef", service + ".ts");
        if (serviceType == ServiceType.QUERY) {
            copyFileFromClasspath("generator/templates/CefQueryService.ts", cefQueryServiceDest, Function.identity());
        } else {
            Function<String, String> processor = content -> {
                content = content.replace("$host", processingEnv.getOptions().getOrDefault(JCEF_WEB_HOST_OPTION, JCEF_WEB_HOST));
                content = content.replace("$port", processingEnv.getOptions().getOrDefault(JCEF_WEB_PORT_OPTION, JCEF_WEB_PORT));
                return content;
            };
            copyFileFromClasspath("generator/templates/CefRestService.ts", cefQueryServiceDest, processor);
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