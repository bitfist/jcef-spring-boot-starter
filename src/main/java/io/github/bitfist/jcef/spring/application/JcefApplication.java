package io.github.bitfist.jcef.spring.application;

import lombok.SneakyThrows;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.swing.UIManager;
import java.util.function.Consumer;

/**
 * ⚙️ Utility class to bootstrap a non-headless Spring Boot application with JCEF.
 */
public abstract class JcefApplication {

    // Prevent instantiation
    private JcefApplication() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 🚀 Run the application without additional customizations.
     */
    public static <T> void run(Class<T> clazz, String[] args) {
        run(clazz, args, builder -> {});
    }

    /**
     * 🚀 Run the application with custom SpringApplicationBuilder adjustments.
     *
     * @param clazz Spring Boot annotated main class.
     * @param args Application arguments.
     * @param customizer Callback to tweak the SpringApplicationBuilder.
     */
    @SneakyThrows
    public static <T> void run(Class<T> clazz, String[] args, Consumer<SpringApplicationBuilder> customizer) {
        // 🖥 Set native look-and-feel
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        var builder = new SpringApplicationBuilder(clazz);
        builder.headless(false);
        customizer.accept(builder);
        builder.run(args);
    }
}
