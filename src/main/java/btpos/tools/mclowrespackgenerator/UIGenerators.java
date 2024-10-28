package btpos.tools.mclowrespackgenerator;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class UIGenerators {
	static JFrame getConsolePanel() {
		JFrame jFrame = new JFrame();
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JTextArea jTextArea = new JTextArea();
		JScrollPane jsp = new JScrollPane(jTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jsp.getVerticalScrollBar().addAdjustmentListener(e -> jTextArea.select(jTextArea.getHeight() + 1000, 0));
		jTextArea.setEditable(false);
		
		TextAreaOutputStream tex = new TextAreaOutputStream(jTextArea);
		
		PrintStream splitConsoleStream = new PrintStream(new OutputStream() {
			final OutputStream sout = System.out;
			
			@Override
			public void write(int b) throws IOException {
				tex.write(b);
				sout.write(b);
			}
		});
		
		System.setOut(splitConsoleStream);
		System.setErr(splitConsoleStream);
		
		jFrame.setSize(500, 300);
		jFrame.add(jTextArea);
		jFrame.setLocationRelativeTo(null);
		
		return jFrame;
	}
}
