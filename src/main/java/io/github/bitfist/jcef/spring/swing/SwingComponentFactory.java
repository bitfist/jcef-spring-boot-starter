package io.github.bitfist.jcef.spring.swing;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.util.function.BiConsumer;

public class SwingComponentFactory {

    public JFrame createJFrame() {
        return new JFrame();
    }

    public JLabel createJLabel(String text) {
        return new JLabel(text);
    }

    public JProgressBar createJProgressBar(int min, int max) {
        return new JProgressBar(min, max);
    }

    public JPanel createJPanel(LayoutManager layout) {
        return new JPanel(layout);
    }

    public JPanel createJPanel(BiConsumer<JPanel, Graphics> renderer) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderer.accept(this, g);
            }
        };
    }

}
