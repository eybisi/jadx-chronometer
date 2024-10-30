package jadx.plugins.example;

import java.awt.*;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.JadxGuiContext;

public class JadxChronometer implements JadxPlugin {
	public static final String PLUGIN_ID = "jadx-chronometer";
	private static final Logger LOG = LoggerFactory.getLogger(JadxChronometer.class);
	private final ChronometerOptions options = new ChronometerOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("jadx-chronometer")
				.description("Shows the time wasted jadxing for the current project")
				.homepage("https://github.com/eybisi/jadx-chronometer")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);

		if (options.isEnable()) {
			JadxGuiContext jgc = context.getGuiContext();
			if (jgc != null) {
				JFrame frame = jgc.getMainFrame();

				JPanel jMainPanel = getMainPanelReflectively(frame);

				// https://github.com/skylot/jadx/blob/a43b3282ef94255f611f66f3f8f0e68306f2cfb0/jadx-gui/src/main/java/jadx/gui/ui/MainWindow.java#L1241
				// 0 - mainPanel = new JPanel(new BorderLayout());
				// 1 - mainPanel.add(treeSplitPane);
				// 2 - mainPanel.add(toolbar, BorderLayout.NORTH);
				JToolBar secondComp = (JToolBar) jMainPanel.getComponent(2);
				// Prevent adding it multiple times, can happen if project gets reloaded.
				int count = secondComp.getComponentCount();
				if (count != 31) {
					return;
				}
				secondComp.addSeparator();
				// Create a JLabel to display the current time
				JLabel timeLabel = new JLabel();
				// TODO Maybe get font size from jadx settings?
				Font font = new Font("Arial", Font.BOLD, 12);
				timeLabel.setFont(font);
				secondComp.add(timeLabel); // Add the label to the toolbar

				Path cacheDir = getCacheDirReflectively(frame);
				Path timerFile = cacheDir.resolve("chronometer");
				Long wastedTime = getWastedTime(timerFile);
				// Subtract the wasted time from the current time
				final Long finalWastedTime = wastedTime;
				Date currTime = new Date();
				final Date subCurrTime = new Date(currTime.getTime() - wastedTime);

				// Timer to update the time every second
				javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
					// Get the current time
					Date currentTime = new Date();
					// Calculate the time difference
					long diff = currentTime.getTime() - subCurrTime.getTime();
					// Format the time difference
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
					sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
					String sCurrentTime = sdf.format(new Date(diff));

					// Update the label with the current time
					timeLabel.setText("Time wasted jadxing " + sCurrentTime);
				});
				timer.start();
				// Add a window listener to stop the timer when the window is closed
				WindowListener[] listeners = frame.getWindowListeners();
				frame.removeWindowListener(listeners[0]);
				frame.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent windowEvent) {
						timer.stop();
						Date currentTime = new Date();
						long diff = currentTime.getTime() - subCurrTime.getTime();
						long ff = finalWastedTime + diff;
						try {
							Files.writeString(timerFile, Long.toString(ff));
						} catch (IOException e) {
							LOG.error("Failed to write timer file", e);
						}
					}
				});
				frame.addWindowListener(listeners[0]);
			}
		} else {
			// LOG.info("Chronometer is disabled");
			JadxGuiContext jgc = context.getGuiContext();
			if (jgc != null) {
				JFrame frame = jgc.getMainFrame();
				JPanel jMainPanel = getMainPanelReflectively(frame);
				JToolBar secondComp = (JToolBar) jMainPanel.getComponent(2);
				removeChronometer(secondComp);
			}
		}
	}

	public Long getWastedTime(Path timerFile) {
		// Try to read chronometer file
		Long wastedTime = 0L;
		if (timerFile.toFile().exists()) {
			String content;
			try {
				content = Files.readString(timerFile);
				wastedTime = Long.parseLong(content);
			} catch (IOException e) {
				LOG.error("Failed to read timer file", e);
			}
		} else {
			try {
				// Create parent directories if they don't exist
				Files.createDirectories(timerFile.getParent());
				Files.createFile(timerFile);
			} catch (IOException e) {
				LOG.error("Failed to create timer file", e);
			}
			try {
				Files.writeString(timerFile, "0");
			} catch (IOException e) {
				LOG.error("Failed to write timer file", e);
			}
		}
		return wastedTime;
	}

	public void removeChronometer(JToolBar comp) {
		int count = comp.getComponentCount();
		if (count == 31) {
			// This is ok
			return;
		}
		if (count > 31) {
			// LOG.info("Removing chronometer");
			comp.remove(count - 1);
			comp.remove(count - 2);
		}
	}

	public JPanel getMainPanelReflectively(JFrame frame) {
		Object mainPanel = null;
		try {
			Field field = frame.getClass().getDeclaredField("mainPanel");
			field.setAccessible(true);
			mainPanel = field.get(frame);
		} catch (Exception e) {
			LOG.error("Failed to get mainPanel", e);
		}
		if (mainPanel == null) {
			return null;
		}
		JPanel jMainPanel = (JPanel) mainPanel;
		return jMainPanel;
	}

	public Path getCacheDirReflectively(JFrame mainWindow) {
		Object jadxProject = null;
		try {
			// Call getSettings() method on the main window
			jadxProject = mainWindow.getClass().getMethod("getProject").invoke(mainWindow);
		} catch (Exception e) {
			LOG.error("Failed to get jadx project", e);
		}
		if (jadxProject == null) {
			return null;
		}
		Path cacheDir = null;
		try {
			// call getCacheDir() method on the jadxProject object
			cacheDir = (Path) jadxProject.getClass().getMethod("getCacheDir").invoke(jadxProject);
		} catch (Exception e) {
			LOG.error("Failed to get cache dir", e);
		}
		return cacheDir;
	}

}
