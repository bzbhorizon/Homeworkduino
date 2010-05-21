/**
 * 
 */
package bzb.se.bridge.offline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import bzb.se.bridge.Flow;
import bzb.se.bridge.Link;
import bzb.se.bridge.Pair;

/**
 * @author bzb
 * 
 */
public class Feed implements Runnable {
	
	Collection<Flow> flows = new ArrayList<Flow>();
	Collection<Link> links = new ArrayList<Link>();
	HashMap<String, Pair> devices = new HashMap<String, Pair>();
	
	public Feed () {
		try {
			BufferedReader input = new BufferedReader(new FileReader(
					"res/flowdump.txt"));
			try {
				String line = null;
				while ((line = input.readLine()) != null) {
						flows.add(Flow.parse(line));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		try {
			BufferedReader input = new BufferedReader(new FileReader(
					"res/linkdump.txt"));
			try {
				String line = null;
				while ((line = input.readLine()) != null) {
						links.add(Link.parseLink(line));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		new Thread(new Runnable() {
			public void run () {
				long lastTime = 0;
				Iterator<Flow> i = flows.iterator();
				while (i.hasNext()) {
					Flow flow = i.next();
					if (lastTime == 0) {
						lastTime = flow.getTimeStamp().getTime();
					}
					
					long delay = flow.getTimeStamp().getTime() - lastTime;
					System.out.println(flow.toString());
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					lastTime = flow.getTimeStamp().getTime();
				}
			}
		}).start();
		
		new Thread(new Runnable() {
			public void run () {
				long lastTime = 0;
				Iterator<Link> i = links.iterator();
				while (i.hasNext()) {
					Link link = i.next();
					if (lastTime == 0) {
						lastTime = link.getTimeStamp().getTime();
					}
					
					long delay = link.getTimeStamp().getTime() - lastTime;
					System.out.println(link.toJSON());
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					devices.put(link.getMacAddress(), new Pair(System.currentTimeMillis(), link));
					
					lastTime = link.getTimeStamp().getTime();
				}
			}
		}).start();
	}

}
