/**
 * 
 */
package bzb.se;

/**
 * @author bzb
 *
 */
public abstract class Utility {

	public static int toBinary (int ledsLit) {
		return (int) (Math.pow(2, ledsLit) - 1);
	}
	
	public static int toBinary (int ledsLitFrom, int ledsLit) {
		int binary = 0;
		for (int j = ledsLitFrom + 1; j <= ledsLitFrom + ledsLit; j++) {
			binary += (int) (Math.pow(2, j - 1));
		}
		return binary;
	}
	
}
