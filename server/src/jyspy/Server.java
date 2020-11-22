package jyspy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

import org.python.util.InteractiveConsole;
import org.python.util.InteractiveInterpreter;
import org.python.util.PythonInterpreter;

public class Server implements Runnable {
	// Interpreter return values.
	private static final char IR = (char)0x11; // DC1  
	private static final char IRM = (char)0x12; // DC2 

	// ===============================================================
	private static Map<String,Object> pythonVariables = null;
	private static int     serverPort = 2000;
	private static boolean isDebug = false;
	private static String startupMessage  = null;
	private static String connectMessage  = null;
	private static String shutdownMessage = null;

	private Server() {}

	public void run() {
		ServerSocket socket = null;

		try {
			socket = new ServerSocket(serverPort);
			final Socket sock = socket.accept();

			if ( connectMessage != null ) {
				System.err.println(connectMessage + sock.getInetAddress());	
				System.err.flush();
			}

			Properties overrideProps = new Properties();
			overrideProps.setProperty("python.security.respectJavaAccessibility", "false");

			PythonInterpreter.initialize(System.getProperties(), overrideProps, new String[] { "" });
			InteractiveInterpreter interpret = new InteractiveInterpreter();

			if ( pythonVariables != null ){
				for (String k : pythonVariables.keySet()){
					interpret.set(k, pythonVariables.get(k));
				}
			}

			DataOutputStream toClient = new DataOutputStream(sock.getOutputStream());
			toClient.writeBytes(InteractiveConsole.getDefaultBanner());
			toClient.write((int)IR ); 
			toClient.flush();
			interpret.setOut(toClient);
			interpret.setErr(toClient);

			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

			final StringBuffer buffer = new StringBuffer(); // to support multi line commands.

			while (sock.isConnected()) {
				String line = null;
				try {
					line = in.readLine();
				} catch (java.net.SocketException se){
					break;
				}
				if ( line == null ){
					break;
				}
				if ( line.length() == 0 ){  // blank line? new prompt!
					toClient.write((int) IR );
					toClient.flush();
					continue;
				}

				if ( isDebug ) System.err.println("----- incoming Last:" + (int)line.substring(line.length()-1).toCharArray()[0]);

				buffer.append(line + "\n"); 
				String interpretMe = buffer.toString();
				boolean more = interpret.runsource(interpretMe);
				if ( more ){
					toClient.write((int)IRM );
				} else {
					buffer.delete(0,buffer.length()-1);
					toClient.write((int) IR );
				}
				toClient.flush();
			}

			if ( shutdownMessage != null ) {
				System.err.println(shutdownMessage);
				System.err.flush();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if ( socket != null && !socket.isClosed() ) {
				try {
					socket.close();
				} catch (IOException e) {
					if ( isDebug ) e.printStackTrace();
				}
			}
		}
	}


	public static Thread activate(
			Integer serverPort,
			Map<String,Object> pyVars, 
			String pythonHome, 
			String pythonPath, 
			String threadName, 
			String startupMessage, 
			String connectMessage, 
			String shutdownMessage,  
			Boolean debug){

		Server.pythonVariables = pyVars;
		Server.connectMessage = connectMessage;
		Server.shutdownMessage = shutdownMessage;

		if ( serverPort != null ) {
			Server.serverPort = serverPort.intValue();
		}

		if ( debug != null ) {
			isDebug = debug.booleanValue();
		}

		if ( pythonHome != null ) {
			if (! (new File(pythonHome)).exists() )
				throw new RuntimeException("pythonHome " + pythonHome + " not found.");
			System.getProperties().put("python.home", pythonHome );
		}

		if ( pythonPath != null ){
			if (! (new File(pythonPath)).exists() )
				throw new RuntimeException("pythonPath " + pythonPath + " not found.");
			System.getProperties().put("python.path",pythonPath);
		}

		Server serv = new Server();

		Thread jyspyThread = new Thread(serv);
		if ( threadName != null ) {
			jyspyThread.setName(threadName);
		}

		jyspyThread.start();

		if (startupMessage != null) {
			System.out.println(startupMessage);
			System.out.flush();
		}

		return jyspyThread;
	}


	/**
	 * for testing only  
	 */
	public static void main(String[] args) throws Exception {

		Thread sthread = activate(null, null, null, null, 
				"JySpy Thread",
				"JySpy Server Running",
				"JySpy connected ",
				"JySpy Shutdown",
				true);

		try {
			while (sthread.isAlive()){
				Thread.sleep(1600);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
