package bzb.se.ui;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JPanel;

import bzb.se.bridge.Bridge;

@SuppressWarnings("serial")
public class Command extends JPanel {

	static Bridge br;

	public static void main(String args[]) {
		br = new Bridge(args[0]);
		new Thread(new Runnable() {
			public void run () {
				while (true) {
					try {
						br.updateRole(Integer.parseInt(new DataInputStream(new BufferedInputStream(new FileInputStream(new File("res/role.cfg")))).readUTF()));
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
