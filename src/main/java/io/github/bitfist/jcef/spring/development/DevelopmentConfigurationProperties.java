package io.github.bitfist.jcef.spring.development;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jcef.development")
@AllArgsConstructor
public class DevelopmentConfigurationProperties {
    private @Nullable Integer debugPort;
    private boolean showDeveloperTools;
}
