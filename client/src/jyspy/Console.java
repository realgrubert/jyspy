package jyspy;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TextAction;

public class Console {
	private static final Color COLOR_STDERR = Color.red;
	private static final Color COLOR_STDOUT = Color.blue;
	private static final Color COLOR_ZONE = Color.black;
	private static final Color COLOR_BACK = Color.white;
	
	private static SimpleAttributeSet SAS_ZONE = new SimpleAttributeSet();
	private static SimpleAttributeSet SAS_STDOUT = new SimpleAttributeSet();
	private static SimpleAttributeSet SAS_STDERR = new SimpleAttributeSet();
	
	static {
		SAS_ZONE.addAttribute(StyleConstants.Foreground, COLOR_ZONE);	
		SAS_STDOUT.addAttribute(StyleConstants.Foreground, COLOR_STDOUT);	
		SAS_STDERR.addAttribute(StyleConstants.Foreground, COLOR_STDERR);	
	}
	
	private static final Font FONT = new Font("Monospaced", Font.PLAIN, 11);
	
	private InterruptHandler interruptHandler;
	private ZoneEnterHandler zoneSubmitHandler;
	
	private PrintStream stdStream;
	private PrintStream errStream;
	
	private JTextPane tpane = new JTextPane();
	private Document doc;
	private String prompt = "";
	
	private String zoneText;
	private int zoneDocIdx = 0;
	
	
	public Console(String prompt) throws Exception {
		if ( prompt == null || prompt.length() == 0){
			throw new IllegalArgumentException("prompt must be a non-empty string.");
		}
		this.prompt = prompt;
		
		stdStream = new PrintStream(new OutputStream() {
			public void write(int b) throws IOException {
				try {
					doc.insertString(doc.getLength(), String.valueOf((char) b), SAS_STDOUT);
				} catch (BadLocationException e) { e.printStackTrace(); }
			}
		});
		
		errStream = new PrintStream(new OutputStream() {
			public void write(int b) throws IOException {
				try {
					doc.insertString(doc.getLength(), String.valueOf((char) b), SAS_STDERR);
				} catch (BadLocationException e) { e.printStackTrace(); }
			}
		});
		
		tpane.addKeyListener(new KeyAdapter() {
			
			public void keyTyped(KeyEvent e) {
				if (tpane.getCaretPosition() < zoneDocIdx) {
					zoneFocus(); 
					e.consume();
				}
			}
		
			public void keyPressed(KeyEvent e) {
				// if not in zone, don't allow backspace
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					if (tpane.getCaretPosition() <= zoneDocIdx) {
						e.consume();
						return;
					}
				}
				// if in zone, go to start of edit zone, *after* the prompt.
				if (e.getKeyCode() == KeyEvent.VK_HOME){
					
					if (tpane.getCaretPosition() >= zoneDocIdx) {
						tpane.setCaretPosition(zoneDocIdx);
						e.consume();
						return;
					}
					
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					// if the caret is outside the zone, go to zone, return.
					if (tpane.getCaretPosition() < zoneDocIdx) {
						zoneFocus();
						e.consume();
						return;
					}
					// if the caret is not at end of zone, move it there to avoid inserting CR/LF in zone text.
					if ( tpane.getCaretPosition() < doc.getLength()){
						tpane.setCaretPosition(doc.getLength()); 
					}
					// grab zone text.
					try {
							zoneText = doc.getText(zoneDocIdx, doc.getLength()
									- zoneDocIdx);
						} catch (BadLocationException e1) {
							e1.printStackTrace();
							zoneText = "";
						}
					}
				
				if (e.getKeyCode() == KeyEvent.VK_F12 ){
					zoneReset();
				}
			}
		});
		
		Keymap keymap = JTextComponent.addKeymap("console", tpane.getKeymap());
		String os_name = System.getProperty("os.name");
		
		int interrupt_key = os_name.matches("^Win.*") ? KeyEvent.VK_PAUSE
				: KeyEvent.VK_C; // BREAK
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(
				(char) KeyEvent.VK_ENTER), (new TextAction("console.enter"){
			public void actionPerformed(ActionEvent e) {
				if (zoneSubmitHandler != null) {
					zoneSubmitHandler.handle(zoneText);
					//zoneReset();  // GIR - remove to eliminate double-prompt bug. called in the main socket loop. 
				}
				// try this to preserve the zone if enter pressed somewhere before the last position.
				tpane.setCaretPosition(doc.getLength());
				
			}
		}));
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(
				(char) interrupt_key, InputEvent.CTRL_MASK), (new TextAction("console.keyboardInterrupt"){
			public void actionPerformed(ActionEvent event) {
				if (interruptHandler != null) {
					interruptHandler.handle();
				}
			}
		}));
		
		tpane.setKeymap(keymap);
		
		
		tpane.setForeground(COLOR_ZONE);  // will be used whenever a keystroke can actually write, i.e.: in the zone.
		tpane.setBackground(COLOR_BACK);
		tpane.setFont(FONT);
		tpane.requestFocus();
		doc = tpane.getDocument();
	}
	
	/* ==================== HANDLE THIS ========================================= */
	public interface InterruptHandler {
		public void handle();
	}
	
	public void setInterruptHandler(InterruptHandler handler) {
		this.interruptHandler = handler;
	}

	/* ------------------------------------------------- */

	public interface ZoneEnterHandler {
		/**
		 * should return only after response is sent to our stream(s).
		 * @param the stuff.
		 */
		public void handle(String line);
	}
	
	public void setEnterHandler(ZoneEnterHandler zsHandler) {
		this.zoneSubmitHandler = zsHandler;
	}
	
	/* ====================================================================== */
	
	public JTextPane getTextPane() {
		return this.tpane;
	}

	public PrintStream getStandardStream() {
		return this.stdStream;
	}

	public PrintStream getErrorStream() {
		return this.errStream;
	}
	/* ======= Editing Zone =========================================================== */
	
	/**
	 *  move to new line
	 *  write the prompt
	 *  set the zoneDocIdx to the end of prompt.
	 */
	public void zoneReset() {
		try {
			if (! doc.getText(doc.getLength()-1, 1).equals("\n")) {
				doc.insertString(doc.getLength(), "\n", SAS_ZONE);
			}
			doc.insertString(doc.getLength(), prompt + " ", SAS_ZONE);	
			tpane.setCaretPosition(doc.getLength());
			zoneDocIdx = doc.getLength();
			zoneFocus();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	/**
	 * set the Prompt for the Edit Zone.
	 * @param prompt
	 */
	public void zonePrompt(String prompt){
		if ( prompt == null || prompt.length() == 0){
			throw new IllegalArgumentException("prompt must be a non-emtpy string.");
		}
		this.prompt = prompt;
	}
	/**
	 * overwrite the text in the current edit zone.
	 * do not use async with the stream
	 *  * @param zone contents.
	 */
	public void zoneSet(String newZone) {
		newZone = newZone==null?"":newZone;
		try {
		if ( zoneDocIdx < doc.getLength()){
			doc.remove(zoneDocIdx, doc.getLength()-zoneDocIdx);
		}
		
		doc.insertString(zoneDocIdx, newZone, null);
		zoneFocus();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	/**
	 * put the cursor at the end of the Edit Zone
	 */
	public void zoneFocus(){
		final Console me = this;
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				me.tpane.setCaretPosition(doc.getLength());
			}
		});
	}
	
	
}
