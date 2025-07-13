package io.github.bitfist.jcef.spring.query.internal;

import io.github.bitfist.jcef.spring.query.DefaultCefQueryRouter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class QueryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    DefaultCefQueryRouter defaultCefQueryRouter(ApplicationContext applicationContext) {
        return new DefaultCefQueryRouter(applicationContext);
    }
}
