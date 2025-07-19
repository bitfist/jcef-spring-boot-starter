package io.github.bitfist.jcef.spring.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JcefApplicationProperties}.
 * Mocks the static {@link JcefApplicationProperties.OsIdentifier} to test OS-specific logic.
 */
@ExtendWith(MockitoExtension.class)
class JcefApplicationPropertiesTest {

    private static final String TEST_APP_NAME = "MyTestApp";
    private MockedStatic<JcefApplicationProperties.OsIdentifier> osIdentifierMock;

    @BeforeEach
    void setUp() {
        // Before each test, we create a static mock for the OsIdentifier class.
        // This allows us to control the value returned by getOsName() for each test case.
        osIdentifierMock = mockStatic(JcefApplicationProperties.OsIdentifier.class);
    }



    @AfterEach
    void tearDown() {
        // It's crucial to close the static mock after each test to avoid
        // mock leakage between tests and to restore the original static method behavior.
        osIdentifierMock.close();
    }

    //region Constructor Tests

    @Test
    @DisplayName("🚫 Constructor should throw IllegalArgumentException for blank application name")
    void constructor_whenApplicationNameIsBlank_thenThrowsException() {
        // We mock ClassPathResource to avoid file system checks during this specific test.
        try (var ignored = mockConstruction(ClassPathResource.class,
                (mock, context) -> when(mock.exists()).thenReturn(true))) {

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> new JcefApplicationProperties("", null, "ui"),
                    "Expected exception for blank application name was not thrown."
            );
            assertEquals("jcef.application-name name must not be blank", ex.getMessage());
        }
    }

    @Test
    @DisplayName("🗂️ Constructor should default distribution classpath to 'ui/' if null")
    void constructor_whenDistributionPathIsNull_thenDefaultsToUI() {
        // Mock ClassPathResource to simulate that 'ui/index.html' exists.
        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> {
            if ("ui/index.html".equals(context.arguments().getFirst())) {
                when(mock.exists()).thenReturn(true);
            }
        })) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, null);
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
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "  ");
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
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "custom/path");
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
                    () -> new JcefApplicationProperties(TEST_APP_NAME, null, "invalid/path"),
                    "Expected exception for missing index.html was not thrown."
            );
            assertTrue(ex.getMessage().contains("Unable to locate index.html under classpath invalid/path/"));
        }
    }

    //endregion

    //region Installation Path Tests

    @Test
    @DisplayName("🪟 getInstallationPath should return correct APPDATA path on Windows")
    void getInstallationPath_whenOsIsWindows_thenUsesAppDataPath() {
        // This test relies on the APPDATA environment variable being set.
        // We use an assumption to skip the test if it's not available in the test environment.
        String appData = System.getenv("APPDATA");
        assumeTrue(appData != null && !appData.isBlank(), "APPDATA environment variable not set; skipping Windows path test.");

        // Arrange: Simulate a Windows OS.
        osIdentifierMock.when(JcefApplicationProperties.OsIdentifier::getOsName).thenReturn("windows 11");
        Path expectedPath = Path.of(appData, TEST_APP_NAME);

        // Act & Assert
        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "ui");
            var actualPath = properties.getInstallationPath();
            assertEquals(expectedPath, actualPath, "The installation path for Windows is incorrect.");
        }
    }

    @Test
    @DisplayName("🍎 getInstallationPath should return correct Application Support path on macOS")
    void getInstallationPath_whenOsIsMac_thenUsesLibraryApplicationSupportPath() {
        // Arrange: Simulate a macOS.
        osIdentifierMock.when(JcefApplicationProperties.OsIdentifier::getOsName).thenReturn("mac os x");
        String userHome = System.getProperty("user.home");
        Path expectedPath = Path.of(userHome, "Library", "Application Support", TEST_APP_NAME);

        // Act & Assert
        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "ui");
            var actualPath = properties.getInstallationPath();
            assertEquals(expectedPath, actualPath, "The installation path for macOS is incorrect.");
        }
    }

    @Test
    @DisplayName("🐧 getInstallationPath should return correct fallback path on Linux when XDG_DATA_HOME is not set")
    void getInstallationPath_whenOsIsLinuxWithoutXdg_thenUsesLocalSharePath() {
        // Arrange: Simulate a Linux OS.
        // We are specifically testing the fallback path, which is used when XDG_DATA_HOME is null.
        // Mocking System.getenv("XDG_DATA_HOME") to return null is tricky without special libraries,
        // so this test relies on it not being set in the test environment, which is a safe assumption.
        osIdentifierMock.when(JcefApplicationProperties.OsIdentifier::getOsName).thenReturn("linux");
        String userHome = System.getProperty("user.home");
        Path expectedPath = Path.of(userHome, ".local", "share", TEST_APP_NAME);

        // Act & Assert
        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "ui");
            var actualPath = properties.getInstallationPath();
            assertEquals(expectedPath, actualPath, "The fallback installation path for Linux is incorrect.");
        }
    }

    // Note: Testing the `if (xdg != null)` branch for Linux is non-trivial without
    // either changing the test execution environment or refactoring the production code
    // to allow injecting environment variables. The test above covers the more common fallback case.

    @Test
    @DisplayName("💾 getInstallationPath should be memoized and calculated only once")
    void getInstallationPath_isMemoized() {
        // Arrange: Simulate any OS
        osIdentifierMock.when(JcefApplicationProperties.OsIdentifier::getOsName).thenReturn("win");

        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "ui");

            // Act: Call the getter multiple times
            var path1 = properties.getInstallationPath();
            var path2 = properties.getInstallationPath();
            var path3 = properties.getInstallationPath();

            // Assert: Check that the result is consistent
            assertEquals(path1, path2);
            assertEquals(path2, path3);

            // Assert: Verify that the underlying OS check method was called exactly once.
            osIdentifierMock.verify(JcefApplicationProperties.OsIdentifier::getOsName, times(1));
        }
    }


    //endregion

    //region Other Path Getters

    @Test
    @DisplayName("📦 getJcefInstallationPath should resolve to 'bundle' subdirectory")
    void getJcefInstallationPath_returnsCorrectPath() {
        // Arrange: Simulate any OS
        osIdentifierMock.when(JcefApplicationProperties.OsIdentifier::getOsName).thenReturn("windows");
        String appData = System.getenv("APPDATA");
        assumeTrue(appData != null, "APPDATA not set, skipping");

        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "ui");
            Path expectedPath = Path.of(appData, TEST_APP_NAME, "bundle");

            // Act
            var actualPath = properties.getJcefInstallationPath();

            // Assert
            assertEquals(expectedPath, actualPath);
        }
    }

    @Test
    @DisplayName("🎨 getUiInstallationPath should resolve to 'ui' subdirectory")
    void getUiInstallationPath_returnsCorrectPath() {
        // Arrange: Simulate any OS
        osIdentifierMock.when(JcefApplicationProperties.OsIdentifier::getOsName).thenReturn("windows");
        String appData = System.getenv("APPDATA");
        assumeTrue(appData != null, "APPDATA not set, skipping");

        try (var ignored = mockConstruction(ClassPathResource.class, (mock, context) -> when(mock.exists()).thenReturn(true))) {
            var properties = new JcefApplicationProperties(TEST_APP_NAME, null, "ui");
            Path expectedPath = Path.of(appData, TEST_APP_NAME, "ui");

            // Act
            var actualPath = properties.getUiInstallationPath();

            // Assert
            assertEquals(expectedPath, actualPath);
        }
    }

    //endregion
}
