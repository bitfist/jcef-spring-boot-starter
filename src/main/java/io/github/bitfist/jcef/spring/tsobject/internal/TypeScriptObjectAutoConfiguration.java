package io.github.bitfist.jcef.spring.tsobject.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class TypeScriptObjectAutoConfiguration {

	@Bean
	MethodInvokingCefQueryHandler methodInvokingCefMessageHandler(ApplicationContext applicationContext) {
		var messageHandler = new MethodInvokingCefMessageHandler(applicationContext);
		return new MethodInvokingCefQueryHandler(messageHandler);
	}
}
