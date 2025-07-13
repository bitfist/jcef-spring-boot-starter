package io.github.bitfist.jcef.spring.debug.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.browser.CefApplicationCustomizer;
import io.github.bitfist.jcef.spring.browser.CefClientCustomizer;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.swing.SwingUtilities;

/**
 * 🐞 Configures debugging features for JCEF:
 * <ul>
 * <li>Enables developer tools UI on page load end if configured.</li>
 * <li>Sets up remote debugging port for external debuggers.</li>
 * </ul>
 */
@AutoConfiguration
class DebugAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "jcef.development-options.show-developer-tools", havingValue = "true")
    CefClientCustomizer developerToolsCustomizer() {
        return cefClient -> {
            cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatus) {
                    SwingUtilities.invokeLater(browser::openDevTools);
                }
            });
        };
    }

    @Bean
    @ConditionalOnProperty(name = "jcef.development-options.debug-port")
    CefApplicationCustomizer debugPortCustomizer(JcefApplicationProperties applicationProperties) {
        return builder -> {
            builder.getCefSettings().remote_debugging_port = applicationProperties.getDevelopmentOptions().debugPort();
            builder.addJcefArgs("--remote-allow-origins=*");
        };
    }
}
