package io.github.bitfist.jcef.spring.swing.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwingAutoConfigurationTest {

	@Test
	void assertSwingComponentFactory() {
		assertNotNull(new SwingAutoConfiguration().swingComponentFactory());
	}

	@Test
	void assertSwingExecutor() {
		assertNotNull(new SwingAutoConfiguration().swingExecutor());
	}
}