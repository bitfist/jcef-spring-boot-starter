package io.github.bitfist.jcef.spring.jsexecution.internal;

import io.github.bitfist.jcef.spring.browser.Browser;
import io.github.bitfist.jcef.spring.jsexecution.JavaScriptExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class JavaScriptExecutionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	JavaScriptExecutor javaScriptExecutor(Browser browser) {
		return new DefaultJavaScriptExecutor(browser);
	}
}
