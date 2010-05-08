/**
 * 
 */
package bzb.se.ui;

import processing.core.PApplet;


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
	
	public void setup() {
		size(200, 200);
	}
	
	public void draw() {
		
	}

}
