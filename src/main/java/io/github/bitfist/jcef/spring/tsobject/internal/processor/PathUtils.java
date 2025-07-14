package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for file path manipulations.
 */
final class PathUtils {

    private PathUtils() {}

    /**
     * Calculates the relative path to import from a target package to a source package.
     *
     * @param fromPackage The package of the file needing the import (e.g., "com.app.ui").
     * @param toPackage   The package of the file to be imported (e.g., "com.app.core").
     * @return The relative path string (e.g., "../core").
     */
    public static String getRelativePath(String fromPackage, String toPackage) {
        if (fromPackage.equals(toPackage)) {
            return ".";
        }

        Path fromPath = Paths.get(fromPackage.replace('.', '/'));
        Path toPath = Paths.get(toPackage.replace('.', '/'));

        Path relativePath = fromPath.relativize(toPath);
        String result = relativePath.toString().replace('\\', '/');

        // If the path does not start with '.', it's a subfolder, so prepend './'
        if(!result.startsWith(".") && !result.startsWith("/")){
            return "./" + result;
        }

        return result;
    }
}