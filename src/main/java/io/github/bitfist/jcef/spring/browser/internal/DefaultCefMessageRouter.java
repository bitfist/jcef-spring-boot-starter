package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.CefQueryException;
import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

/**
 * Handles incoming Chromium Embedded Framework (CEF) queries and publishes
 * associated events to the application event system. This class extends
 * {@code CefMessageRouterHandlerAdapter} and provides default handling for
 * browser queries by processing JSON payloads and emitting application-specific
 * events.
 */
@Slf4j
@RequiredArgsConstructor
class DefaultCefMessageRouter extends CefMessageRouterHandlerAdapter {

    private final CefQueryHandler messageHandler;

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        try {
            var result = messageHandler.handleQuery(request);
            callback.success(result);
            return true;
        } catch (CefQueryException exception) {
            log.error("[ERROR] {} [CODE] {}", exception.getMessage(), exception.getErrorCode(), exception);
            callback.failure(exception.getErrorCode(), exception.getMessage());
        } catch (Throwable throwable) {
            log.error("Unexpected error: {}", throwable.getMessage(), throwable);
            callback.failure(500, "Unexpected error: " + throwable.getMessage());
        }
        return true;
    }
}
