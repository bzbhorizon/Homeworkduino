/**
 * 
 */
package bzb.se.ui;

import java.awt.Color;
import java.io.File;
import java.util.Iterator;

import processing.core.PApplet;
import proxml.XMLElement;
import proxml.XMLInOut;
import bzb.se.bridge.Bridge;
import bzb.se.bridge.Link;
import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlGroup;
import controlP5.ControlP5;
import controlP5.Label;
import controlP5.Slider;
import controlP5.Textlabel;


/**
 * @author bzb
 *
 */
@SuppressWarnings("serial")
public class Control extends PApplet {

	static Bridge br;
	
	public static void main(String args[]) {
		br = new Bridge(Integer.parseInt(args[0]));
		
		String className = Control.class.getName();
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
		frameRate(10);

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
			
			if (i == role) {
				roleGroups[i].open();
				updateGroup();
				roleGroups[i].setColorBackground(150);
				roleGroups[i].setBackgroundHeight(50);
			} else {
				roleGroups[i].close();
				roleGroups[i].setColorBackground(50);
				roleGroups[i].setBackgroundHeight(50);
			}
			roleGroups[i].activateEvent(true);
		}
		
		background(myColorBackground);
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
		br.updateRole();
	}
		
	static String getRoleText () {
		return "When set to the '" + roles[role] + "' role the probe " + roleTexts[role];
	}
	
	Bang lastBang;
	
	public void controlEvent(ControlEvent theEvent) {
		if(theEvent.isGroup()) {
			if (theEvent.group().isOpen()) {
				for (int i = 0; i < roleGroups.length; i++) {
					if (theEvent.group().name().equals(roleGroups[i].name())) {
						role = i;
						roleLabel.setValue(getRoleText());
						updateLabel.set("Set probe to " + roles[role]);
						update.show();
						updateGroup();
					} else {
						roleGroups[i].close();
					}
				}
			} else {
				theEvent.group().open();
			}
		} else if (theEvent.isController()) {
			if (theEvent.name().startsWith("m")) {
				if (lastBang != null) {
					lastBang.setColorForeground(Color.GRAY.getRGB());
				}
				lastBang = (Bang) theEvent.controller();
				lastBang.setColorForeground(Color.RED.getRGB());
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
	
	boolean updating = false;
	Slider progressBar;
	float progress = 0;
	
	public void updateGroup() {
		//new Thread(new Runnable() {
			//public void run () {
		new Thread(new Runnable() {
			public void run () {
				progressBar = controlP5.addSlider("progress",0,100,progress,screen.width / 2 - 100,screen.height / 2 - 10,200,10);
				progressBar.setCaptionLabel("Please wait ...");
				while (progress < 100) {
					progressBar.setValue(progress);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				controlP5.remove("progress");
			}
		}).start();

		new Thread(new Runnable() {
			public void run () {
				switch (role) {
					case 0:
						Iterator<String> macs = br.feed.getDevices().keySet().iterator();
						int num = br.feed.getDevices().size();
						int i = 0;
						while (macs.hasNext()) {
							progress = (float) ((double)i / (double)num * 100);
							
							String mac = macs.next();
							if (mac != null) {
								controlP5.remove("m" + mac);
								Link thisDevice = br.feed.getDevices().get(mac).link;
								if (thisDevice != null) {
									String bangText = mac + " " + thisDevice.getCorporation();
									int brightness = (int) (0-thisDevice.getRssi()*255/120.0);
									if (controlP5 != null) {
										Bang b = controlP5.addBang("m" + mac, 0, 4 + i++ * 30, 40, 10);
										if (b != null) {
											b.setGroup(roleGroups[role]);
											b.setColorActive(Color.WHITE.getRGB());
											b.setColorForeground(new Color(brightness,brightness,brightness).getRGB());
											b.setCaptionLabel(bangText);
										}
									} else {
										System.out.println("bleh");
									}
								} else {
									System.out.println("bleh");
								}
							} else {
								System.out.println("bleh");
							}
						}
						break;
					case 1:
						break;
					case 2:
						break;
					default:
						break;
				}
				progress = 100;
				controlP5.remove("progress");
			}
		}).start();
	}
}
