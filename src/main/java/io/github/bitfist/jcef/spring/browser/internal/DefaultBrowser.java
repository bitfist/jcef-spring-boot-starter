package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.Browser;
import lombok.RequiredArgsConstructor;
import org.cef.browser.CefBrowser;

@RequiredArgsConstructor
class DefaultBrowser implements Browser {

    private final CefBrowser cefBrowser;

    @Override
    public void executeJavaScript(String code) {
        cefBrowser.executeJavaScript(code, null, 0);
    }
}
