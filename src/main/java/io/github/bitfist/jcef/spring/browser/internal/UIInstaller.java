package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 🔄 Syncs UI resources from the classpath into the installation directory.
 * Performs a full refresh if any resource is newer or missing.
 */
@Slf4j
@Component
class UIInstaller {

    private final JcefApplicationProperties properties;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    /**
     * 🛠 Constructs the installer and triggers resource synchronization.
     *
     * @param properties Application properties guiding paths.
     */
    public UIInstaller(JcefApplicationProperties properties) {
        this.properties = properties;
        installUIResources();
    }

    /**
     * ✅ Checks for updates and copies UI resources if needed.
     */
    public void installUIResources() {
        try {
            Path targetBase = properties.getUiInstallationPath();
            String pattern = "classpath*:" + properties.getDistributionClasspath() + "/**/*";
            Resource[] resources = resolver.getResources(pattern);

            if (needsFullRefresh(resources, targetBase)) {
                log.info("Detected updated UI resources; cleaning {}", targetBase);
                cleanDirectory(targetBase);
                copyAll(resources, targetBase);
                log.info("UI installation refreshed at {}", targetBase);
            } else {
                log.info("UI is up-to-date, no action taken");
            }
        } catch (IOException e) {
            log.error("Failed to sync UI resources", e);
        }
    }

    /**
     * Determines if any resource is newer than its counterpart on disk
     * or if the installation directory is missing/empty.
     *
     * @param resources array of classpath resources to check
     * @param targetBase installation root on disk
     */
    private boolean needsFullRefresh(Resource[] resources, Path targetBase) throws IOException {
        if (!Files.exists(targetBase) || isEmptyDirectory(targetBase)) {
            return true;
        }
        String prefix = "/" + properties.getDistributionClasspath() + "/";
        for (Resource res : resources) {
            if (!res.isReadable() || res.getFilename() == null) continue;
            String rel = extractRelative(res, prefix);
            Path dest = targetBase.resolve(rel);
            long srcTs = res.lastModified();
            long dstTs = Files.exists(dest)
                    ? Files.getLastModifiedTime(dest).toMillis()
                    : -1;
            if (srcTs > dstTs) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if the directory exists but has no files/subdirectories. */
    private boolean isEmptyDirectory(Path dir) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            return !ds.iterator().hasNext();
        }
    }

    /** Deletes the directory (if present) and recreates it empty. */
    private void cleanDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            FileSystemUtils.deleteRecursively(dir);
        }
        Files.createDirectories(dir);
    }

    /** Copies all given resources into the target base, preserving structure. */
    private void copyAll(Resource[] resources, Path targetBase) throws IOException {
        String prefix = "/" + properties.getDistributionClasspath() + "/";
        for (Resource res : resources) {
            if (!res.isReadable() || res.getFilename() == null) continue;
            String rel = extractRelative(res, prefix);
            Path dest = targetBase.resolve(rel);
            Files.createDirectories(dest.getParent());
            try (InputStream in = res.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("Copied UI file: {}", dest);
        }
    }

    /**
     * Given a resource URL like ".../dist/path/to/file", strip everything
     * up to and including the prefix so we get "path/to/file".
     */
    private String extractRelative(Resource resource, String prefix) throws IOException {
        String url = resource.getURL().toString();
        int idx = url.indexOf(prefix);
        String rel = (idx >= 0)
                ? url.substring(idx + prefix.length())
                : resource.getFilename();
        return rel.startsWith("/") ? rel.substring(1) : rel;
    }
}

