package io.github.bitfist.jcef.spring.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JcefApplicationProperties}.
 */
@ExtendWith(MockitoExtension.class)
class JcefApplicationPropertiesTest {

	private static final String TEST_APP_NAME = "MyTestApp";

	@Test
	@DisplayName("🗂️ Constructor should default distribution classpath to 'ui/' if null")
	void constructor_whenDistributionPathIsNull_thenDefaultsToUI() {
		var environment = mock(Environment.class);
		when(environment.getProperty("jcef.application-name")).thenReturn(TEST_APP_NAME);
		when(environment.getProperty("jcef.installation-path")).thenReturn("win");
		// Mock ClassPathResource to simulate that 'ui/index.html' exists.
		try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> {
			if ("ui/index.html".equals(context.arguments().getFirst())) {
				when(mock.exists()).thenReturn(true);
			}
		})) {
			var properties = new JcefApplicationProperties(null, null);
			properties.setEnvironment(environment);

			assertEquals("ui/", properties.getDistributionClasspath(), "Classpath should default to 'ui/'");
		}
	}

	@Test
	@DisplayName("🗂️ Constructor should default distribution classpath to 'ui/' if blank")
	void constructor_whenDistributionPathIsBlank_thenDefaultsToUI() {
		// Mock ClassPathResource to simulate that 'ui/index.html' exists.
		try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> {
			if ("ui/index.html".equals(context.arguments().getFirst())) {
				when(mock.exists()).thenReturn(true);
			}
		})) {
			var properties = new JcefApplicationProperties(null, "  ");
			assertEquals("ui/", properties.getDistributionClasspath(), "Classpath should default to 'ui/' for a blank string");
		}
	}

	@Test
	@DisplayName("✅ Constructor should add trailing slash to distribution classpath if missing")
	void constructor_whenDistributionPathIsProvided_thenUsesItAndAddsSlash() {
		// Mock ClassPathResource to simulate that 'custom/path/index.html' exists.
		try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> {
			if ("custom/path/index.html".equals(context.arguments().getFirst())) {
				when(mock.exists()).thenReturn(true);
			}
		})) {
			var properties = new JcefApplicationProperties(null, "custom/path");
			assertEquals("custom/path/", properties.getDistributionClasspath(), "A trailing slash should be added");
		}
	}

	@Test
	@DisplayName("🚫 Constructor should throw exception if index.html is not found")
	void constructor_whenIndexHtmlDoesNotExist_thenThrowsException() {
		// Mock ClassPathResource to simulate that 'invalid/path/index.html' does NOT exist.
		try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> {
			// We can also assert that the constructor is checking the correct file path.
			assertEquals("invalid/path/index.html", context.arguments().getFirst());
			when(mock.exists()).thenReturn(false);
		})) {
			var ex = assertThrows(IllegalArgumentException.class,
					() -> new JcefApplicationProperties(null, "invalid/path"),
					"Expected exception for missing index.html was not thrown."
			);
			assertTrue(ex.getMessage().contains("Unable to locate index.html under classpath invalid/path/"));
		}
	}

	//endregion

	//region Installation Path Tests

	@Test
	@DisplayName("📦 getJcefDataPath should resolve to 'cef_data' subdirectory")
	void getJcefDataPath_returnsCorrectPath() {
		var environment = mock(Environment.class);
		when(environment.getProperty("jcef.application-name")).thenReturn(TEST_APP_NAME);
		when(environment.getProperty("jcef.installation-path")).thenReturn(Path.of("win", TEST_APP_NAME).toString());
		try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
			var properties = new JcefApplicationProperties(null, "ui");
			properties.setEnvironment(environment);
			Path expectedPath = Path.of("win", TEST_APP_NAME, "cef_data");

			// Act
			var actualPath = properties.getJcefDataPath();

			// Assert
			assertEquals(expectedPath, actualPath);
		}
	}

	@Test
	@DisplayName("🎨 getUiInstallationPath should resolve to 'ui' subdirectory")
	void getUiInstallationPath_returnsCorrectPath() {
		var environment = mock(Environment.class);
		when(environment.getProperty("jcef.application-name")).thenReturn(TEST_APP_NAME);
		when(environment.getProperty("jcef.installation-path")).thenReturn(Path.of("win", TEST_APP_NAME).toString());
		try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
			var properties = new JcefApplicationProperties(null, "ui");
			properties.setEnvironment(environment);
			Path expectedPath = Path.of("win", TEST_APP_NAME, "ui");

			// Act
			var actualPath = properties.getUiInstallationPath();

			// Assert
			assertEquals(expectedPath, actualPath);
		}
	}
}
