package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import me.friwi.jcefmaven.EnumProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.info.BuildProperties;

import java.time.Instant;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*", disabledReason = "Needs Swing, cannot run in CI")
class DefaultSplashScreenTest {

    @Test
    @DisplayName("🛠️ Initialize without splash resource and no build properties")
    void shouldInitializeWithoutSplashAndNoBuildProps() {
        var props = new JcefApplicationProperties("test", null, null);

        assertDoesNotThrow(() -> new DefaultInstallerSplashScreen(props, null));
    }

    @Test
    @DisplayName("📁 Initialize with invalid splash resource path")
    void shouldInitializeWithInvalidSplashPath() {
        var props = new JcefApplicationProperties("test", "nonexistent/image.png", null);

        assertDoesNotThrow(() -> new DefaultInstallerSplashScreen(props, null));
    }

    @Test
    @DisplayName("🔧 Initialize with valid splash path and build properties")
    void shouldInitializeWithInvalidSplashAndBuildProps() {
        var props = new JcefApplicationProperties("test", "empty.png", null);

        var info = new Properties();
        info.put("version", "1.2.3");
        info.put("time", Instant.now().toString());
        var buildProps = new BuildProperties(info);

        assertDoesNotThrow(() -> new DefaultInstallerSplashScreen(props, buildProps));
    }

    @Test
    @DisplayName("🚀 DOWNLOADING with percentage ≥ 0")
    void shouldHandleDownloadingWithProgress() {
        var screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.DOWNLOADING, 75f));

        // clean up
        hideScreen(screen);
    }

    @Test
    @DisplayName("⌛ DOWNLOADING with indeterminate (-1)")
    void shouldHandleDownloadingIndeterminate() {
        var screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.DOWNLOADING, EnumProgress.NO_ESTIMATION));

        // clean up
        hideScreen(screen);
    }

    @Test
    @DisplayName("🔄 EXTRACTING")
    void shouldHandleExtracting() {
        var screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.EXTRACTING, 0f));

        // clean up
        hideScreen(screen);
    }

    @Test
    @DisplayName("❌ other states (e.g. INITIALIZING)")
    void shouldHandleOtherState() {
        var screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.INITIALIZING, 0f));
    }

    private DefaultInstallerSplashScreen createScreen() {
        var props = new JcefApplicationProperties("test", null, null);
        return new DefaultInstallerSplashScreen(props, null);
    }

    private void hideScreen(DefaultInstallerSplashScreen screen) {
        screen.handleProgress(EnumProgress.INITIALIZING, 0f);
    }
}
