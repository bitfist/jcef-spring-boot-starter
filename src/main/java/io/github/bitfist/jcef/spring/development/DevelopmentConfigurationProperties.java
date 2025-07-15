package io.github.bitfist.jcef.spring.development;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jcef.development")
@RequiredArgsConstructor
public class DevelopmentConfigurationProperties {
    private final @Nullable Integer debugPort;
    private final boolean showDeveloperTools;
}
