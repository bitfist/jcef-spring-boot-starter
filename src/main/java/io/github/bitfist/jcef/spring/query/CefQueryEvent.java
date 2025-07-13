package io.github.bitfist.jcef.spring.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cef.callback.CefQueryCallback;

/**
 * <p>
 * Represents an event associated with a Chromium Embedded Framework (CEF) query.
 * This class encapsulates the payload of the query and a callback object to handle
 * the query's response or any potential errors.
 * </p>
 * <p>
 * The {@code CefQueryJson} object contains the route and payload data of the query.
 * The {@code CefQueryCallback} is used to send results or errors back to the browser
 * context.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public class CefQueryEvent {

    private final CefQueryJson payload;
    private final CefQueryCallback callback;
}
