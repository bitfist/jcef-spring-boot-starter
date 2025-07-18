package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("🖼️  UIInstaller Tests")
class UIInstallerTest {

    /**
     * Minimal subclass to disable the heavy install process during tests.
     */
    static class TestUIInstaller extends UIInstaller {
        TestUIInstaller(JcefApplicationProperties p) {
            super(p);
        }

        @Override
        public void installUIResources() {
        }

        String extractRelativeForTest(Resource r, String p) throws Exception {
            var m = UIInstaller.class.getDeclaredMethod("extractRelative", Resource.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(this, r, p);
        }

        boolean isEmptyDirectoryForTest(Path d) throws Exception {
            var m = UIInstaller.class.getDeclaredMethod("isEmptyDirectory", Path.class);
            m.setAccessible(true);
            return (boolean) m.invoke(this, d);
        }
    }

    @Test
    @DisplayName("🔖  extractRelative strips prefix and leading slash")
    void whenExtractRelative_thenReturnsRelativePath() throws Exception {
        var props = new JcefApplicationProperties("app", null, "ui");
        var installer = new TestUIInstaller(props);

        Resource res = mock(Resource.class);
        when(res.getURL()).thenReturn(new URL("file:/opt/app/ui/css/style.css"));
        when(res.getFilename()).thenReturn("style.css");

        assertEquals("css/style.css",
                installer.extractRelativeForTest(res, "/ui/"));
    }

    @Test
    @DisplayName("📂  isEmptyDirectory returns true for an empty directory")
    void whenDirectoryEmpty_thenIsEmptyDirectoryTrue() throws Exception {
        Path tmp = Files.createTempDirectory("uitest");
        tmp.toFile().deleteOnExit();

        var props = new JcefApplicationProperties("app", null, "ui");
        var installer = new TestUIInstaller(props);

        assertTrue(installer.isEmptyDirectoryForTest(tmp));
    }
}
