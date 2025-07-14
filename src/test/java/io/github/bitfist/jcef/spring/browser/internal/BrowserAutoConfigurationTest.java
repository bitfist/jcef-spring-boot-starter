package io.github.bitfist.jcef.spring.browser.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.browser.AbstractSplashScreen;
import io.github.bitfist.jcef.spring.browser.Browser;
import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserFrameCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("🖥️ BrowserAutoConfiguration Tests")
class BrowserAutoConfigurationTest {

    @Mock
    private JcefApplicationProperties applicationProperties;

    @InjectMocks
    private BrowserAutoConfiguration browserAutoConfiguration;

    @Nested
    @DisplayName("💧 Bean Creation Tests")
    class BeanCreationTests {

        @Test
        @DisplayName("✅ should create DefaultSplashScreen with BuildProperties")
        @DisabledIfEnvironmentVariable(named = "CI", matches = ".*", disabledReason = "Needs Swing, cannot run in CI")
        void progressFrameProvider_withBuildProperties() {
            // Given
            BuildProperties mockBuildProperties = mock(BuildProperties.class);

            // When
            AbstractSplashScreen splashScreen = browserAutoConfiguration.progressFrameProvider(applicationProperties, Optional.of(mockBuildProperties));

            // Then
            assertThat(splashScreen)
                    .isNotNull()
                    .isInstanceOf(DefaultSplashScreen.class);
        }

        @Test
        @DisplayName("✅ should create DefaultSplashScreen without BuildProperties")
        @DisabledIfEnvironmentVariable(named = "CI", matches = ".*", disabledReason = "Needs Swing, cannot run in CI")
        void progressFrameProvider_withoutBuildProperties() {
            // When
            AbstractSplashScreen splashScreen = browserAutoConfiguration.progressFrameProvider(applicationProperties, Optional.empty());

            // Then
            assertThat(splashScreen)
                    .isNotNull()
                    .isInstanceOf(DefaultSplashScreen.class);
        }

        @Test
        @DisplayName("✅ should create UIInstaller")
        void uiInstaller() {
            // When
            UIInstaller uiInstaller = browserAutoConfiguration.uiInstaller(applicationProperties);

            // Then
            assertThat(uiInstaller).isNotNull();
        }

        @Test
        @DisplayName("✅ should create BrowserStarter")
        void browserStarter() {
            // Given
            CefApp mockCefApp = mock(CefApp.class);
            CefBrowser mockCefBrowser = mock(CefBrowser.class);
            List<CefBrowserFrameCustomizer> customizers = Collections.emptyList();

            // When
            BrowserStarter browserStarter = browserAutoConfiguration.browserStarter(mockCefApp, mockCefBrowser, customizers);

            // Then
            assertThat(browserStarter).isNotNull();
        }

        @Test
        @DisplayName("✅ should create DefaultBrowser")
        void browser() {
            // Given
            CefBrowser mockCefBrowser = mock(CefBrowser.class);

            // When
            Browser browser = browserAutoConfiguration.browser(mockCefBrowser);

            // Then
            assertThat(browser)
                    .isNotNull()
                    .isInstanceOf(DefaultBrowser.class);
        }

        @Test
        @DisplayName("✅ should create ObjectMapper")
        void cefBrowserObjectMapper() {
            // When
            ObjectMapper objectMapper = browserAutoConfiguration.cefBrowserObjectMapper();

            // Then
            assertThat(objectMapper).isNotNull();
        }
    }

    @Nested
    @DisplayName("🛠️ CEF Configuration Tests")
    class CefConfigurationTests {

        @Mock
        private ConfigurableApplicationContext mockApplicationContext;
        @Mock
        private AbstractSplashScreen mockSplashScreen;
        @Captor
        private ArgumentCaptor<CefApp.CefAppState> stateCaptor;
        @Captor
        private ArgumentCaptor<CefClientCustomizer> clientCustomizerCaptor;

