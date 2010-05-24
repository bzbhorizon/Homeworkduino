/**
 * 
 */
package bzb.se.bridge.online;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import bzb.se.bridge.Bridge;
import bzb.se.bridge.JavaSRPC;
import bzb.se.bridge.Lease;
import bzb.se.bridge.Link;
import bzb.se.bridge.Pair;
import bzb.se.bridge.Lease.Action;

/**
 * @author bzb
 *
 */
public class Feed implements Runnable {
	
	JavaSRPC rpc = new JavaSRPC();
	private static final long OLD = 12000; // 12 seconds
	private static final int TIME_DELTA = 10;
	private Date last = null;
	private final Map<String, Link> links = new HashMap<String, Link>();
	/**
	 * @return the links
	 */
	public Collection<Link> getLinks() {
		return new ArrayList<Link>(links.values());
	}

	private final Map<String, Lease> leases = new HashMap<String, Lease>();
	
	Map<String, Pair> devices = Collections.synchronizedMap(new HashMap<String, Pair>());
	
	/**
	 * @return the devices
	 */
	public Map<String, Pair> getDevices() {
		return devices;
	}
	
	private Bridge br;
	
	public Feed (Bridge br) {
		this.br = br;
	}
	
	public void run () {
		
		while (true) {
			if (!rpc.isConnected()) {
				try {
					rpc.connect(InetAddress.getByName("192.168.0.1"),
							987);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			

			Date expected = new Date();
			String linkQuery;
			String leaseQuery;

			while (rpc.isConnected()) {
				
				expected = new Date(expected.getTime() + TIME_DELTA);
				if (last != null) {
					final String s = String.format("@%016x@", last
							.getTime() * 1000000);
					linkQuery = String
							.format(
									"SQL:select * from Links [ range %d seconds ] where timestamp > %s",
									TIME_DELTA + 1, s);
					leaseQuery = String
							.format(
									"SQL:select * from Leases where timestamp > %s",
									TIME_DELTA + 1, s);
				} else {
					linkQuery = String
							.format(
									"SQL:select * from Links [ range %d seconds ]",
									TIME_DELTA);
					leaseQuery = String.format(
							"SQL:select * from Leases", TIME_DELTA);
				}

				try {
					String linkResults = rpc.call(linkQuery);

					if (linkResults != null) {
						Iterable<Link> newLinks = Link
								.parseResultSet(linkResults);
						synchronized (links) {
							for (Link link : newLinks) {
								final Link existingLink = links
										.get(link.getMacAddress());
								if (existingLink != null) {
									existingLink.update(link);
								} else {
									links.put(link.getMacAddress(),
											link);
									final Lease lease = leases.get(link
											.getMacAddress());
									if (lease != null) {
										link.update(lease);
									}
									
									Pair p = new Pair(System.currentTimeMillis(), link);
									if (p.isOld()) {
										devices.remove(link.getMacAddress());
									} else {
										devices.put(link.getMacAddress(), new Pair(System.currentTimeMillis(), link));
									}
								}
							}
						}
					}

					String leaseResults = rpc.call(leaseQuery);
					if (leaseResults != null) {
						Iterable<Lease> newLeases = Lease
								.parseResultSet(leaseResults);
						synchronized (links) {
							for (Lease lease : newLeases) {
								final Link existingLink = links
										.get(lease.getMacAddress());
								if (existingLink != null) {
									existingLink.update(lease);
								}

								if (lease.getAction() == Action.add) {
									leases.put(lease.getMacAddress(),
											lease);
								} else if (lease.getAction() == Action.del) {
									leases
											.remove(lease
													.getMacAddress());
								}
							}
						}
					}

					last = new Date();
					
					br.updateDevices();
					br.updateLinks();
					
				} catch (Exception e1) {
					// logger.log(Level.SEVERE, e1.getMessage(), e1);
					e1.printStackTrace();
				}

				try {
					Thread.sleep(2000);
				} catch (Exception e) {
				}
			}
		}
		
	}

}
