package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.CefBrowserFrameCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * 🚀 Component that initializes and starts the JCEF browser UI
 * once the Spring application is fully ready.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BrowserStarter {

    private final CefApp cefApp;
    private final CefBrowser cefBrowser;
    private final List<CefBrowserFrameCustomizer> cefBrowserFrameCustomizers;

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = createFrame(cefBrowser, cefApp);
            frame.setVisible(true);
        });
    }

    private JFrame createFrame(CefBrowser browser, CefApp cefApp) {
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cefApp.dispose();
                frame.dispose();
                System.exit(0);
            }
        });
        cefBrowserFrameCustomizers.forEach(consumer -> consumer.accept(frame));
        return frame;
    }
}
