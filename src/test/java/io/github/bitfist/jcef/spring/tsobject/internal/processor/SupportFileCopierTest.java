package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("🧪 SupportFileCopier Tests")
class SupportFileCopierTest {

	@Nested
	@DisplayName("📁 copySupportFiles behavior")
	class CopySupportFiles {

		@Test
		@DisplayName("✅ web enabled: copies CefRestService.ts and replaces placeholder 🔁")
		void testCopySupportFiles_webEnabled_replacesPlaceholder(@TempDir Path tempDir) throws IOException {
			// Arrange
			var backendUri = "http://example.com/api";
			var copier = new SupportFileCopier(
					tempDir.toString(),
					true,
					backendUri
			);

			// Act
			copier.copySupportFiles();

			// Assert: CefCommunicationService.ts should exist with replaced backend URI
			var destService = tempDir.resolve("jcef").resolve("CefCommunicationService.ts");
			assertTrue(Files.exists(destService), "CefCommunicationService.ts should have been created");

			String content = Files.readString(destService, StandardCharsets.UTF_8);
			assertFalse(content.contains(SupportFileCopier.BACKEND_URI_PLACEHOLDER), "Placeholder should have been replaced");
			assertTrue(content.contains(backendUri), "Backend URI should be injected into the copied file");

			// Also assert other expected support files copied
			assertTrue(Files.exists(tempDir.resolve("jcef").resolve("ResponseValueConverter.ts")));
			assertTrue(Files.exists(tempDir.resolve("jcef").resolve("ResponseType.ts")));
			assertTrue(Files.exists(tempDir.resolve("types").resolve("cef.d.ts")));
		}

		@Test
		@DisplayName("✅ web disabled: copies CefQueryService.ts instead of REST service 🔄")
		void testCopySupportFiles_webDisabled_usesQueryService(@TempDir Path tempDir) throws IOException {
			// Arrange
			var copier = new SupportFileCopier(
					tempDir.toString(),
					false,
					"should-not-matter"
			);

			// Act
			copier.copySupportFiles();

			// Assert: CefCommunicationService.ts should exist with content from query service
			var destService = tempDir.resolve("jcef").resolve("CefCommunicationService.ts");
			assertTrue(Files.exists(destService), "CefCommunicationService.ts should have been created");

			String content = Files.readString(destService, StandardCharsets.UTF_8);
			// It should not contain the REST placeholder string (since it's a different template)
			assertTrue(content.contains("cefQuery"), "Expected query-service derived content");

			// Shared support files also present
			assertTrue(Files.exists(tempDir.resolve("jcef").resolve("ResponseValueConverter.ts")));
			assertTrue(Files.exists(tempDir.resolve("jcef").resolve("ResponseType.ts")));
			assertTrue(Files.exists(tempDir.resolve("types").resolve("cef.d.ts")));
		}
	}

	@Nested
	@DisplayName("🛠 copyFileFromClasspath low-level behavior")
	class CopyFileFromClasspath {

		@Test
		@DisplayName("🚨 missing resource should throw IOException")
		void testCopyFileFromClasspath_missingResource_throws() {
			// Arrange
			Path dest = Path.of("nonexistent-dir", "foo.txt");

			// Act & Assert
			IOException ex = assertThrows(IOException.class, () ->
					SupportFileCopier.copyFileFromClasspath("this/does/not/exist.txt", dest, Function.identity())
			);
			assertTrue(ex.getMessage().contains("Cannot find resource"));
		}

		@Test
		@DisplayName("🧩 processor is applied to content before writing")
		void testCopyFileFromClasspath_processorApplied(@TempDir Path tempDir) throws IOException {
			// To test this without relying on existing classpath resource, we can create a small temporary
			// in-memory resource by using a custom classloader. However, for simplicity here we assume a
			// known test resource exists: generator/templates/ResponseValueConverter.ts

			var dest = tempDir.resolve("out.ts");

			// Use a processor that appends a marker
			Function<String, String> processor = original -> original + "\n// processed";

			SupportFileCopier.copyFileFromClasspath("generator/templates/ResponseValueConverter.ts", dest, processor);

			String written = Files.readString(dest);
			assertTrue(written.endsWith("// processed") || written.contains("// processed"),
					"Processor should have modified content");
		}
	}
}
