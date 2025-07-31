package io.github.bitfist.jcef.spring.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DevelopmentConfigurationPropertiesTest {

	@Test
	void returnsDefaultUriOnNull() {
		var target = new DevelopmentConfigurationProperties(1234, true, true, null);

		assertNotNull(target.getFrontendUri());
		assertEquals(DevelopmentConfigurationProperties.DEFAULT_FRONTEND_URL, target.getFrontendUri());
	}

}