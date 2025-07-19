package io.github.bitfist.jcef.spring.tsobject.internal;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptObjectProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(TypeScriptObjectProperties.class)
@Import(MethodInvokingRestEndpoint.class)
class TypeScriptObjectAutoConfiguration {

    @Bean
    MethodInvokingCefQueryHandler methodInvokingCefMessageHandler(ApplicationContext applicationContext) {
        var messageHandler = new MethodInvokingCefMessageHandler(applicationContext);
        return new MethodInvokingCefQueryHandler(messageHandler);
    }
}
