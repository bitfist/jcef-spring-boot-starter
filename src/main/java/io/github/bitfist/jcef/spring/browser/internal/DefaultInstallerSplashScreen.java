package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.swing.SwingComponentFactory;
import io.github.bitfist.jcef.spring.swing.SwingExecutor;
import lombok.SneakyThrows;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.info.BuildProperties;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 📊 Default UI implementation showing JCEF installation progress, with splash image and version overlay.
 */
class DefaultInstallerSplashScreen implements IProgressHandler {

	private final JFrame frame;
	private final SwingComponentFactory swingComponentFactory;
	private final SwingExecutor swingExecutor;
	private final JProgressBar progressBar;
	private final JLabel stateLabel;

	public DefaultInstallerSplashScreen(SwingComponentFactory swingComponentFactory, SwingExecutor swingExecutor, JcefApplicationProperties applicationProperties, @Nullable BuildProperties buildProperties) {
		this.swingComponentFactory = swingComponentFactory;
		this.swingExecutor = swingExecutor;
		this.frame = swingComponentFactory.createJFrame();

		frame.setTitle("Setup");
		frame.setUndecorated(true);

		var imagePanel = createImagePanel(applicationProperties, buildProperties);

		stateLabel = swingComponentFactory.createJLabel("");
		progressBar = swingComponentFactory.createJProgressBar(0, 100);

		var bottom = swingComponentFactory.createJPanel(new BorderLayout(5, 5));
		bottom.add(stateLabel, BorderLayout.NORTH);
		bottom.add(progressBar, BorderLayout.SOUTH);

		var contentPane = frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		if (imagePanel != null) {
			contentPane.add(imagePanel, BorderLayout.CENTER);
		}
		contentPane.add(bottom, BorderLayout.SOUTH);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setMinimumSize(frame.getSize());
	}

	@SneakyThrows
	private @Nullable JPanel createImagePanel(JcefApplicationProperties applicationProperties, @Nullable BuildProperties buildProperties) {
		var path = applicationProperties.getSplashScreenClasspathResource();
		if (path == null) {
			return null;
		}
		var imgUrl = getClass().getClassLoader().getResource(path);
		if (imgUrl == null) {
			return null;
		}

		var splashImage = ImageIO.read(imgUrl);
		var panel = swingComponentFactory.createJPanel((p, g) -> g.drawImage(splashImage, 0, 0, p.getWidth(), p.getHeight(), p));
		panel.setLayout(new BorderLayout());
		var size = new Dimension(splashImage.getWidth(), splashImage.getHeight());
		panel.setMinimumSize(size);
		panel.setPreferredSize(size);

		if (buildProperties != null && isNotBlank(buildProperties.getVersion()) && buildProperties.getTime() != null) {
			var versionText = buildProperties.getVersion() + " (" + buildProperties.getTime() + ")";
			var versionLabel = swingComponentFactory.createJLabel(versionText);
			versionLabel.setForeground(Color.WHITE);

			var overlay = swingComponentFactory.createJPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
			overlay.setOpaque(true);
			overlay.setBackground(new Color(0, 0, 0, 120));
			overlay.add(versionLabel);

			panel.add(overlay, BorderLayout.SOUTH);
		}
		return panel;
	}

	/**
	 * 🔄 Updates the progress bar and state label based on the current installation state.
	 */
	@Override
	public void handleProgress(EnumProgress state, float percent) {
		swingExecutor.invokeLater(() -> {
			switch (state) {
				case DOWNLOADING -> {
					stateLabel.setText("Downloading...");
					if (percent >= 0) {
						progressBar.setIndeterminate(false);
						progressBar.setValue((int) percent);
					} else {
						progressBar.setIndeterminate(true);
					}
					frame.setVisible(true);
				}
				case EXTRACTING -> {
					stateLabel.setText("Extracting...");
					progressBar.setIndeterminate(true);
					frame.setVisible(true);
				}
				default -> frame.setVisible(false);
			}
		});
	}
}
