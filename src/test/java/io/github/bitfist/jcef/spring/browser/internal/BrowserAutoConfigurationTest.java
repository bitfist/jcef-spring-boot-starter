package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserFrameCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import io.github.bitfist.jcef.spring.browser.DevelopmentConfigurationProperties;
import io.github.bitfist.jcef.spring.swing.SwingComponentFactory;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.IProgressHandler;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("🖥️ BrowserAutoConfiguration Tests")
class BrowserAutoConfigurationTest {

	@Mock
	private JcefApplicationProperties applicationProperties;

	@Mock
	private DevelopmentConfigurationProperties developmentProperties;

	@Mock
	private SwingComponentFactory swingComponentFactory;

	@InjectMocks
	private BrowserAutoConfiguration browserAutoConfiguration;

	@Nested
	@DisplayName("💧 Bean Creation Tests")
	class BeanCreationTests {

		@Test
		@DisplayName("✅ should create UIInstaller")
		void uiInstaller() {
			// When
			var uiInstaller = browserAutoConfiguration.uiInstaller(applicationProperties);

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
			var browserStarter = browserAutoConfiguration.browserStarter(mockCefApp, mockCefBrowser, customizers);

			// Then
			assertThat(browserStarter).isNotNull();
		}

		@Test
		@DisplayName("✅ should create DefaultBrowser")
		void browser() {
			// Given
			CefBrowser mockCefBrowser = mock(CefBrowser.class);

			// When
			var browser = browserAutoConfiguration.browser(mockCefBrowser);

			// Then
			assertThat(browser)
					.isNotNull()
					.isInstanceOf(DefaultBrowser.class);
		}

		@Test
		@DisplayName("✅ should create ObjectMapper")
		void cefBrowserObjectMapper() {
			// When
			var objectMapper = browserAutoConfiguration.cefBrowserObjectMapper();

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
		private IProgressHandler mockSplashScreen;
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
			when(applicationProperties.getJcefDataPath()).thenReturn(Paths.get("test/data"));

			CefApplicationCustomizer mockCustomizer = mock(CefApplicationCustomizer.class);
			var customizers = Collections.singletonList(mockCustomizer);
			CefApp mockCefApp = mock(CefApp.class);

			// Mock the construction of CefAppBuilder
			try (var mockedBuilder = mockConstruction(CefAppBuilder.class,
					(mock, context) -> {
						when(mock.build()).thenReturn(mockCefApp);
						when(mock.getCefSettings()).thenReturn(new CefSettings());
					})) {

				// When
				var createdCefApp = browserAutoConfiguration.cefApp(mockApplicationContext, mockSplashScreen, customizers);

				// Then
				assertThat(createdCefApp).isEqualTo(mockCefApp);

				// Verify interactions with the mocked builder
				var builder = mockedBuilder.constructed().getFirst();
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
			try (var mockedBuilder = mockConstruction(CefAppBuilder.class,
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
			try (var staticCefMessageRouter = mockStatic(CefMessageRouter.class)) {
				var cefMessageRouter = mock(CefMessageRouter.class);
				staticCefMessageRouter.when(CefMessageRouter::create).thenReturn(cefMessageRouter);
				CefApp mockCefApp = mock(CefApp.class);
				CefClient mockCefClient = mock(CefClient.class);
				CefQueryHandler cefQueryHandler = mock(CefQueryHandler.class);
				when(mockCefApp.createClient()).thenReturn(mockCefClient);

				CefClientCustomizer mockCustomizer = mock(CefClientCustomizer.class);
				var customizers = Collections.singletonList(mockCustomizer);

				// When
				var createdClient = browserAutoConfiguration.cefClient(mockCefApp, cefQueryHandler, customizers);

				// Then
				assertThat(createdClient).isEqualTo(mockCefClient);
				verify(mockCefApp).createClient();
				verify(mockCustomizer).accept(mockCefClient);
				verify(cefMessageRouter).addHandler(any(DefaultCefMessageRouter.class), eq(true));
			}
		}

		@Test
		@DisplayName("📄 should create CefBrowser with correct file URI and apply customizers")
		void cefBrowser_createsAndCustomizesWithFileUri() {
			// Given
			CefClient mockCefClient = mock(CefClient.class);
			CefBrowser mockCefBrowser = mock(CefBrowser.class);
			Path mockUiPath = Paths.get("test/ui");
			var expectedFile = mockUiPath.resolve("index.html").toAbsolutePath();

			when(applicationProperties.getUiInstallationPath()).thenReturn(mockUiPath);
			when(mockCefClient.createBrowser(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockCefBrowser);

			CefBrowserCustomizer mockCustomizer = mock(CefBrowserCustomizer.class);
			var customizers = Collections.singletonList(mockCustomizer);

			// When
			var createdBrowser = browserAutoConfiguration.cefBrowser(mockCefClient, customizers);

			// Then
			assertThat(createdBrowser).isEqualTo(mockCefBrowser);
			verify(mockCefClient).createBrowser(eq(expectedFile.toUri().toString()), eq(false), eq(false));
			verify(mockCustomizer).accept(mockCefBrowser);
		}

		@ParameterizedTest
		@ValueSource(strings = {"", "https://invalid:8000"})
		@DisplayName("📄 should create CefBrowser with correct web URI and apply customizers")
		void cefBrowser_createsAndCustomizesWithWebUri(String frontendUri) {
			// Given
			CefClient mockCefClient = mock(CefClient.class);
			CefBrowser mockCefBrowser = mock(CefBrowser.class);
			CefBrowserCustomizer mockCustomizer = mock(CefBrowserCustomizer.class);

			when(mockCefClient.createBrowser(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockCefBrowser);
			when(developmentProperties.getFrontendUri()).thenReturn(frontendUri);
			when(developmentProperties.isEnableWebCommunication()).thenReturn(true);

			// When
			var createdBrowser = browserAutoConfiguration.cefBrowser(mockCefClient, Collections.singletonList(mockCustomizer));

			// Then
			assertThat(createdBrowser).isEqualTo(mockCefBrowser);
			verify(mockCefClient).createBrowser(eq(frontendUri), eq(false), eq(false));
			verify(mockCustomizer).accept(mockCefBrowser);
		}
	}

}