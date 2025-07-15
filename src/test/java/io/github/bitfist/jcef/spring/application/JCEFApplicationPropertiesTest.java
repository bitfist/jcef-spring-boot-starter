package io.github.bitfist.jcef.spring.application;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties.DevelopmentOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("📁  JCEFApplicationProperties Tests")
class JcefApplicationPropertiesTests {

    @Test
    @DisplayName("📦  Distribution classpath defaults to «ui» when blank")
    void whenDistributionClasspathBlank_thenDefaultToUi() {
        var props = new JcefApplicationProperties("myApp", null, "ui", new DevelopmentOptions(null, false));
        assertEquals("ui/", props.getDistributionClasspath());
    }

    @Test
    @DisplayName("🗃️  Installation path is cached (same instance)")
    void whenInstallationPathCalledTwice_thenCached() {
        var props = new JcefApplicationProperties("app", null, "ui", new DevelopmentOptions(null, false));
        var p1 = props.getInstallationPath();
        var p2 = props.getInstallationPath();
        assertSame(p1, p2);
    }

    @Test
    @DisplayName("🧩  Child paths are derived from installation path")
    void whenChildPathsRequested_thenDerivedCorrectly() {
        var props = new JcefApplicationProperties("x", null, "ui", new DevelopmentOptions(null, false));
        var base = props.getInstallationPath();
        assertEquals(base.resolve("bundle"), props.getJcefInstallationPath());
        assertEquals(base.resolve("ui"), props.getUiInstallationPath());
    }

    @Test
    @DisplayName("\uD83D\uDCA3  Application name is expected")
    void whenApplicationNameMissing_thenExpectIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JcefApplicationProperties("", null, "ui", new DevelopmentOptions(null, false)));
    }

    @Test
    @DisplayName("\uD83D\uDCA3  index.html is is expected under distributionClasspath")
    void whenIndexHtmlMissing_thenExpectIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JcefApplicationProperties("x", null, "test", new DevelopmentOptions(null, false)));
    }
}
