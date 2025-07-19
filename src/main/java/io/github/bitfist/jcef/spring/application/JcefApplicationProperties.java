package io.github.bitfist.jcef.spring.application;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
public class JcefApplicationProperties {

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

        // ✅ Validate that index.html exists in the distributionClasspath on the classpath.
        if (!this.distributionClasspath.endsWith("/")) {
            this.distributionClasspath += "/";
        }
        var path = this.distributionClasspath + "index.html";
        var indexHtml = new ClassPathResource(path);
        if (!indexHtml.exists()) {
            throw new IllegalArgumentException("Unable to locate index.html under classpath " + this.distributionClasspath);
        }
    }

    // region Paths

    private Path installationPath; // Base directory for app data
    private Path jcefInstallationPath; // Subdirectory for JCEF binaries
    private Path uiInstallationPath; // Subdirectory for UI files

    /**
     * 🗂 Determine the platform-specific installation path.
     */
    public Path getInstallationPath() {
        if (installationPath == null) {
            Path baseDir;
            try {
                if (OsIdentifier.getOsName().contains("win")) {
                    baseDir = Path.of(System.getenv("APPDATA"));
                } else if (OsIdentifier.getOsName().contains("mac")) {
                    baseDir = Path.of(System.getProperty("user.home"), "Library", "Application Support");
                } else {
                    String xdg = System.getenv("XDG_DATA_HOME");
                    if (xdg != null && !xdg.isBlank()) {
                        baseDir = Path.of(xdg);
                    } else {
                        baseDir = Path.of(System.getProperty("user.home"), ".local", "share");
                    }
                }
            } catch (NullPointerException e) {
                baseDir = Path.of("~");
            }
            installationPath = baseDir.resolve(applicationName);
        }
        return installationPath;
    }

    /**
     * 📦 Path to JCEF bundle directory.
     */
    public Path getJcefInstallationPath() {
        if (jcefInstallationPath == null) {
            jcefInstallationPath = getInstallationPath().resolve("bundle");
        }
        return jcefInstallationPath;
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

    static class OsIdentifier {
        static String getOsName() {
            return System.getProperty("os.name").toLowerCase();
        }
    }
}
