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

    private String appDataPath;
    private final String applicationName;
    @Nullable
    private final String splashScreenClasspathResource;
    private String distributionClasspath;

    public JcefApplicationProperties(String applicationName, @Nullable String splashScreenClasspathResource, @Nullable String distributionClasspath) {
        if (isBlank(applicationName)) {
            throw new IllegalArgumentException("jcef.application-name name must not be blank");
        }
        this.applicationName = applicationName;
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
        appDataPath = environment.getProperty("os.app-data-path");
        if (isBlank(appDataPath)) {
            throw new IllegalArgumentException("os.app-data-path name must not be blank");
        }
    }

    // region Paths

    private Path jcefInstallationPath; // Subdirectory for JCEF binaries
    private Path uiInstallationPath; // Subdirectory for UI files
    private Path jcefDataPath; // Subdirectory for JCEF data

    public Path getApplicationInstallationPath() {
        return Path.of(appDataPath, applicationName);
    }

    /**
     * 📦 Path to JCEF installation directory.
     */
    public Path getJcefInstallationPath() {
        if (jcefInstallationPath == null) {
            jcefInstallationPath = getApplicationInstallationPath().resolve("bundle");
        }
        return jcefInstallationPath;
    }

    /**
     * 📦 Path to JCEF data directory.
     */
    public Path getJcefDataPath() {
        if (jcefDataPath == null) {
            jcefDataPath = getApplicationInstallationPath().resolve("cef_data");
        }
        return jcefDataPath;
    }

    /**
     * 📦 Path to UI resources directory.
     */
    public Path getUiInstallationPath() {
        if (uiInstallationPath == null) {
            uiInstallationPath = getApplicationInstallationPath().resolve("ui");
        }
        return uiInstallationPath;
    }

    // endregion
}
