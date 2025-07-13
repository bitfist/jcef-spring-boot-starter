package io.github.bitfist.jcef.spring.browser.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.browser.AbstractSplashScreen;
import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefBrowserCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
class CefConfiguration {

    private final JcefApplicationProperties applicationProperties;

    @Bean
    CefApp cefApp(
            ConfigurableApplicationContext applicationContext,
            AbstractSplashScreen splashScreen,
            List<CefApplicationCustomizer> cefApplicationCustomizers
    ) {
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(applicationProperties.getJcefInstallationPath().toFile());
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.setProgressHandler(splashScreen);
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
    CefClient cefClient(
            ApplicationEventPublisher eventPublisher,
            CefApp cefApp,
            @Qualifier("cefBrowserObjectMapper") ObjectMapper objectMapper,
            List<CefClientCustomizer> cefClientCustomizers
    ) {
        CefClient client = cefApp.createClient();
        CefMessageRouter messageRouter = CefMessageRouter.create();
        messageRouter.addHandler(new CefQueryPublisher(eventPublisher, objectMapper), true);
        client.addMessageRouter(messageRouter);
        cefClientCustomizers.forEach(consumer -> consumer.accept(client));
        return client;
    }

    @Bean
    CefBrowser cefBrowser(CefClient client, List<CefBrowserCustomizer> cefBrowserCustomizers) {
        File file = applicationProperties.getUiInstallationPath().resolve("index.html").toFile();
        CefBrowser browser = client.createBrowser(file.toURI().toString(), false, false);
        cefBrowserCustomizers.forEach(consumer -> consumer.accept(browser));
        return browser;
    }
}
