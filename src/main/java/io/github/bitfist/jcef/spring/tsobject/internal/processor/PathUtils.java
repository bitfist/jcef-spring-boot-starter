package io.github.bitfist.jcef.spring.tsobject.internal.processor;

/**
 * Utility class for file path manipulations.
 */
final class PathUtils {

	private PathUtils() {
	}

	static String calculateRelativePath(String fromPath, String toPath, String fileName) {
		var fromParts = fromPath.split("/");
		var toParts = toPath.split("/");

		// Find common prefix
		var commonLength = 0;
		for (var i = 0; i < Math.min(fromParts.length, toParts.length); i++) {
			if (fromParts[i].equals(toParts[i])) {
				commonLength++;
			} else {
				break;
			}
		}

		// Build relative path
		var path = new StringBuilder();

		// Go up from source
		path.append("../".repeat(Math.max(0, fromParts.length - commonLength)));

		// Go down to target
		for (var i = commonLength; i < toParts.length; i++) {
			path.append(toParts[i]).append("/");
		}

		// Add filename
		path.append(fileName);

		// If in same directory, use ./
		if (path.length() == fileName.length()) {
			return "./" + fileName;
		}

		return path.toString();
	}
}