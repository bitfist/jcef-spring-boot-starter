package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptClass;
import io.github.bitfist.jcef.spring.tsobject.TypeScriptService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SupportedAnnotationTypes({
		"io.github.bitfist.jcef.spring.tsobject.TypeScriptClass",
		"io.github.bitfist.jcef.spring.tsobject.TypeScriptService"
})
@SupportedOptions({
		TypeScriptProcessor.JCEF_OUTPUT_PATH_OPTION,
		TypeScriptProcessor.JCEF_WEB_COMMUNICATION_ENABLED_OPTION,
		TypeScriptProcessor.JCEF_WEB_BACKEND_URI_OPTION
})
public class TypeScriptProcessor extends AbstractProcessor {

	static final String JCEF_OUTPUT_PATH_OPTION = "jcef.output.path";
	static final String JCEF_WEB_COMMUNICATION_ENABLED_OPTION = "jcef.web.communication.enabled";
	static final String JCEF_WEB_BACKEND_URI_OPTION = "jcef.web.backend.uri";

	static final String DEFAULT_WEB_BACKEND_URI = "http://localhost:8080";

	private Messager messager;
	private boolean supportFilesCopied = false;

	// options
	private String outputPath;
	private boolean webCommunicationEnabled = false;
	private String webBackendUri = DEFAULT_WEB_BACKEND_URI;

	private TypeScriptModelGenerator modelGenerator;

	@Override
	public SourceVersion getSupportedSourceVersion() {
		var sourceVersions = SourceVersion.values();
		// return latest version
		return sourceVersions[sourceVersions.length - 1];
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		modelGenerator = new TypeScriptModelGenerator(processingEnv.getElementUtils());
		initializeOptions();
	}

	private void initializeOptions() {
		outputPath = processingEnv.getOptions().get(JCEF_OUTPUT_PATH_OPTION);
		webCommunicationEnabled = Boolean.parseBoolean(processingEnv.getOptions().getOrDefault(JCEF_WEB_COMMUNICATION_ENABLED_OPTION, "false"));
		webBackendUri = processingEnv.getOptions().getOrDefault(JCEF_WEB_BACKEND_URI_OPTION, DEFAULT_WEB_BACKEND_URI);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (optionsAreInvalid()) {
			return false;
		}

		if (roundEnv.processingOver()) {
			try {
				copySupportFiles();
			} catch (IOException e) {
				messager.printMessage(Diagnostic.Kind.ERROR, "Failed to copy JCEF support files: " + e.getMessage());
				return false; // Stop processing if support files fail to copy
			}
			generateTypeScriptFiles();
			return true;
		}

		// Process @TypeScriptClass annotations
		for (Element element : roundEnv.getElementsAnnotatedWith(TypeScriptClass.class)) {
			if (element.getKind() == ElementKind.CLASS) {
				modelGenerator.processClass((TypeElement) element);
			} else if (element.getKind() == ElementKind.ENUM) {
				modelGenerator.processEnum((TypeElement) element);
			}
		}

		// Process @TypeScriptService annotations
		for (Element element : roundEnv.getElementsAnnotatedWith(TypeScriptService.class)) {
			if (element.getKind() == ElementKind.CLASS) {
				modelGenerator.processService((TypeElement) element);
			}
		}

		return true;
	}

	private boolean optionsAreInvalid() {
		var invalid = false;

		if (isBlank(outputPath)) {
			messager.printError("Required option " + JCEF_OUTPUT_PATH_OPTION + " is not set.");
			invalid = true;
		}

		return invalid;
	}

	private void copySupportFiles() throws IOException {
		if (supportFilesCopied) {
			return;
		}
		supportFilesCopied = true;
		new SupportFileCopier(outputPath, webCommunicationEnabled, webBackendUri)
				.copySupportFiles();
		messager.printMessage(Diagnostic.Kind.NOTE, "Successfully copied JCEF support files.");
	}

	private void generateTypeScriptFiles() {
		var classModel = modelGenerator.getClassModel();
		var classGenerator = new TypeScriptClassGenerator();
		var enumGenerator = new TypeScriptEnumGenerator();
		var serviceGenerator = new TypeScriptServiceGenerator();

		for (TSClass tsClass : classModel.values()) {
			try {
				var content = switch (tsClass.getType()) {
					case CLASS -> classGenerator.generate(tsClass, classModel);
					case ENUM -> enumGenerator.generate(tsClass);
					case SERVICE -> serviceGenerator.generate(tsClass, classModel);
				};

				writeFile(tsClass, content);
			} catch (IOException e) {
				messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate TypeScript for " + tsClass.getJavaClassName() + ": " + e.getMessage());
			}
		}
	}

	private void writeFile(TSClass tsClass, String content) throws IOException {
		var filePath = Paths.get(outputPath, tsClass.getOutputPath(), tsClass.getTsClassName() + ".ts");
		filePath.getParent().toFile().mkdirs();
		Files.writeString(
				filePath,
				content,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);
	}
}