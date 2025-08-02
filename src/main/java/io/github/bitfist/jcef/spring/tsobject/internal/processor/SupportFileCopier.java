package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

@RequiredArgsConstructor
class SupportFileCopier {

	static final String BACKEND_URI_PLACEHOLDER = "$backendUri";

	private final String outputPath;
	private final boolean webCommunicationEnabled;
	private final String webBackendUri;

	void copySupportFiles() throws IOException {
		// Copy service file
		Path cefServiceDest = Path.of(this.outputPath, "jcef", "CefCommunicationService.ts");
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

		try (var inputStream = SupportFileCopier.class.getClassLoader().getResourceAsStream(sourceClasspath)) {
			if (inputStream == null) {
				throw new IOException("Cannot find resource '" + sourceClasspath + "' on the classpath.");
			}

			// Read all bytes and convert to string
			var original = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

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
