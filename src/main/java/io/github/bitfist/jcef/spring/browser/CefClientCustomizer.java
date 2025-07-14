package io.github.bitfist.jcef.spring.browser;

import org.cef.CefClient;

import java.util.function.Consumer;

/**
 * 🔧 Hook to customize the CefClient instance (e.g., message routers, handlers).
 */
public interface CefClientCustomizer extends Consumer<CefClient> {

}
