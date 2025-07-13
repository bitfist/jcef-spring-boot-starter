package io.github.bitfist.jcef.spring.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("🚨  CefQueryException Tests")
class CefQueryExceptionTest {

    @Test
    @DisplayName("✔️  Retains error code and message")
    void testErrorCodeAndMessage() {
        CefQueryException ex = new CefQueryException(404, "Not found");
        assertEquals(404, ex.getErrorCode());
        assertEquals("Not found", ex.getMessage());
    }
}
