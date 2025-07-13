package io.github.bitfist.jcef.spring.browser.javascript;

import lombok.RequiredArgsConstructor;
import org.cef.browser.CefBrowser;

/**
 * Default implementation of the {@link JavaScriptExecutor} interface.
 * Provides a mechanism for executing JavaScript code in the context of a CEF (Chromium Embedded Framework) browser.
 */
@RequiredArgsConstructor
public class DefaultJavaScriptExecutor implements JavaScriptExecutor {

    private final CefBrowser cefBrowser;

    @Override
    public void execute(String code) {
        cefBrowser.executeJavaScript(code, null, 0);
    }
}
