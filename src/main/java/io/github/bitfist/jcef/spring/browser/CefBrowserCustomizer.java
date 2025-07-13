package io.github.bitfist.jcef.spring.browser;

import org.cef.browser.CefBrowser;
import java.util.function.Consumer;

/**
 * 🔧 Hook to customize the CefBrowser instance after creation.
 */
public interface CefBrowserCustomizer extends Consumer<CefBrowser> {

}
