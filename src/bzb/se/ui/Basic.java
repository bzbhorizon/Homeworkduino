/**
 * 
 */
package bzb.se.ui;

import bzb.se.bridge.Bridge;

/**
 * @author bzb
 *
 */
public class Basic {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Bridge bridge = new Bridge(6);
		bridge.testPattern();
	}

}
