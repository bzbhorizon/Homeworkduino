package bzb.se.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import bzb.se.bridge.Bridge;

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
                newContentPane.setLayout(new GridLayout(2, 3));
                frame.setContentPane(newContentPane);
                frame.setMaximumSize(new Dimension());
                frame.pack();
                frame.setVisible(true);
            }
        });
	}

	private JToggleButton b1;
	private JToggleButton b2;
	private JToggleButton b3;
	
	public Command () {		
		b1 = new JToggleButton("Signal strength monitor");
        b1.setVerticalTextPosition(AbstractButton.CENTER);
        b1.setHorizontalTextPosition(AbstractButton.CENTER);
        b1.setMnemonic(KeyEvent.VK_S);
        b1.setActionCommand("chooseSignal");

        b2 = new JToggleButton("Bandwidth monitor");
        b2.setVerticalTextPosition(AbstractButton.CENTER);
        b2.setHorizontalTextPosition(AbstractButton.CENTER);
        b2.setMnemonic(KeyEvent.VK_B);
        b2.setActionCommand("chooseBandwidth");

        b3 = new JToggleButton("Network event monitor");
        b3.setVerticalTextPosition(AbstractButton.CENTER);
        b3.setHorizontalTextPosition(AbstractButton.CENTER);
        b3.setMnemonic(KeyEvent.VK_E);
        b3.setActionCommand("chooseExtraordinary");

        b1.addActionListener(this);
        b2.addActionListener(this);
        b3.addActionListener(this);

        b1.setToolTipText("Click this button to monitor the strength of the connection between the probe and the network");
        b2.setToolTipText("Click this button to monitor the amount of traffic on the network");
        b3.setToolTipText("Click this button to monitor important events occurring on the network");

        add(new JLabel(new ImageIcon("res/strength.png")));
        add(new JLabel(new ImageIcon("res/bandwidth.png")));
        add(new JLabel(new ImageIcon("res/event.png")));
        add(b1);
        add(b2);
        add(b3);
        
        ButtonGroup bg = new ButtonGroup();
        bg.add(b1);
        bg.add(b2);
        bg.add(b3);
        
        b2.setSelected(true);
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
