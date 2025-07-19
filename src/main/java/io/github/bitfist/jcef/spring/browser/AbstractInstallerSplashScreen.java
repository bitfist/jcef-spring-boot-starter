package io.github.bitfist.jcef.spring.browser;

import me.friwi.jcefmaven.IProgressHandler;

import javax.swing.JFrame;

/**
 * 📦 Abstract frame to display progress of JCEF installation.
 * Implements IProgressHandler for integration with the Maven-based installer.
 * <p>
 * See:
 * 🔧 DefaultInstallationProgressFrame for the standard implementation.
 */
public abstract class AbstractInstallerSplashScreen extends JFrame implements IProgressHandler {

    /**
     * 🔨 Construct frame with given window title.
     *
     * @param title Title shown on the frame header.
     */
    public AbstractInstallerSplashScreen(String title) {
        super(title);
    }

}
