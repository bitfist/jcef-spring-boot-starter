package io.github.bitfist.jcef.spring.browser;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Data
@ConfigurationProperties(prefix = "jcef.development")
@AllArgsConstructor
public class DevelopmentConfigurationProperties {

	public static final String DEFAULT_FRONTEND_URL = "http://localhost:3000";

	private @Nullable Integer debugPort;
	private boolean showDeveloperTools;
	private boolean enableWebCommunication;
	private @Nullable String frontendUri;

	public String getFrontendUri() {
		if (isBlank(frontendUri)) {
			return DEFAULT_FRONTEND_URL;
		}
		return frontendUri;
	}
}
