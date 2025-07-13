package io.github.bitfist.jcef.spring.browser.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.browser.AbstractSplashScreen;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Optional;

/**
 * 🖥 Auto-configuration for browser components.
 * Imports BrowserConfiguration to set up browser startup and UI installation.
 */
@Configuration
@Import({BrowserStarter.class, CefConfiguration.class, UIInstaller.class})
class BrowserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AbstractSplashScreen progressFrameProvider(JcefApplicationProperties applicationProperties, Optional<BuildProperties> buildProperties) {
        return new DefaultSplashScreen(applicationProperties, buildProperties.orElse(null));
    }

    @Bean
    ObjectMapper cefBrowserObjectMapper() {
        return new ObjectMapper();
    }
}
