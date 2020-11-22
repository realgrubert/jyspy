package jyspy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class History {
	private int idx = 0; // may == linez.size() for "nothing" state.
	private ArrayList<String> linez = new ArrayList<String>();
	
	public History(String historyPath) throws IOException{
		File f = new File(historyPath);
		if ( f.exists()){
			String line = null;
			LineNumberReader lnr = new LineNumberReader(new FileReader(historyPath));
			while (( line = lnr.readLine() ) != null ){
				linez.add(line);
			}
			lnr.close();
		}
		pw = new PrintWriter(new FileOutputStream(new File(historyPath),true));
	}
	
	public PrintWriter pw = null;
	
	public void move(boolean forward) {
		if (linez.size() == 0) {
			return;
		}
		idx += forward ? 1 : -1;// adjust index.
		idx = Math.min(Math.max(idx, 0), linez.size());
	}
	
	public String getLine(){
		if (idx == linez.size()) return null;
		return (String)linez.get(idx);
	}
	
	public void addLine(String line){
		if ( line == null || line.length() == 0 ) return;
		if ( linez.size()>0 && line.equals(linez.get(linez.size()-1))) return;  // don't store double entries twice. 
		if ( pw != null ){
			pw.println(line);
			pw.flush();
		}
		linez.add(line);
		idx = linez.size();
	}
	
	public void writeOut(PrintWriter pw){
		for ( int i = 0; i< linez.size(); i++){
			pw.println(linez.get(i));
		}
		
	}
}
