package io.github.bitfist.jcef.spring.browser;

import me.friwi.jcefmaven.CefAppBuilder;

import java.util.function.Consumer;

/**
 * 🔧 Allows customization of the underlying CefAppBuilder before creating the CefApp.
 * Can be implemented with lambdas via <code>Consumer<CefAppBuilder></code>.
 */
public interface CefApplicationCustomizer extends Consumer<CefAppBuilder> {
}
