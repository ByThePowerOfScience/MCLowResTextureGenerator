package btpos.tools.mclowrespackgenerator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.EventQueue;

public interface UserInterfaceHandler {
	static UserInterfaceHandler get(boolean isHeadless) {
		try {
			if (System.getProperty("os.name").toLowerCase().contains("windows"))
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}
		
		if (isHeadless)
			return new Headless();
		else
			return new Graphical();
	}
	
	default void onStart() {}
	
	void onFinish(String message);
	
	final class Graphical implements UserInterfaceHandler {
		private JFrame consoleFrame;
		
		@Override
		public void onStart() {
			consoleFrame = UIGenerators.enableConsolePanel();
			
			EventQueue.invokeLater(() -> consoleFrame.setVisible(true));
		}
		
		@Override
		public void onFinish(String message) {
			EventQueue.invokeAndWait(() -> JOptionPane.showMessageDialog(null, message));
			consoleFrame.dispose();
		}
	}
	
	final class Headless implements UserInterfaceHandler {
		@Override
		public void onFinish(String message) {
			System.out.println(message);
		}
	}
}
