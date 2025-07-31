package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import java.nio.file.Paths;

/**
 * Utility class for file path manipulations.
 */
final class PathUtils {

	private PathUtils() {
	}

	/**
	 * Calculates the relative path to import from a target package to a source package.
	 *
	 * @param fromPath The path of the file needing the import (e.g., "com/app/ui").
	 * @param toPath   The path of the file to be imported (e.g., "com/app/core").
	 * @return The relative path string (e.g., "../core").
	 */
	public static String getRelativePath(String fromPath, String toPath) {
		if (fromPath.equals(toPath)) {
			return ".";
		}

		var relativePath = Paths.get(fromPath).relativize(Paths.get(toPath));
		var result = relativePath.toString().replace('\\', '/');

		// If the path does not start with a '.', it's a subfolder, so prepend './'
		if (!result.startsWith(".") && !result.startsWith("/")) {
			return "./" + result;
		}

		return result;
	}
}