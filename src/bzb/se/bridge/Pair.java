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
	
}
