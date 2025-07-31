package io.github.bitfist.jcef.spring.application;

import lombok.SneakyThrows;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.Map;
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
	public static <T> void run(Class<T> clazz, String applicationName, String[] args) {
		run(clazz, applicationName, args, builder -> {
		});
	}

	/**
	 * 🚀 Run the application with custom SpringApplicationBuilder adjustments.
	 *
	 * @param clazz      Spring Boot annotated application class.
	 * @param args       Application arguments.
	 * @param customizer Callback to tweak the SpringApplicationBuilder.
	 */
	@SneakyThrows
	public static <T> void run(Class<T> clazz, String applicationName, String[] args, Consumer<SpringApplicationBuilder> customizer) {
		// 🖥 Set native look-and-feel
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		var appDataPath = getAppDataPath().toAbsolutePath();
		var builder = new SpringApplicationBuilder(clazz);
		builder.headless(false);
		builder.properties(Map.of(
				"jcef.application-name", applicationName,
				"jcef.installation-path", appDataPath.resolve(applicationName).toString()
		));
		customizer.accept(builder);
		builder.run(args);
	}

	/**
	 * 🗂 Determine the platform-specific installation path.
	 */
	private static Path getAppDataPath() {
		try {
			var osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("win")) {
				return Path.of(System.getenv("APPDATA"));
			} else if (osName.contains("mac")) {
				return Path.of(System.getProperty("user.home"), "Library", "Application Support");
			} else {
				String xdg = System.getenv("XDG_DATA_HOME");
				if (xdg != null && !xdg.isBlank()) {
					return Path.of(xdg);
				} else {
					return Path.of(System.getProperty("user.home"), ".local", "share");
				}
			}
		} catch (NullPointerException e) {
			return Path.of("~");
		}
	}
}
