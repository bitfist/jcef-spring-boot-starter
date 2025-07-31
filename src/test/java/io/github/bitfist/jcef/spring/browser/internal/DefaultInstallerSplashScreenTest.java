package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.application.JcefApplicationProperties;
import io.github.bitfist.jcef.spring.swing.SwingComponentFactory;
import io.github.bitfist.jcef.spring.swing.SwingExecutor;
import me.friwi.jcefmaven.EnumProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.info.BuildProperties;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.time.Instant;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultInstallerSplashScreenTest {

	@Mock
	private SwingComponentFactory swingComponentFactory;

	@Mock
	private SwingExecutor swingExecutor;

	@Mock
	private JcefApplicationProperties applicationProperties;

	@Mock
	private BuildProperties buildProperties;

	@Mock
	private Container contentPane;

	@Mock
	private JFrame frame;

	@Mock
	private JLabel stateLabel;

	@Mock
	private JProgressBar progressBar;

	@Mock
	private JPanel panel;

	private DefaultInstallerSplashScreen splashScreen;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws Exception {
		// Stub factory methods
		when(swingComponentFactory.createJFrame()).thenReturn(frame);
		when(swingComponentFactory.createJLabel(anyString())).thenReturn(stateLabel);
		when(swingComponentFactory.createJProgressBar(0, 100)).thenReturn(progressBar);
		when(swingComponentFactory.createJPanel((LayoutManager) any())).thenReturn(panel);
		when(swingComponentFactory.createJPanel((BiConsumer<JPanel, Graphics>) any())).thenReturn(panel);
		when(applicationProperties.getSplashScreenClasspathResource()).thenReturn("empty.png");

		when(buildProperties.getVersion()).thenReturn("1.0.0");
		when(buildProperties.getTime()).thenReturn(Instant.now());

		when(frame.getContentPane()).thenReturn(contentPane);

		splashScreen = new DefaultInstallerSplashScreen(swingComponentFactory, swingExecutor, applicationProperties, buildProperties);
	}

	@Test
	@DisplayName("🔄 DOWNLOADING with percent>=0 shows progress")
	void testHandleProgressDownloadingWithPercent() {
		splashScreen.handleProgress(EnumProgress.DOWNLOADING, 50f);

		var runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(swingExecutor).invokeLater(runnableArgumentCaptor.capture());
		runnableArgumentCaptor.getValue().run();

		verify(stateLabel).setText("Downloading...");
		verify(progressBar).setIndeterminate(false);
		verify(progressBar).setValue(50);
		verify(frame).setVisible(true);
	}

	@Test
	@DisplayName("🔄 DOWNLOADING with percent<0 shows indeterminate progress")
	void testHandleProgressDownloadingIndeterminate() {
		splashScreen.handleProgress(EnumProgress.DOWNLOADING, -1f);

		var runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(swingExecutor).invokeLater(runnableArgumentCaptor.capture());
		runnableArgumentCaptor.getValue().run();

		verify(stateLabel).setText("Downloading...");
		verify(progressBar).setIndeterminate(true);
		verify(frame).setVisible(true);
	}

	@Test
	@DisplayName("🔄 EXTRACTING sets indeterminate and shows progress")
	void testHandleProgressExtracting() {
		splashScreen.handleProgress(EnumProgress.EXTRACTING, 0f);

		var runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(swingExecutor).invokeLater(runnableArgumentCaptor.capture());
		runnableArgumentCaptor.getValue().run();

		verify(stateLabel).setText("Extracting...");
		verify(progressBar).setIndeterminate(true);
		verify(frame).setVisible(true);
	}
}
