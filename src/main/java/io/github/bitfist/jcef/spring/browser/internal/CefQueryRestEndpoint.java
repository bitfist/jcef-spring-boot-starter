package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A REST endpoint for invoking methods via HTTP POST requests.
 * This controller is activated when the configuration property
 * `jcef.development.enable-web-communication` is set to `true`.
 * <p>
 * The endpoint is mapped to the URL path `/jcef`.
 * Incoming requests are processed through the {@link CefQueryHandler},
 * which handles deserialization of the input query, method invocation,
 * and serialization of any response data.
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Accepts POST requests with a JSON-formatted query in the request body.</li>
 * <li>Delegates query processing to the {@link CefQueryHandler}.</li>
 * <li>Supports nullable input and output for enhanced flexibility in communication.</li>
 * </ul>
 */
@RestController
@ConditionalOnProperty(name = "jcef.development.enable-web-communication", havingValue = "true")
@RequestMapping("/jcef")
@RequiredArgsConstructor
class CefQueryRestEndpoint {

    private final CefQueryHandler queryHandler;

    @PostMapping
    @Nullable
    String invokeMethod(@RequestBody @Nullable String query) {
        return queryHandler.handleQuery(query);
    }
}
