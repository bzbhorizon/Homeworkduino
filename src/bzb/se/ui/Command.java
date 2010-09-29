package bzb.se.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JPanel;

import bzb.se.bridge.Bridge;

@SuppressWarnings("serial")
public class Command extends JPanel {

	static Bridge br;

	public static void main(String args[]) {
		br = new Bridge(args[0]);
		new Thread(new Runnable() {
			private BufferedReader buf;

			public void run () {
				while (true) {
					try {
						buf = new BufferedReader(new FileReader(new File("res/role.cfg")));
						br.updateRole(Integer.parseInt(buf.readLine()));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}
