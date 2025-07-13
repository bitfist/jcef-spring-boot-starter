package io.github.bitfist.jcef.spring.browser;

import javax.swing.JFrame;
import java.util.function.Consumer;

/**
 * 🔧 Hook to customize the JFrame that hosts the CefBrowser UI component.
 */
public interface CefBrowserFrameCustomizer extends Consumer<JFrame> {

}
