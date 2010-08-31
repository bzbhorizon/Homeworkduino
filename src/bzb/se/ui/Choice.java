package bzb.se.ui;

import java.io.File;

import controlP5.Bang;
import controlP5.ControlP5;
import processing.core.PApplet;
import proxml.XMLElement;
import proxml.XMLInOut;
import bzb.se.bridge.Bridge;

@SuppressWarnings("serial")
public class Choice extends PApplet {

	static Bridge br;
	static final int ROLE_SIGNAL = 0;
	static final int ROLE_BANDWIDTH = 1;
	static int role = ROLE_SIGNAL;
	static final String PROBE_MAC = "00:04:20:1b:7d:76";
	static String currentDevice;

	public static void main(String args[]) {
		br = new Bridge(args[0]);

		String className = Choice.class.getName();
		PApplet.main(new String[] { "--present", className });
	}

	private static ControlP5 controlP5;
	private static Bang[] roleButtons;
	private static final int padding = 100;

	public void setup() {
		size(screen.width, screen.height);
		background(color(0, 0, 0));
		

		xmlIO = new XMLInOut(this);
		loadConfig();

		controlP5 = new ControlP5(this);

		roleButtons = new Bang[3];

		roleButtons[0] = controlP5.addBang("chooseSignal", padding,
				(screen.height - padding * 2) / 3, 20, 20);
		roleButtons[0].setLabel("Signal strength probe");

		roleButtons[1] = controlP5.addBang("chooseBandwidth", padding,
				(screen.height - padding * 2) / 3 * 2, 20, 20);
		roleButtons[1].setLabel("Bandwidth alarm");

		roleButtons[2] = controlP5.addBang("chooseExtraordinary", padding,
				screen.height - padding * 2, 20, 20);
		roleButtons[2].setLabel("Something extraordinary");
	}

	public void draw() {
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
		br.updateRole(role);
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
