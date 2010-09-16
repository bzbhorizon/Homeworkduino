/**
 * 
 */
package bzb.se;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author bzb
 *
 */
public abstract class Utility {

	public static int toBinary (int ledsLit) {
		return (int) (Math.pow(2, ledsLit) - 1);
	}
	
	public static int toBinary (int ledsLitFrom, int ledsLit) {
		int binary = 0;
		for (int j = ledsLitFrom + 1; j <= ledsLitFrom + ledsLit; j++) {
			binary += (int) (Math.pow(2, j - 1));
		}
		return binary;
	}
	
	public static void writeToLog (String line) {
		try {
		    BufferedWriter out = new BufferedWriter(new FileWriter("res/output.log"));
		    out.write(System.currentTimeMillis() + "," + new SimpleDateFormat("kk:mm:ss dd/MM/yyyy").format(Calendar.getInstance().getTime()) + "," + line + "\r\n");
		    out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