        @Test
        @DisplayName("🔧 should build CefApp with customizers and correct settings")
        void cefApp_buildsCorrectly() throws Exception {
            // Given
            Path mockPath = Paths.get("test/path");
            when(applicationProperties.getJcefInstallationPath()).thenReturn(mockPath);

            CefApplicationCustomizer mockCustomizer = mock(CefApplicationCustomizer.class);
            List<CefApplicationCustomizer> customizers = Collections.singletonList(mockCustomizer);
            CefApp mockCefApp = mock(CefApp.class);

            // Mock the construction of CefAppBuilder
            try (MockedConstruction<CefAppBuilder> mockedBuilder = mockConstruction(CefAppBuilder.class,
                    (mock, context) -> {
                        when(mock.build()).thenReturn(mockCefApp);
                        when(mock.getCefSettings()).thenReturn(new CefSettings());
                    })) {

                // When
                CefApp createdCefApp = browserAutoConfiguration.cefApp(mockApplicationContext, mockSplashScreen, customizers);

                // Then
                assertThat(createdCefApp).isEqualTo(mockCefApp);

                // Verify interactions with the mocked builder
                CefAppBuilder builder = mockedBuilder.constructed().get(0);
                verify(builder).setInstallDir(mockPath.toFile());
                verify(builder).setProgressHandler(mockSplashScreen);
                verify(builder).setAppHandler(any(MavenCefAppHandlerAdapter.class));
                verify(mockCustomizer).accept(builder);
                verify(builder).build();
            }
        }

        @Test
        @DisplayName("💥 should handle exception during CefApp build")
        void cefApp_throwsRuntimeExceptionOnBuildFailure() throws Exception {
            // Given
            Path mockPath = Paths.get("test/path");
            when(applicationProperties.getJcefInstallationPath()).thenReturn(mockPath);

            // Mock the construction of CefAppBuilder to throw an exception
            try (MockedConstruction<CefAppBuilder> mockedBuilder = mockConstruction(CefAppBuilder.class,
                    (mock, context) -> {
                        when(mock.build()).thenThrow(new InterruptedException("Build failed"));
                        when(mock.getCefSettings()).thenReturn(new CefSettings());
                    })) {

                // When & Then
                assertThrows(RuntimeException.class, () -> {
                    browserAutoConfiguration.cefApp(mockApplicationContext, mockSplashScreen, Collections.emptyList());
                });
            }
        }


        @Test
        @DisplayName("🖥️ should create CefClient and apply customizers")
        void cefClient_createsAndCustomizes() {
            // Given
            CefApp mockCefApp = mock(CefApp.class);
            CefClient mockCefClient = mock(CefClient.class);
            when(mockCefApp.createClient()).thenReturn(mockCefClient);

            CefClientCustomizer mockCustomizer = mock(CefClientCustomizer.class);
            List<CefClientCustomizer> customizers = Collections.singletonList(mockCustomizer);

            // When
            CefClient createdClient = browserAutoConfiguration.cefClient(mockCefApp, customizers);

            // Then
            assertThat(createdClient).isEqualTo(mockCefClient);
            verify(mockCefApp).createClient();
            verify(mockCustomizer).accept(mockCefClient);
        }

        @Test
        @DisplayName("📄 should create CefBrowser with correct URL and apply customizers")
        void cefBrowser_createsAndCustomizes() {
            // Given
            CefClient mockCefClient = mock(CefClient.class);
            CefBrowser mockCefBrowser = mock(CefBrowser.class);
            Path mockUiPath = Paths.get("test/ui");
            File expectedFile = mockUiPath.resolve("index.html").toFile();

            when(applicationProperties.getUiInstallationPath()).thenReturn(mockUiPath);
            when(mockCefClient.createBrowser(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockCefBrowser);

            CefBrowserCustomizer mockCustomizer = mock(CefBrowserCustomizer.class);
            List<CefBrowserCustomizer> customizers = Collections.singletonList(mockCustomizer);

            // When
            CefBrowser createdBrowser = browserAutoConfiguration.cefBrowser(mockCefClient, customizers);

            // Then
            assertThat(createdBrowser).isEqualTo(mockCefBrowser);
            verify(mockCefClient).createBrowser(eq(expectedFile.toURI().toString()), eq(false), eq(false));
            verify(mockCustomizer).accept(mockCefBrowser);
        }
    }

}