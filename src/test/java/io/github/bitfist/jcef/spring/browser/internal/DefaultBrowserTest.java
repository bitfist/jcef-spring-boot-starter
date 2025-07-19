package io.github.bitfist.jcef.spring.browser.internal;

import org.cef.browser.CefBrowser;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultBrowserTest {

    @Test
    void executeJavaScript() {
        CefBrowser cefBrowser = mock(CefBrowser.class);
        var code = "console.log('Hello World!');";

        new DefaultBrowser(cefBrowser).executeJavaScript(code);

        verify(cefBrowser).executeJavaScript(code, null, 0);
    }

}