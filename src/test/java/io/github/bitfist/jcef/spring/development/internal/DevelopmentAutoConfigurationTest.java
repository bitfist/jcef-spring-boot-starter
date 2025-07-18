package io.github.bitfist.jcef.spring.development.internal;

import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import io.github.bitfist.jcef.spring.development.DevelopmentConfigurationProperties;
import me.friwi.jcefmaven.CefAppBuilder;
import org.assertj.core.api.Assertions;
import org.cef.CefSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevelopmentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevelopmentAutoConfiguration.class))
            .withBean(DevelopmentConfigurationProperties.class, () ->
                    new DevelopmentConfigurationProperties(null, false)
            );

    @Test
    @DisplayName("🐞 developerToolsCustomizer loads when show-developer-tools=true")
    void developerToolsCustomizerLoaded() {
        contextRunner
                .withPropertyValues("jcef.development.show-developer-tools=true")
                .run(context -> Assertions.assertThat(context).hasSingleBean(CefClientCustomizer.class));
    }

    @Test
    @DisplayName("🚫 developerToolsCustomizer absent when show-developer-tools=false")
    void developerToolsCustomizerNotLoaded() {
        contextRunner
                // no property set → bean should not be created
                .run(context -> Assertions.assertThat(context).doesNotHaveBean(CefClientCustomizer.class));
    }

    @Test
    @DisplayName("🔍 developerToolsCustomizer registers a load handler")
    void developerToolsCustomizerRegistersHandler() {
        var cfg = new DevelopmentAutoConfiguration();
        var customizer = cfg.developerToolsCustomizer();
        org.cef.CefClient client = mock(org.cef.CefClient.class);

        customizer.accept(client);

        // verify that addLoadHandler was called with a CefLoadHandlerAdapter
        var captor = org.mockito.ArgumentCaptor.forClass(org.cef.handler.CefLoadHandlerAdapter.class);
        verify(client).addLoadHandler(captor.capture());
    }

    @Test
    @DisplayName("🛠️ debugPortCustomizer sets port and adds args")
    void debugPortCustomizerSetsPortAndArgs() {
        // simulate having debug-port property
        var props = new DevelopmentConfigurationProperties(5555, false);
        var cfg = new DevelopmentAutoConfiguration();
        var customizer = cfg.debugPortCustomizer(props);

        CefAppBuilder builder = mock(CefAppBuilder.class);
        var settings = new CefSettings();
        when(builder.getCefSettings()).thenReturn(settings);

        customizer.accept(builder);

        // remote_debugging_port should be set
        assertEquals(5555, settings.remote_debugging_port);
        // and argument added
        verify(builder).addJcefArgs("--remote-allow-origins=*");
    }

    @Test
    @DisplayName("🚫 debugPortCustomizer absent when no debug-port")
    void debugPortCustomizerNotLoadedWithoutProperty() {
        contextRunner
                // only show-developer-tools, no debug-port
                .withPropertyValues("jcef.development.show-developer-tools=true")
                .run(context -> Assertions.assertThat(context).doesNotHaveBean(CefApplicationCustomizer.class));
    }
}
