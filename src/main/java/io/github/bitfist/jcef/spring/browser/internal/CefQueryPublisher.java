package io.github.bitfist.jcef.spring.browser.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.query.CefQueryEvent;
import io.github.bitfist.jcef.spring.query.CefQueryException;
import io.github.bitfist.jcef.spring.query.CefQueryJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Handles incoming Chromium Embedded Framework (CEF) queries and publishes
 * associated events to the application event system. This class extends
 * {@code CefMessageRouterHandlerAdapter} and provides default handling for
 * browser queries by processing JSON payloads and emitting application-specific
 * events.
 */
@Slf4j
@RequiredArgsConstructor
class CefQueryPublisher extends CefMessageRouterHandlerAdapter {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        try {
            CefQueryJson payload = objectMapper.readValue(request, CefQueryJson.class);
            CefQueryEvent event = new CefQueryEvent(payload, callback);
            eventPublisher.publishEvent(event);
        } catch (CefQueryException exception) {
            log.error("[ERROR] {} [CODE] {}", exception.getMessage(), exception.getErrorCode(), exception);
            callback.failure(exception.getErrorCode(), exception.getMessage());
        } catch (JsonProcessingException exception) {
            log.error("Failed to parse JSON payload: {}", exception.getMessage(), exception);
            callback.failure(500, "Failed to parse JSON payload: " + exception.getMessage());
        } catch (Throwable throwable) {
            log.error("Unexpected error: {}", throwable.getMessage(), throwable);
            callback.failure(500, "Unexpected error: " + throwable.getMessage());
        }
        return true;
    }
}
