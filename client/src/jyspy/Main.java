package jyspy;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

public class Main implements Runnable
{
	static int PORT = 8125; 
	static String HOST = "localhost"; // name or IP address of the target machine
	// history also useful to copy/paste commands.. if not the output :)
	static String HISTORY_FILE = "./jyspy.history"; 
	
	static String PROMPT = ">>>";
	static String PROCESS = "...";
	
	// Interpreter return values.
	static final char IR = (char)0x11; // DC1  
	static final char IRM = (char)0x12; // DC2 

	public static void activate() {
		(new Thread(new Main())).start();
	}

	
	public void run() {
		try {

			final Socket sock = new Socket(HOST, PORT);

			JFrame frame = new JFrame();
			frame.setTitle("JySpy Client connected to " + sock.getInetAddress());
			frame.setSize(600, 400);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			final Console console = new Console(PROMPT);
			
			frame.getContentPane().add(new JScrollPane(console.getTextPane()));
			final PrintWriter pwcon = new PrintWriter(console.getStandardStream());
			final History history = new History(HISTORY_FILE);
			
			// Send line to Server
			final PrintWriter writer = new PrintWriter(sock.getOutputStream());
			console.setEnterHandler(new Console.ZoneEnterHandler() {
				public void handle(String line) {
					history.addLine(line);
					writer.println(line);
					writer.flush();
				}
			});

			Keymap keymap = console.getTextPane().getKeymap();
			keymap.addActionForKeyStroke(KeyStroke.getKeyStroke((char) KeyEvent.VK_UP, 0),
					(new TextAction("console.up") {
						public void actionPerformed(ActionEvent event) {
							history.move(false);
							console.zoneSet(history.getLine());

						}
					}));

			keymap.addActionForKeyStroke(KeyStroke.getKeyStroke((char) KeyEvent.VK_DOWN, 0), (new TextAction(
					"console.down") {
				public void actionPerformed(ActionEvent event) {
					history.move(true);
					console.zoneSet(history.getLine());
				}
			}));
			frame.setVisible(true);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
                    sock.getInputStream()));

			while (sock.isConnected()) {
				int c = in.read();
				if ( c == -1 ){
					System.err.println("End of Stream"); // not expected.
					break;
				}
				switch (c)
				{
				case (int)IR:
					console.zonePrompt(PROMPT);	
					console.zoneReset();
					break;
				case ((int)IRM):
					console.zonePrompt(PROCESS);
					console.zoneReset();
					break;
				default:
					pwcon.write(c);
				    pwcon.flush();
				}
			}
			
		} catch (Exception e) {
			System.err.println("Unable to start JySpy");
			e.printStackTrace();
			System.err.flush();
		}
		System.exit(-1);
	}

	public static void main(String[] args) throws Exception {
		(new Main()).run();
	}

}
