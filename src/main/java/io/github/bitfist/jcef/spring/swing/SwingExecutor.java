package io.github.bitfist.jcef.spring.swing;

import javax.swing.SwingUtilities;

public class SwingExecutor {

    public void invokeLater(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }
}
