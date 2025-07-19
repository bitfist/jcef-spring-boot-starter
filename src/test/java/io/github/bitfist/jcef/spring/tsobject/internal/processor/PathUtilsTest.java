package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathUtilsTest {

    @Test
    @DisplayName("Should return '.' when packages are the same")
    void getRelativePath_SamePackage() {
        assertEquals(".", PathUtils.getRelativePath("com/app/ui", "com/app/ui"));
    }

    @Test
    @DisplayName("Should return './core' for a direct sub-package")
    void getRelativePath_ToSubPackage() {
        assertEquals("./core", PathUtils.getRelativePath("com/app", "com/app/core"));
    }

    @Test
    @DisplayName("Should return '../' for a direct parent package")
    void getRelativePath_ToParentPackage() {
        assertEquals("..", PathUtils.getRelativePath("com/app/ui", "com/app"));
    }

    @Test
    @DisplayName("Should return '../core' for a sibling package")
    void getRelativePath_ToSiblingPackage() {
        assertEquals("../core", PathUtils.getRelativePath("com/app/ui", "com/app/core"));
    }

    @Test
    @DisplayName("Should return '../../api/model' for a complex relative path")
    void getRelativePath_ComplexPath() {
        assertEquals("../../api/model", PathUtils.getRelativePath("com/app/ui/components", "com/app/api/model"));
    }

    @Test
    @DisplayName("Should handle root packages correctly")
    void getRelativePath_FromRoot() {
        assertEquals("./model", PathUtils.getRelativePath("", "model"));
    }
}