package bzb.se.ui;

import java.io.File;

import proxml.XMLElement;
import proxml.XMLInOut;
import bzb.se.bridge.Bridge;

public class Command {

	static Bridge br;
	static final int ROLE_SIGNAL = 0;
	static final int ROLE_BANDWIDTH = 1;
	static int role = ROLE_SIGNAL;
	static final String PROBE_MAC = "00:23:76:07:3b:ba";
	static String currentDevice;

	public static void main(String args[]) {
		br = new Bridge(args[0]);
		role = Integer.parseInt(args[1]);
	}

	static XMLInOut xmlIO;
	//static XMLElement configRoot;
	static final String configFileURL = "res/config.xml";

	static void updateConfigFile() {
		XMLElement configRoot = new XMLElement("config");
		configRoot.addAttribute("role", role);
		if (currentDevice != null) {
			configRoot.addAttribute("monitoring", currentDevice);
		}
		xmlIO.saveElement(configRoot, "../" + configFileURL);
		br.updateRole();
	}

	static void loadConfig() {
		File f = new File(configFileURL);
		if (!f.exists()) {
			updateConfigFile();
			xmlIO.loadElement(configFileURL);
		}
	}

	public void xmlEvent(XMLElement element) {
		if (element.getName().equals("config")) {
			if (element.getAttribute("role") != null) {
				role = Integer.parseInt(element.getAttribute("role"));
			}
			if (element.getAttribute("monitoring") != null) {
				currentDevice = element.getAttribute("monitoring");
			}
		}
	}

	public void chooseSignal(int value) {
		role = 0;
		currentDevice = PROBE_MAC;
		updateConfigFile();
	}

	public void chooseBandwidth(int value) {
		role = 1;
		currentDevice = null;
		updateConfigFile();
	}

	public void chooseExtraordinary(int value) {
		role = 2;
		currentDevice = null;
		updateConfigFile();
	}
}
