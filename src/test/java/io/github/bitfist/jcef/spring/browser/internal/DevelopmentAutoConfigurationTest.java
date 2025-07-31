package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import io.github.bitfist.jcef.spring.browser.DevelopmentConfigurationProperties;
import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevelopmentAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DevelopmentAutoConfiguration.class));

	@Test
	@DisplayName("🐞 developerToolsCustomizer loads when show-developer-tools=true")
	void developerToolsCustomizerLoaded() {
		contextRunner
				.withPropertyValues("jcef.development.show-developer-tools=true")
				.run(context -> assertThat(context).hasSingleBean(CefClientCustomizer.class));
	}

	@Test
	@DisplayName("🚫 developerToolsCustomizer absent when show-developer-tools=false")
	void developerToolsCustomizerNotLoaded() {
		contextRunner
				// no property set → bean should not be created
				.run(context -> assertThat(context).doesNotHaveBean(CefClientCustomizer.class));
	}

	@Test
	@DisplayName("🐞 corsConfigurer loads when enable-web-communication=true")
	void corsConfigurerLoaded() {
		contextRunner
				.withBean(CefQueryHandler.class, () -> mock(CefQueryHandler.class))
				.withPropertyValues("jcef.development.enable-web-communication=true")
				.run(context -> assertThat(context).hasBean("corsConfigurer"));
	}

	@Test
	@DisplayName("🐞 corsConfigurer throws no exception")
	void corsConfigurerDoesNotThrowException() {
		var developmentProperties = new DevelopmentConfigurationProperties(123, true, true, "http://invalid.host:80");
		var corsConfigurer = new DevelopmentAutoConfiguration.CorsConfiguration().corsConfigurer(developmentProperties);
		var corsRegistry = mock(CorsRegistry.class);
		var corsRegistration = mock(CorsRegistration.class);

		when(corsRegistry.addMapping("/**")).thenReturn(corsRegistration);
		when(corsRegistration.allowedOrigins(any())).thenReturn(corsRegistration);
		when(corsRegistration.allowedMethods(any())).thenReturn(corsRegistration);
		when(corsRegistration.allowedHeaders(any())).thenReturn(corsRegistration);

		// when
		corsConfigurer.addCorsMappings(corsRegistry);

		// then
		verify(corsRegistry).addMapping("/**");
		verify(corsRegistration).allowedOrigins("http://invalid.host:80");
		verify(corsRegistration).allowedMethods("POST");
		verify(corsRegistration).allowedHeaders("*");
	}


	@Test
	@DisplayName("🚫 corsConfigurer absent when enable-web-communication=false")
	void corsConfigurerNotLoaded() {
		contextRunner
				// no property set → bean should not be created
				.run(context -> assertThat(context).doesNotHaveBean("corsConfigurer"));
	}

	@Test
	@DisplayName("🔍 developerToolsCustomizer registers a load handler")
	void developerToolsCustomizerRegistersHandler() {
		var cfg = new DevelopmentAutoConfiguration();
		var customizer = cfg.developerToolsCustomizer();
		var client = mock(org.cef.CefClient.class);
		var cefBrowser = mock(CefBrowser.class);

		customizer.accept(client);

		// verify that addLoadHandler was called with a CefLoadHandlerAdapter
		var captor = org.mockito.ArgumentCaptor.forClass(org.cef.handler.CefLoadHandlerAdapter.class);
		verify(client).addLoadHandler(captor.capture());

		var cefLoadHandlerAdapter = captor.getValue();
		cefLoadHandlerAdapter.onLoadEnd(cefBrowser, null, 0);
	}

	@Test
	@DisplayName("🛠️ debugPortCustomizer sets port and adds args")
	void debugPortCustomizerSetsPortAndArgs() {
		// simulate having debug-port property
		var props = new DevelopmentConfigurationProperties(5555, false, false, null);
		var cfg = new DevelopmentAutoConfiguration();
		var customizer = cfg.debugPortCustomizer(props);

		var builder = mock(CefAppBuilder.class);
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
				.run(context -> assertThat(context).doesNotHaveBean(CefApplicationCustomizer.class));
	}
}
