package io.github.bitfist.jcef.spring.browser.javascript.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(JavaScriptExecutionConfiguration.class)
class JavaScriptExecutionAutoConfiguration {
}
