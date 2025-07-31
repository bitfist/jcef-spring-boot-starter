package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for UIInstaller, verifying synchronization of UI resources.
 */
class UIInstallerTest {

	@Mock
	private JcefApplicationProperties properties;
	private UIInstaller installer;

	@TempDir
	Path tempDir;

	UIInstallerTest() {
		// Initialize Mockito annotations
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("📁 should copy UI resources when installation directory is empty")
	void shouldCopyResourcesWhenDirectoryEmpty() throws IOException {
		// Arrange: mock properties to point to a temporary dir and test classpath
		when(properties.getUiInstallationPath()).thenReturn(tempDir);
		when(properties.getDistributionClasspath()).thenReturn("ui");

		// Prepare installer
		installer = new UIInstaller(properties);

		// Act: perform initialization which should install resources
		installer.initialize();

		// Assert: expected resource (e.g., index.html) exists in the target location
		var expected = tempDir.resolve("index.html");
		assertTrue(Files.exists(expected), "Expected index.html to be copied");
		assertTrue(Files.size(expected) > 0, "Copied file should not be empty");
	}

	@Test
	@DisplayName("✅ should not copy UI resources when up-to-date")
	void shouldNotCopyResourcesWhenUpToDate() throws IOException {
		// Arrange: mock properties
		when(properties.getUiInstallationPath()).thenReturn(tempDir);
		when(properties.getDistributionClasspath()).thenReturn("ui");

		// Prepare installer
		installer = new UIInstaller(properties);

		// Simulate existing file with newer timestamp
		var existing = tempDir.resolve("index.html");
		Files.createDirectories(existing.getParent());
		var original = "original-content";
		Files.writeString(existing, original, StandardOpenOption.CREATE);
		// Set file timestamp ahead of classpath resource
		existing.toFile().setLastModified(System.currentTimeMillis() + 10_000);

		// Act
		installer.initialize();

		// Assert: file should remain unchanged
		String content = Files.readString(existing);
		assertEquals(original, content, "Existing up-to-date file should not be overwritten");
	}
}
