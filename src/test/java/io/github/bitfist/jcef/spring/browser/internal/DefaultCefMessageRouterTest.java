package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.CefMessageException;
import io.github.bitfist.jcef.spring.browser.CefMessageHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the DefaultCefMessageRouter class.
 * This test suite uses JUnit 5 and Mockito to verify the behavior of the onQuery method.
 */
@ExtendWith(MockitoExtension.class)
class DefaultCefMessageRouterTest {

    // Test constants
    private static final long QUERY_ID = 12345L;
    private static final String TEST_REQUEST = "{\"action\":\"testAction\",\"payload\":\"testData\"}";
    private static final boolean PERSISTENT = false;

    // Mocks for the dependencies required by the onQuery method.
    @Mock
    private CefMessageHandler messageHandler;

    @Mock
    private CefBrowser browser;

    @Mock
    private CefFrame frame;

    @Mock
    private CefQueryCallback callback;

    // The instance of the class we are testing, with mocks injected.
    @InjectMocks
    private DefaultCefMessageRouter cefMessageRouter;

    @Test
    @DisplayName("✅ Success Path - Should handle query successfully and call callback.success")
    void onQuery_whenHandlerSucceeds_shouldCallSuccess() throws CefMessageException {
        // Arrange: Define the expected successful result from the message handler.
        String expectedResult = "{\"status\":\"success\",\"data\":\"some result\"}";
        when(messageHandler.handleQuery(TEST_REQUEST)).thenReturn(expectedResult);

        // Act: Call the method under test.
        boolean result = cefMessageRouter.onQuery(browser, frame, QUERY_ID, TEST_REQUEST, PERSISTENT, callback);

        // Assert: Verify the interactions and the return value.
        // 1. Ensure the message handler was called with the correct request.
        verify(messageHandler).handleQuery(TEST_REQUEST);
        // 2. Ensure the callback's success method was called with the handler's result.
        verify(callback).success(expectedResult);
        // 3. Ensure no failure methods were called on the callback.
        verify(callback, never()).failure(anyInt(), anyString());
        // 4. The method should always return true.
        assertTrue(result, "The onQuery method should return true on success.");
    }

    @Test
    @DisplayName("❌ Exception Path - Should handle CefMessageException and call callback.failure")
    void onQuery_whenHandlerThrowsCefMessageException_shouldCallFailure() throws CefMessageException {
        // Arrange: Configure the message handler to throw a specific CefMessageException.
        int errorCode = 404;
        String errorMessage = "Resource not found";
        CefMessageException cefException = new CefMessageException(errorCode, new Exception(errorMessage));
        when(messageHandler.handleQuery(TEST_REQUEST)).thenThrow(cefException);

        // Act: Call the method under test.
        boolean result = cefMessageRouter.onQuery(browser, frame, QUERY_ID, TEST_REQUEST, PERSISTENT, callback);

        // Assert: Verify the error handling logic.
        // 1. Ensure the message handler was called.
        verify(messageHandler).handleQuery(TEST_REQUEST);
        // 2. Ensure the callback's failure method was called with the exception's code and message.
        verify(callback).failure(errorCode, "java.lang.Exception: " + errorMessage);
        // 3. Ensure the success method was never called.
        verify(callback, never()).success(anyString());
        // 4. The method should always return true.
        assertTrue(result, "The onQuery method should return true even when a CefMessageException occurs.");
    }

    @Test
    @DisplayName("💥 Exception Path - Should handle unexpected Throwable and call callback.failure")
    void onQuery_whenHandlerThrowsUnexpectedException_shouldCallFailureWith500() throws CefMessageException {
        // Arrange: Configure the message handler to throw a generic, unexpected exception.
        String errorMessage = "Something went terribly wrong!";
        RuntimeException unexpectedException = new RuntimeException(errorMessage);
        when(messageHandler.handleQuery(TEST_REQUEST)).thenThrow(unexpectedException);

        // Act: Call the method under test.
        boolean result = cefMessageRouter.onQuery(browser, frame, QUERY_ID, TEST_REQUEST, PERSISTENT, callback);

        // Assert: Verify the generic error handling logic.
        // 1. Ensure the message handler was called.
        verify(messageHandler).handleQuery(TEST_REQUEST);
        // 2. Ensure the callback's failure method was called with a 500 status code and a generic message.
        verify(callback).failure(500, "Unexpected error: " + errorMessage);
        // 3. Ensure the success method was never called.
        verify(callback, never()).success(anyString());
        // 4. The method should always return true.
        assertTrue(result, "The onQuery method should return true even when an unexpected error occurs.");
    }
}