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
	private Link link;
	
	public Pair (Long lastActivity, Link link) {
		this.lastActivity = lastActivity;
		this.setLink(link);
	}
	
	public boolean isOld() {
		if (System.currentTimeMillis() - lastActivity > 30000) {
			return true;
		} else {
			return false;
		}
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public Link getLink() {
		return link;
	}
	
}
