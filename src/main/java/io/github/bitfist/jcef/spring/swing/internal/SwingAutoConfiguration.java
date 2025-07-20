package io.github.bitfist.jcef.spring.swing.internal;

import io.github.bitfist.jcef.spring.swing.SwingComponentFactory;
import io.github.bitfist.jcef.spring.swing.SwingExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class SwingAutoConfiguration {

    @Bean
    SwingComponentFactory swingComponentFactory() {
        return new SwingComponentFactory();
    }

    @Bean
    SwingExecutor swingExecutor() {
        return new SwingExecutor();
    }
}
