package io.github.bitfist.jcef.spring.tsobject.internal;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A REST endpoint for invoking methods via HTTP POST requests.
 * This controller is activated when the configuration property
 * `jcef.tsobject.enable-web-communication` is set to `true`.
 * <p>
 * The endpoint is mapped to the URL path `/jcef`.
 * Incoming requests are processed through the {@link MethodInvokingCefQueryHandler},
 * which handles deserialization of the input query, method invocation,
 * and serialization of any response data.
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Accepts POST requests with a JSON-formatted query in the request body.</li>
 * <li>Delegates query processing to the {@link MethodInvokingCefQueryHandler}.</li>
 * <li>Supports nullable input and output for enhanced flexibility in communication.</li>
 * </ul>
 */
@Controller
@ConditionalOnProperty(name = "jcef.tsobject.enable-web-communication", havingValue = "true")
@RequestMapping("/jcef")
@RequiredArgsConstructor
class MethodInvokingRestEndpoint {

    private final MethodInvokingCefQueryHandler queryHandler;

    @PostMapping
    @Nullable
    String invokeMethod(@RequestBody @Nullable String query) {
        return queryHandler.handleQuery(query);
    }
}
