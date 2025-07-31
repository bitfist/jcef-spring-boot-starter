package io.github.bitfist.jcef.spring.jsexecution.internal;

import io.github.bitfist.jcef.spring.browser.Browser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class JavaScriptExecutionAutoConfigurationTest {

	@Test
	void createsBean() {
		Browser browser = mock(Browser.class);
		var autoConfiguration = new JavaScriptExecutionAutoConfiguration();

		assertNotNull(autoConfiguration.javaScriptExecutor(browser));
	}

}