/**
 * 
 */
package bzb.se.bridge;

/**
 * @author bzb
 *
 */
public class Pair {

	public Long lastActivity;
	public Link link;
	
	public Pair (Long lastActivity, Link link) {
		this.lastActivity = lastActivity;
		this.link = link;
	}
	
	public boolean isOld() {
		if (System.currentTimeMillis() - lastActivity > 30000) {
			return true;
		} else {
			return false;
		}
	}
	
}
