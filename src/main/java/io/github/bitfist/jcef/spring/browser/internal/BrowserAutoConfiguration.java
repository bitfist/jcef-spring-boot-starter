package io.github.bitfist.jcef.spring.browser.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.browser.AbstractInstallerSplashScreen;
import io.github.bitfist.jcef.spring.browser.Browser;
import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserFrameCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * 🖥 Auto-configuration for browser components.
 * Imports BrowserConfiguration to set up browser startup and UI installation.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
class BrowserAutoConfiguration {

    private final JcefApplicationProperties applicationProperties;

    @Bean
    @ConditionalOnMissingBean
    AbstractInstallerSplashScreen progressFrameProvider(JcefApplicationProperties applicationProperties, Optional<BuildProperties> buildProperties) {
        return new DefaultInstallerSplashScreen(applicationProperties, buildProperties.orElse(null));
    }

    @Bean
    UIInstaller uiInstaller(JcefApplicationProperties applicationProperties) {
        return new UIInstaller(applicationProperties);
    }

    @Bean
    BrowserStarter browserStarter(CefApp cefApp, CefBrowser cefBrowser, List<CefBrowserFrameCustomizer> cefBrowserFrameCustomizers) {
        return new BrowserStarter(cefApp, cefBrowser, cefBrowserFrameCustomizers);
    }

    @Bean
    Browser browser(CefBrowser cefBrowser) {
        return new DefaultBrowser(cefBrowser);
    }

    // region CEF

    @Bean
    ObjectMapper cefBrowserObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    CefApp cefApp(
            ConfigurableApplicationContext applicationContext,
            AbstractInstallerSplashScreen installerSplashScreen,
            List<CefApplicationCustomizer> cefApplicationCustomizers
    ) {
        var builder = new CefAppBuilder();
        builder.setInstallDir(applicationProperties.getJcefInstallationPath().toFile());
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().root_cache_path = applicationProperties.getJcefDataPath().toFile().getAbsolutePath();
        builder.setProgressHandler(installerSplashScreen);
        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                if (state == CefApp.CefAppState.TERMINATED) {
                    applicationContext.close();
                }
            }
        });
        cefApplicationCustomizers.forEach(consumer -> consumer.accept(builder));

        try {
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create CefApp", e);
            throw new RuntimeException(e);
        }
    }

    @Bean
    CefClient cefClient(CefApp cefApp, CefQueryHandler messageHandler, List<CefClientCustomizer> cefClientCustomizers) {
        var client = cefApp.createClient();

        CefMessageRouter messageRouter = CefMessageRouter.create();
        messageRouter.addHandler(new DefaultCefMessageRouter(messageHandler), true);

        client.addMessageRouter(messageRouter);
        cefClientCustomizers.forEach(consumer -> consumer.accept(client));

        return client;
    }

    @Bean
    CefBrowser cefBrowser(CefClient client, List<CefBrowserCustomizer> cefBrowserCustomizers) {
        var file = applicationProperties.getUiInstallationPath().resolve("index.html").toFile();
        var browser = client.createBrowser(file.toURI().toString(), false, false);
        cefBrowserCustomizers.forEach(consumer -> consumer.accept(browser));
        return browser;
    }

    // endregion
}
