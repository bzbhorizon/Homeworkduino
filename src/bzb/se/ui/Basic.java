/**
 * 
 */
package bzb.se.ui;

import java.awt.Color;
import java.io.File;

import bzb.se.bridge.Bridge;

import processing.core.PApplet;
import proxml.XMLElement;
import proxml.XMLInOut;
import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlGroup;
import controlP5.ControlP5;
import controlP5.Label;
import controlP5.Textlabel;


/**
 * @author bzb
 *
 */
@SuppressWarnings("serial")
public class Basic extends PApplet {

	static Bridge b;
	
	public static void main(String args[]) {
		b = new Bridge(Integer.parseInt(args[0]));
		
		String className = Basic.class.getName();
	    PApplet.main(new String[] { "--present", className });
	}
	
	ControlP5 controlP5;
	int myColorBackground = color(0,0,0);
	
	static int role = 0;
	static final String[] roles = new String[]{
		"Signal","Network","Device"
	};
	ControlGroup[] roleGroups = new ControlGroup[roles.length];
	static final String[] roleTexts = new String[]{
		"can be carried around the home to see how the strength of the wireless network signal varies from place to place",
		"shows somthing about the network",
		"shows something about a particular device on the network"
	};
	Textlabel roleLabel;
	Label updateLabel;
	Bang update;
	
	static final String configFileURL = "res/config.xml";
	static XMLInOut xmlIO;
	static XMLElement configRoot;
	
	public void setup() {
		size(screen.width, screen.height);
		smooth();
		controlP5 = new ControlP5(this);
		
		xmlIO = new XMLInOut(this);
		loadConfig();
		
		update = controlP5.addBang("update", screen.width / 2 - 50, screen.height - 100, 100, 30);
		updateLabel = update.captionLabel();
		update.hide();
		
		Bang exit = controlP5.addBang("exit", screen.width - 130, 100, 30, 30);
		exit.setColorForeground(Color.RED.getRGB());
		exit.setColorActive(Color.BLACK.getRGB());
		
		controlP5.addTextlabel("title", "This interface controls the Homework probe, allowing the user to change the characteristics of the network that the probe measures", 100, 100);
		
		roleLabel = controlP5.addTextlabel("role", getRoleText(), 100, 120);
		
		for (int i = 0; i < roles.length; i++) {
			roleGroups[i] = controlP5.addGroup(roles[i], 100 + i * 200, 200);
			roleGroups[i].setBarHeight(20);
			controlP5.addBang(i + "1", 0, 4, 20, 20).setGroup(roleGroups[i]);
			controlP5.addBang(i + "2", 30, 4, 20, 20).setGroup(roleGroups[i]);
			if (i == role) {
				roleGroups[i].open();
				roleGroups[i].setColorBackground(150);
				roleGroups[i].setBackgroundHeight(50);
			} else {
				roleGroups[i].close();
				roleGroups[i].setColorBackground(50);
				roleGroups[i].setBackgroundHeight(50);
			}
			roleGroups[i].activateEvent(true);
		}
	}
	
	static void updateConfigFile () {
		configRoot.addAttribute("role", role);
		xmlIO.saveElement(configRoot, "../" + configFileURL);
	}
	
	static void loadConfig() {
		File f = new File(configFileURL);
		if (!f.exists()) {
			configRoot = new XMLElement("config");
			updateConfigFile();
		}
		xmlIO.loadElement(configFileURL);
	}
	
	public void xmlEvent(XMLElement element){
		if (element.getName().equals("config")) {
			configRoot = element;
			role = Integer.parseInt(configRoot.getAttribute("role"));
		}
	}
	
	public void update (int value) {
		update.hide();
		updateConfigFile();
		for (int i = 0; i < roleGroups.length; i++) {
			if (i == role) {
				roleGroups[i].setColorBackground(150);
				roleGroups[i].setBackgroundHeight(50);
			} else {
				roleGroups[i].setColorBackground(50);
				roleGroups[i].setBackgroundHeight(50);
			}
		}
		b.updateRole();
	}
		
	static String getRoleText () {
		return "When set to the '" + roles[role] + "' role the probe " + roleTexts[role];
	}
	
	public void controlEvent(ControlEvent theEvent) {
		if(theEvent.isGroup()) {
			if (theEvent.group().isOpen()) {
				for (int i = 0; i < roleGroups.length; i++) {
					if (theEvent.group().name().equals(roleGroups[i].name())) {
						role = i;
						roleLabel.setValue(getRoleText());
						updateLabel.set("Set probe to " + roles[role]);
						update.show();
					} else {
						roleGroups[i].close();
					}
				}
			} else {
				theEvent.group().open();
			}
		}
	}
	
	public void draw() {
		background(myColorBackground);
	}
	
	public void exit (int value) {
		exit();
	}
	
	public void keyPressed() {
		if (key == ESC) {
			key = 0;  // Fools! don't let them escape!
		}
	}

}
