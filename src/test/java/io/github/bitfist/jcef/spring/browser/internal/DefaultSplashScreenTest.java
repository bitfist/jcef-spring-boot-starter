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
        JcefApplicationProperties props = new JcefApplicationProperties("test", null, null);

        assertDoesNotThrow(() -> new DefaultSplashScreen(props, null));
    }

    @Test
    @DisplayName("📁 Initialize with invalid splash resource path")
    void shouldInitializeWithInvalidSplashPath() {
        JcefApplicationProperties props = new JcefApplicationProperties("test", "nonexistent/image.png", null);

        assertDoesNotThrow(() -> new DefaultSplashScreen(props, null));
    }

    @Test
    @DisplayName("🔧 Initialize with valid splash path and build properties")
    void shouldInitializeWithInvalidSplashAndBuildProps() {
        JcefApplicationProperties props = new JcefApplicationProperties("test", "empty.png", null);

        Properties info = new Properties();
        info.put("version", "1.2.3");
        info.put("time", Instant.now().toString());
        BuildProperties buildProps = new BuildProperties(info);

        assertDoesNotThrow(() -> new DefaultSplashScreen(props, buildProps));
    }

    @Test
    @DisplayName("🚀 DOWNLOADING with percentage ≥ 0")
    void shouldHandleDownloadingWithProgress() {
        DefaultSplashScreen screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.DOWNLOADING, 75f));

        // clean up
        hideScreen(screen);
    }

    @Test
    @DisplayName("⌛ DOWNLOADING with indeterminate (-1)")
    void shouldHandleDownloadingIndeterminate() {
        DefaultSplashScreen screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.DOWNLOADING, EnumProgress.NO_ESTIMATION));

        // clean up
        hideScreen(screen);
    }

    @Test
    @DisplayName("🔄 EXTRACTING")
    void shouldHandleExtracting() {
        DefaultSplashScreen screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.EXTRACTING, 0f));

        // clean up
        hideScreen(screen);
    }

    @Test
    @DisplayName("❌ other states (e.g. INITIALIZING)")
    void shouldHandleOtherState() {
        DefaultSplashScreen screen = createScreen();
        assertDoesNotThrow(() -> screen.handleProgress(EnumProgress.INITIALIZING, 0f));
    }

    private DefaultSplashScreen createScreen() {
        JcefApplicationProperties props = new JcefApplicationProperties("test", null, null);
        return new DefaultSplashScreen(props, null);
    }

    private void hideScreen(DefaultSplashScreen screen) {
        screen.handleProgress(EnumProgress.INITIALIZING, 0f);
    }
}
