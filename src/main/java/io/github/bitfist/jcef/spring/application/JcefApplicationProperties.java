package io.github.bitfist.jcef.spring.application;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * ⚙️ Configuration properties for JCEF integration.
 * <p>
 * Binds to properties prefixed with 'jcef'.
 */
@Getter
@ConfigurationProperties(prefix = "jcef")
public class JcefApplicationProperties implements EnvironmentAware {

	@Nullable
	private final String splashScreenClasspathResource;
	private String applicationName;
	private String distributionClasspath;

	public JcefApplicationProperties(@Nullable String splashScreenClasspathResource, @Nullable String distributionClasspath) {
		this.splashScreenClasspathResource = splashScreenClasspathResource;
		if (isBlank(distributionClasspath)) {
			this.distributionClasspath = "ui";
		} else {
			this.distributionClasspath = distributionClasspath;
		}

		if (!this.distributionClasspath.endsWith("/")) {
			this.distributionClasspath += "/";
		}
		// ✅ Validate that index.html exists in the distributionClasspath on the classpath.
		checkIndexHtmlExists();
	}

	private void checkIndexHtmlExists() {
		var path = this.distributionClasspath + "index.html";
		var indexHtml = new ClassPathResource(path);
		if (!indexHtml.exists()) {
			throw new IllegalArgumentException("Unable to locate index.html under classpath " + this.distributionClasspath);
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		applicationName = environment.getProperty("jcef.application-name");
		installationPath = Path.of(environment.getProperty("jcef.installation-path"));
	}

	// region Paths

	private Path installationPath; // Main installation directory
	private Path jcefInstallationPath; // Subdirectory for JCEF binaries
	private Path uiInstallationPath; // Subdirectory for UI files
	private Path jcefDataPath; // Subdirectory for JCEF data

	/**
	 * 📦 Path to JCEF installation directory.
	 */
	public Path getJcefInstallationPath() {
		if (jcefInstallationPath == null) {
			jcefInstallationPath = getInstallationPath().resolve("bundle");
		}
		return jcefInstallationPath;
	}

	/**
	 * 📦 Path to JCEF data directory.
	 */
	public Path getJcefDataPath() {
		if (jcefDataPath == null) {
			jcefDataPath = getInstallationPath().resolve("cef_data");
		}
		return jcefDataPath;
	}

	/**
	 * 📦 Path to UI resources directory.
	 */
	public Path getUiInstallationPath() {
		if (uiInstallationPath == null) {
			uiInstallationPath = getInstallationPath().resolve("ui");
		}
		return uiInstallationPath;
	}

	// endregion
}
