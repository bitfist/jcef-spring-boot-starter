package io.github.bitfist.jcef.spring.browser.javascript.internal;

import io.github.bitfist.jcef.spring.browser.javascript.DefaultJavaScriptExecutor;
import io.github.bitfist.jcef.spring.browser.javascript.JavaScriptExecutor;
import org.cef.browser.CefBrowser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class JavaScriptExecutionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JavaScriptExecutor javaScriptExecutor(CefBrowser cefBrowser) {
        return new DefaultJavaScriptExecutor(cefBrowser);
    }
}
