package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.browser.AbstractInstallerSplashScreen;
import lombok.SneakyThrows;
import me.friwi.jcefmaven.EnumProgress;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.info.BuildProperties;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 📊 Default UI implementation showing JCEF installation progress, with splash image and version overlay.
 */
class DefaultInstallerSplashScreen extends AbstractInstallerSplashScreen {

    private final JProgressBar progressBar;
    private final JLabel stateLabel;

    public DefaultInstallerSplashScreen(JcefApplicationProperties applicationProperties, @Nullable BuildProperties buildProperties) {
        super("Setup");
        setUndecorated(true);

        var imagePanel = getImagePanel(applicationProperties, buildProperties);

        // 4) State label and progress bar below the image
        stateLabel = new JLabel("");
        progressBar = new JProgressBar(0, 100);

        var bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(stateLabel, BorderLayout.NORTH);
        bottom.add(progressBar, BorderLayout.SOUTH);

        // Frame layout
        getContentPane().setLayout(new BorderLayout());
        if (imagePanel != null) {
            getContentPane().add(imagePanel, BorderLayout.CENTER);
        }
        getContentPane().add(bottom, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(getSize());
    }

    @SneakyThrows
    private @Nullable JPanel getImagePanel(JcefApplicationProperties applicationProperties, @Nullable BuildProperties buildProperties) {
        var path = applicationProperties.getSplashScreenClasspathResource();
        if (path == null) {
            return null;
        }
        var imgUrl = getClass().getClassLoader().getResource(path);
        if (imgUrl == null) {
            return null;
        }

        // 1) Load splash image
        BufferedImage splashImage = ImageIO.read(imgUrl);

        // 2) Image panel with version overlay
        var imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(splashImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        imagePanel.setLayout(new BorderLayout());
        var size = new Dimension(splashImage.getWidth(), splashImage.getHeight());
        imagePanel.setMinimumSize(size);
        imagePanel.setPreferredSize(size);

        if (buildProperties != null && isNotBlank(buildProperties.getVersion()) && buildProperties.getTime() != null) {
            // 3) Version overlay in bottom‐right
            var date = buildProperties.getTime().toString();
            var version = buildProperties.getVersion();
            var versionLabel = new JLabel(version + " (" + date + ")");
            versionLabel.setForeground(Color.WHITE);

            var overlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            overlay.setOpaque(true);
            overlay.setBackground(new Color(0, 0, 0, 120));
            overlay.add(versionLabel);

            // ★ HERE ★ lower‐right corner
            imagePanel.add(overlay, BorderLayout.SOUTH);
        }

        return imagePanel;
    }

    /**
     * 🔄 Updates the progress bar and state label based on the current installation state.
     *
     * @param state   Current progress stage (DOWNLOADING/EXTRACTING/etc).
     * @param percent Percentage completed for downloading (or -1 for indeterminate).
     */
    @Override
    public void handleProgress(EnumProgress state, float percent) {
        SwingUtilities.invokeLater(() -> {
            switch (state) {
                case DOWNLOADING -> {
                    stateLabel.setText("Downloading…");
                    if (percent >= 0) {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue((int) percent);
                    } else {
                        progressBar.setIndeterminate(true);
                    }
                    setVisible(true);
                }
                case EXTRACTING -> {
                    stateLabel.setText("Extracting…");
                    progressBar.setIndeterminate(true);
                    setVisible(true);
                }
                default -> setVisible(false);
            }
        });
    }
}
