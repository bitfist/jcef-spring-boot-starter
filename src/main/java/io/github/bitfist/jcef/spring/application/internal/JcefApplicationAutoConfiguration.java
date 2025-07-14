package io.github.bitfist.jcef.spring.application.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>🔌 Auto-configuration for JCEF Spring Boot integration.</p>
 * Enables JcefApplicationProperties.
 */
@AutoConfiguration
@EnableConfigurationProperties(JcefApplicationProperties.class)
class JcefApplicationAutoConfiguration {
}
