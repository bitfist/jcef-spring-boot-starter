package io.github.bitfist.jcef.spring.tsobject.internal;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class TypeScriptServiceAutoConfigurationTest {

	private final ApplicationContext applicationContext = mock(ApplicationContext.class);

	private final TypeScriptObjectAutoConfiguration autoConfiguration = new TypeScriptObjectAutoConfiguration();

	@Test
	void shouldCreateCefMessageHandler() {
		assertNotNull(autoConfiguration.methodInvokingCefMessageHandler(applicationContext));
	}
}