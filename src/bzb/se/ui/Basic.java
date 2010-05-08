/**
 * 
 */
package bzb.se.ui;

import java.awt.Color;

import processing.core.PApplet;
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

	public static void main(String args[]) {
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
	
	public void setup() {
		size(screen.width, screen.height);
		smooth();
		controlP5 = new ControlP5(this);
		
		Bang update = controlP5.addBang("update", screen.width / 2 - 50, screen.height - 100, 100, 30);
		Label l = update.captionLabel();
		l.set("Update Probe");
		
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
			} else {
				roleGroups[i].close();
			}
			roleGroups[i].activateEvent(true);
		}
	}
		
	static String getRoleText () {
		String text = "Currently the probe is set to the '" + roles[role] + "' role. In this role the probe " + roleTexts[role]; 
		return text;
	}
	
	public void controlEvent(ControlEvent theEvent) {
		if(theEvent.isGroup()) {
			if (theEvent.group().isOpen()) {
				for (int i = 0; i < roleGroups.length; i++) {
					if (theEvent.group().name().equals(roleGroups[i].name())) {
						role = i;
						roleLabel.setValue(getRoleText());
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
	
	public void update (int value) {
		System.out.println(role);
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
