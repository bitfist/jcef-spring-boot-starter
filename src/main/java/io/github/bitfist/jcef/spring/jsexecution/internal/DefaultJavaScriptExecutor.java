package io.github.bitfist.jcef.spring.jsexecution.internal;

import io.github.bitfist.jcef.spring.browser.Browser;
import io.github.bitfist.jcef.spring.jsexecution.JavaScriptExecutor;
import lombok.RequiredArgsConstructor;

/**
 * Default implementation of the {@link JavaScriptExecutor} interface.
 * Provides a mechanism for executing JavaScript code in the context of a CEF (Chromium Embedded Framework) browser.
 */
@RequiredArgsConstructor
class DefaultJavaScriptExecutor implements JavaScriptExecutor {

    private final Browser browser;

    @Override
    public void execute(String code) {
        browser.executeJavaScript(code);
    }
}
