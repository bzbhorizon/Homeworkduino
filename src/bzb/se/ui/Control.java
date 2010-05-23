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
import controlP5.Slider;
import controlP5.Textlabel;


/**
 * @author bzb
 *
 */
@SuppressWarnings("serial")
public class Control extends PApplet {

	static Bridge br;
	static final String PROBE_MAC = "0021d8e06ca2";
	static String currentDevice;
	
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
	Bang update;
	int[] groupXPositions = new int[roles.length];
	
	static final String configFileURL = "res/config.xml";
	static XMLInOut xmlIO;
	static XMLElement configRoot;
	
	public void setup() {
		size(screen.width, screen.height);
		frameRate(25);

		controlP5 = new ControlP5(this);
		
		xmlIO = new XMLInOut(this);
		loadConfig();
		
		update = controlP5.addBang("update", screen.width / 2 - 50, screen.height - 100, 100, 30);
		update.hide();
		
		Bang exit = controlP5.addBang("exit", screen.width - 130, 100, 30, 30);
		exit.setColorForeground(Color.RED.getRGB());
		exit.setColorActive(Color.BLACK.getRGB());
		
		controlP5.addTextlabel("title", "This interface controls the Homework probe, allowing the user to change the characteristics of the network that the probe measures", 100, 100);
		
		roleLabel = controlP5.addTextlabel("role", getRoleText(), 100, 120);
		
		for (int i = 0; i < roles.length; i++) {
			groupXPositions[i] = 100 + i * (int)((double)(screen.width - 200) / 3.0);
			roleGroups[i] = controlP5.addGroup(roles[i], groupXPositions[i], 200, (int)((double)(screen.width - 200) / 3.0 * 0.8));
			roleGroups[i].setBarHeight(20);
			
			if (i == role) {
				roleGroups[i].open();
				roleGroups[i].setColorBackground(60);
				roleGroups[i].setLabel(roles[i] + " (active)");
				updateGroup();
			} else {
				roleGroups[i].close();
				roleGroups[i].setColorBackground(30);
			}
			roleGroups[i].activateEvent(true);
			
			
		}
		
		background(myColorBackground);
	}
	
	static void updateConfigFile () {
		configRoot.addAttribute("role", role);
		if (currentDevice != null) {
			configRoot.addAttribute("monitoring", currentDevice);
		}
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
		if (role == 0) {
			currentDevice = PROBE_MAC;
		} else if (role == 1) {
			currentDevice = new String();
		} else if (role == 2) {	
			if (possCurrentDevice != null) {
				currentDevice = possCurrentDevice;
			}
		}
		updateConfigFile();
		for (int i = 0; i < roleGroups.length; i++) {
			if (i == role) {
				roleGroups[i].setColorBackground(60);
				roleGroups[i].setLabel(roles[i] + " (active)");
			} else {
				roleGroups[i].setColorBackground(30);
				roleGroups[i].setLabel(roles[i]);
			}
		}
		br.updateRole();
		if (role == 2) {
			br.updateLinks();
			updateGroup();
		}
	}
		
	static String getRoleText () {
		return "When set to the '" + roles[role] + "' role the probe " + roleTexts[role];
	}
	
	Bang lastBang;
	String possCurrentDevice;
	
	public void controlEvent(ControlEvent theEvent) {
		if(theEvent.isGroup()) {
			if (theEvent.group().isOpen()) {
				for (int i = 0; i < roleGroups.length; i++) {
					if (theEvent.group().name().equals(roleGroups[i].name())) {
						role = i;
						roleLabel.setValue(getRoleText());
						
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
				possCurrentDevice = theEvent.name().substring(1);
			}
		}
	}
	
	public void draw() {
		try {
			background(myColorBackground);
		} catch (Exception e) {
			
		}
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
				int pos = 0;
				update.hide();
				
				switch (role) {
					case 2:
						Iterator<String> macs = br.feed.getDevices().keySet().iterator();
						int num = br.feed.getDevices().size();
						int i = 0;
						while (macs.hasNext()) {
							progress = (float) ((double)i / (double)num * 100);
							
							try {
								String mac = macs.next();
								if (mac != null) {
									controlP5.remove("m" + mac);
									if (i < 12) {
										Link thisDevice = br.feed.getDevices().get(mac).link;
										if (thisDevice != null) {
											String bangText = thisDevice.getCorporation();
											if (mac.equals(currentDevice)) {
												int signal = (int)(0 - thisDevice.getRssi() / Bridge.MAX_RSSI * 100);
												bangText += " (" + signal + "%; monitoring)";
											}
											if (controlP5 != null) {
												pos = 4 + i++ * 30;
												Bang b = controlP5.addBang("m" + mac, 0, pos, 40, 10);
												if (b != null) {
													b.setGroup(roleGroups[role]);
													b.setColorActive(Color.WHITE.getRGB());
													if (mac.equals(currentDevice) ) {
														b.setColorForeground(Color.RED.getRGB());
														lastBang = b;
													}
													b.setCaptionLabel(bangText);
												}
											} else {
												System.out.println("bleh");
											}
										} else {
											System.out.println("bleh");
										}
									}
								} else {
									System.out.println("bleh");
								}
							} catch (Exception e) {
								System.out.println("bleh");
								pos += 30;
								break;
							}
						}
						break;
					case 1:
						break;
					case 0:
							Link thisDevice = br.feed.getDevices().get(PROBE_MAC).link;
							if (thisDevice != null) {
								float signal = 0 - thisDevice.getRssi() / Bridge.MAX_RSSI * 100;
								if (controlP5 != null) {
									controlP5.remove("d" + PROBE_MAC);
									Slider s = controlP5.addSlider("d" + PROBE_MAC,0,100,signal,0,4,(int)((double)(screen.width - 200) / 3.0 * 0.75),10);
									if (s != null) {
										s.setGroup(roleGroups[role]);
										signal *= 255 / 100;
										s.setColorForeground(new Color((int)signal,(int)signal,(int)signal).getRGB());
										s.setCaptionLabel("%");
									}
								} else {
									System.out.println("bleh");
								}
							} else {
								System.out.println("bleh");
							}
						break;
					default:
						break;
				}
				progress = 100;
				controlP5.remove("progress");
				
				roleGroups[role].setBackgroundHeight(pos + 40);
				roleGroups[role].setBackgroundColor(15);
				
				update.setPosition(roleGroups[role].position().x(), roleGroups[role].position().y() + roleGroups[role].getBackgroundHeight() + 20);
				update.captionLabel().set("Set probe to " + roles[role]);
				update.show();
			}
		}).start();
	}
}
