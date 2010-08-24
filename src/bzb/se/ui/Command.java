package bzb.se.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import bzb.se.bridge.Bridge;

import processing.core.PApplet;
import proxml.XMLElement;
import proxml.XMLInOut;

@SuppressWarnings("serial")
public class Command extends JPanel implements ActionListener {

	static Bridge br;
	static final int ROLE_SIGNAL = 0;
	static final int ROLE_BANDWIDTH = 1;
	static int role = ROLE_SIGNAL;
	static final String PROBE_MAC = "00:23:76:07:3b:ba";
	static String currentDevice;

	public static void main(String args[]) {
		br = new Bridge(args[0]);
		role = Integer.parseInt(args[1]);
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Homeworkduino control");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Command newContentPane = new Command();
                newContentPane.setOpaque(true); //content panes must be opaque
                frame.setContentPane(newContentPane);
                frame.pack();
                frame.setVisible(true);
            }
        });
	}
	
	public Command () {		
		JButton b1 = new JButton("Signal strength monitor");
        b1.setVerticalTextPosition(AbstractButton.CENTER);
        b1.setHorizontalTextPosition(AbstractButton.LEADING); //aka LEFT, for left-to-right locales
        b1.setMnemonic(KeyEvent.VK_S);
        b1.setActionCommand("chooseSignal");

        JButton b2 = new JButton("Bandwidth monitor");
        b2.setVerticalTextPosition(AbstractButton.CENTER);
        b2.setHorizontalTextPosition(AbstractButton.CENTER);
        b2.setMnemonic(KeyEvent.VK_B);
        b2.setActionCommand("chooseBandwidth");

        JButton b3 = new JButton("Network event monitor");
        b3.setMnemonic(KeyEvent.VK_E);
        b3.setActionCommand("chooseExtraordinary");

        b1.addActionListener(this);
        b2.addActionListener(this);
        b3.addActionListener(this);

        b1.setToolTipText("Click this button to monitor the strength of the connection between the probe and the network");
        b2.setToolTipText("Click this button to monitor the amount of traffic on the network");
        b3.setToolTipText("Click this button to monitor important events occurring on the network");

        //Add Components to this container, using the default FlowLayout.
        add(b1);
        add(b2);
        add(b3);
	}

	static void updateConfig() {
		if (currentDevice != null) {
			br.currentDevice = currentDevice;
		}
		br.updateRole(role);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("chooseSignal")) {
			role = 0;
			currentDevice = PROBE_MAC;
			updateConfig();
		} else if (ae.getActionCommand().equals("chooseBandwidth")) {
			role = 1;
			currentDevice = null;
			updateConfig();
		} else if (ae.getActionCommand().equals("chooseExtraordinary")) {
			role = 2;
			currentDevice = null;
			updateConfig();
		}
	}
}
