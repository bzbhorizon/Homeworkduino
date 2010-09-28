/**
 * 
 */
package bzb.se.bridge;

/**
 * @author psxbdb
 *
 */

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.CopyOnWriteArrayList;

import bzb.se.Utility;
import bzb.se.bridge.online.Feed;

public class Bridge implements Runnable, SerialPortEventListener {
	private static CommPortIdentifier portId;

	private static InputStream inputStream;
	private static OutputStream outputStream;
	private static SerialPort serialPort;

	private static float minRssi = 0;
	private static float maxRssi = -120;
	private static final int MINS_INACTIVITY = 5;

	private static final int LIGHT_DELAY = 1300;
	private static final int MAX_LIGHT_DELAY = 6000;
	
	private static int role = 1;
	//private static String currentDevice;
	private static String probeMac;
	private static double signalStrength = minRssi / 100;
	private static double recentUsageBps = 0.0;
	private static double maxUsageBps = 0.0;
	private static long maxUsageAt = 0;

	private Feed feed;

	private Heartbeat heartbeat;

	public Bridge(String commPort) {
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier
				.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				//System.out.println(portId.getName());
				if (portId.getName().equals(commPort)) {
					try {
						probeMac = new DataInputStream(new BufferedInputStream(new FileInputStream(new File("res/probe.cfg")))).readUTF();
						new Thread(this).start();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		//System.out.println("Search ended");

		feed = new Feed(this);
		//feed.run();
		new Thread(feed).start();
	}

	public void end() {
		stopHeartbeat();
		
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (serialPort != null) {
			serialPort.close();
		}
	}

	public void run() {
		try {
			serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (UnsupportedCommOperationException e) {
			System.out.println(e);
		} catch (PortInUseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
	}

	private void send(byte[] data, String lightCommand) {
		if (outputStream != null) {
			try {
				//System.out.println(data);
				StringBuffer s = new StringBuffer();
				for (int i = 0; i < data.length; i++) {
					s.append(data[i] + " ");
				}
				Utility.writeToLog(s.toString() + "," + lightCommand);
				outputStream.write(data);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*private static final int[] ledPositions = new int[]{
		1,9,17,2,10,18,3,11,19,4,12,20,5,13,6,14,7,15,8,16
	};*/

	private static final int LEDS_PER_ROW = 8;
	private static final int TOTAL_LEDS = 20;
	private static final int ROWS = (int) Math.ceil((double) TOTAL_LEDS
			/ (double) LEDS_PER_ROW);
	private static byte[] LEDsinBinary = new byte[ROWS * 3];
	private static int lastLEDs = 0;

	private void lightProportionSpread(double percentage, int rgb, int remainderRgb) {
		//System.out.println(percentage);

		int LEDs = (int) (percentage * TOTAL_LEDS);
		int remainderLEDs = TOTAL_LEDS - LEDs;
		if (LEDs != lastLEDs) {
			lastLEDs = LEDs;
			LEDsinBinary = new byte[ROWS * 3];
			for (int i = 0; i < ROWS; i++) {
				int litThisRow = 0;
				if (LEDs > LEDS_PER_ROW) {
					litThisRow = LEDS_PER_ROW;
				} else {
					litThisRow = LEDs;
				}
				LEDs -= litThisRow;
				LEDsinBinary[i * 3 + rgb] = (byte) Utility.toBinary(litThisRow);

				if (remainderRgb >= 0 && remainderRgb < 3) {
					int remainingThisRow = LEDS_PER_ROW - litThisRow;
					int remainderThisRow = 0;
					if (remainderLEDs > remainingThisRow) {
						remainderThisRow = remainingThisRow;
					} else {
						remainderThisRow = remainderLEDs;
					}
					remainderLEDs -= remainderThisRow;
					LEDsinBinary[i * 3 + remainderRgb] = (byte) Utility.toBinary(litThisRow, remainderThisRow);
				}
			}
			send(LEDsinBinary, "lightProportionSpread," + percentage + "," + rgb + "," + remainderRgb);
		} else {
			//System.out.println("No change to proportion lit");
		}
	}
	
	private void lightProportionSequential(double percentage, int rgb, int remainderRgb) {
		//System.out.println(percentage);

		int LEDs = (int) (percentage * TOTAL_LEDS);
		if (LEDs != lastLEDs) {
			lastLEDs = LEDs;
			LEDsinBinary = new byte[ROWS * 3];
			for (int i = 0; i < TOTAL_LEDS; i++) {
				if (i < LEDs) {
					for (int j = 0; j < crap[i].length; j++) {
						LEDsinBinary[j * 3 + rgb] += crap[i][j];
					}
				} else if (remainderRgb >= 0 && remainderRgb < 3) {
					for (int j = 0; j < crap[i].length; j++) {
						LEDsinBinary[j * 3 + remainderRgb] += crap[i][j];
					}
				}
			}
			send(LEDsinBinary, "lightProportionSequential," + percentage + "," + rgb + "," + remainderRgb);
		} else {
			//System.out.println("No change to proportion lit");
		}
	}
	
	private void lightsOff () {
		send(new byte[ROWS * 3], "lightsOff");
	}
	
	private void lightProportionSequential(double r, double g, double b) {
		double total = r + g + b;

		int rLEDs = (int) (r / total * TOTAL_LEDS);
		int gLEDs = (int) (g / total * TOTAL_LEDS);
		
		if (rLEDs != lastLEDs) {
			lastLEDs = rLEDs;
			LEDsinBinary = new byte[ROWS * 3];
			for (int i = 0; i < TOTAL_LEDS; i++) {
				if (i < rLEDs) {
					for (int j = 0; j < crap[i].length; j++) {
						LEDsinBinary[j * 3] += crap[i][j];
					}
				} else if (i < rLEDs + gLEDs) {
					for (int j = 0; j < crap[i].length; j++) {
						LEDsinBinary[j * 3 + 1] += crap[i][j];
					}
				} else {
					for (int j = 0; j < crap[i].length; j++) {
						LEDsinBinary[j * 3 + 2] += crap[i][j];
					}
				}
			}
			send(LEDsinBinary, "lightProportionSequential," + r + "," + g + "," + b);
		} else {
			//System.out.println("No change to proportion lit");
		}
	}

	private void lightWarning () {
		lightProportionSpread(1, 0, -1);
	}
	
	private byte[][] crap = new byte[][]{
		new byte[]{1,0,0},
		new byte[]{0,1,0},
		new byte[]{0,0,1},
		new byte[]{2,0,0},
		new byte[]{0,2,0},
		new byte[]{0,0,2},
		new byte[]{4,0,0},
		new byte[]{0,4,0},
		new byte[]{0,0,4},
		new byte[]{8,0,0},
		new byte[]{0,8,0},
		new byte[]{0,0,8},
		new byte[]{16,0,0},
		new byte[]{0,16,0},
		//new byte[]{0,0,16},
		new byte[]{32,0,0},
		new byte[]{0,32,0},
		//new byte[]{0,0,32},
		new byte[]{64,0,0},
		new byte[]{0,64,0},
		new byte[]{(byte)128,0,0},
		new byte[]{0,(byte)128,0}
	};
	
	private void lightIndividual (int number, int rgb) {
		//System.out.print(number + " -> ");
		LEDsinBinary = new byte[ROWS * 3];
		for (int i = 0; i < 9; i++) {
			if (i%3 == rgb) {
				LEDsinBinary[i] = crap[number][i/3];
			} else {
				LEDsinBinary[i] = (byte)0;
			}
			//System.out.print(LEDsinBinary[i] + " ");
		}
		//System.out.println();
		send(LEDsinBinary, "lightIndividual," + number + "," + rgb);
	}
	
	private void lightPair (int number, int rgb) {
		int number2 = (number + TOTAL_LEDS / 2)%TOTAL_LEDS;
		LEDsinBinary = new byte[ROWS * 3];
		for (int i = 0; i < 9; i++) {
			if (i%3 == rgb) {
					LEDsinBinary[i] = crap[number][i/3];
					LEDsinBinary[i] += crap[number2][i/3];
			} else {
				LEDsinBinary[i] = (byte)0;
			}
		}
		send(LEDsinBinary, "lightPair," + number + "," + rgb);
	}
	
	private void startHeartbeat () {
		if (heartbeat == null) {
			heartbeat = new Heartbeat();
		}
		heartbeat.setRunning(true);
		new Thread(heartbeat).start();
	}
	
	private void stopHeartbeat () {
		if (heartbeat != null) {
			heartbeat.setRunning(false);
		}
	}
	
	private class Heartbeat implements Runnable {
		
		private boolean running;
		
		public Heartbeat () {
			setRunning(true);
		}

		public void run() {
			int i = 0;
			while (isRunning()) {
				if (i > TOTAL_LEDS - 1) {
					i = 0;
				}
				double usage = recentUsageBps / maxUsageBps;
				//System.out.println(usage);
				if (usage > 0.9) {
					lightPair(i, 0);
					i++;
				} else if (usage > 0.3) {
					lightPair(i, 1);
					i++;
				} else if (usage > 0.05) {
					lightPair(i, 2);
					i++;
				} else {
					lightPair(i, 2);
				}
				try {
					double delay = 1.0 - usage;
					if (delay < 0) {
						delay = 0;
					}
					delay = LIGHT_DELAY + delay * MAX_LIGHT_DELAY;
					Thread.sleep((long)delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public void setRunning(boolean running) {
			this.running = running;
		}

		public boolean isRunning() {
			return running;
		}
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			try {
				byte[] readBuffer = new byte[inputStream.available()];
				inputStream.read(readBuffer);
			} catch (IOException e) {
				System.out.println(e);
			}
			break;
		}
	}

	public void updateRole(int newRole) {
		role = newRole;
		if (role == 1) {
			startHeartbeat();
		} else {
			stopHeartbeat();
		}
		LEDsinBinary = new byte[ROWS * 3];
		lastLEDs = 0;
	}

	private long lastUpdated = 0;
	private static final int FLOW_POLL_PERIOD = 1000;

	private boolean flowsUpdating = false;
	
	public void updateFlows() {
		if (role == 1 && System.currentTimeMillis() - lastUpdated > FLOW_POLL_PERIOD && !flowsUpdating) {
			new Thread(new Runnable() {
				public void run () {
					flowsUpdating = true;
					Collection<CopyOnWriteArrayList<Flow>> sources = feed.getFlows().values();
					Iterator<CopyOnWriteArrayList<Flow>> flowsList = sources.iterator();
					double totalBytes = 0.0;
					while (flowsList.hasNext()) {
						CopyOnWriteArrayList<Flow> flows = flowsList.next();
						Iterator<Flow> flowIt = flows.iterator();
						while (flowIt.hasNext()) {
							Flow flow = flowIt.next();
							if (System.currentTimeMillis() - flow.getTimeStamp().getTime() > 30000) {
								flows.remove(flow);
							} else {
								if (flow.getTimeStamp().getTime() > lastUpdated) {
									totalBytes += flow.getByteCount();
								}
							}
						}
					}
					recentUsageBps = totalBytes / ((System.currentTimeMillis() - lastUpdated) / 1000.0);

					if (recentUsageBps > maxUsageBps) {
						maxUsageBps = recentUsageBps;
						maxUsageAt = System.currentTimeMillis();
						//System.out.println("Raising maximum bandwidth used to " + maxUsageBps / 1000 + "kbps");
					}
					
					// drop max usage threshold after 5 minutes of lower activity
					if (System.currentTimeMillis() - maxUsageAt > 1000*60*MINS_INACTIVITY) {
						maxUsageAt -= 1000*60*MINS_INACTIVITY;
						maxUsageBps *= 0.75;
						//System.out.println("Decaying maximum bandwidth used to " + maxUsageBps / 1000 + "kbps");
					}
					
					lastUpdated = System.currentTimeMillis();
					flowsUpdating = false;
				}
			}).start();
		}
	}
	
	private static final int LINK_POLL_PERIOD = 500;

	public void updateLinks() {
		if (role == 0 && System.currentTimeMillis() - lastUpdated > LINK_POLL_PERIOD) {
			new Thread(new Runnable() {
				public void run () {
					boolean foundProbe = false;
					Iterator<Link> links = feed.getLinks().iterator();
					while (links.hasNext()) {
						Link link = links.next();
						if (probeMac != null
								&& link.getMacAddress().equals(probeMac)) {
							if (link.getRssi() < minRssi) {
								minRssi = link.getRssi() - 5;
							} else if (link.getRssi() > maxRssi) {
								maxRssi = link.getRssi() + 5;	
							}
							if (role == 0) {
								float range = Math.abs(minRssi - maxRssi);
								signalStrength = 1 + (link.getRssi() - maxRssi) / range;
								lightProportionSequential(signalStrength, 1, -1);
								foundProbe = true;
							}
							break;
						}
					}
					if (!foundProbe) {
						lightWarning();
					}
					lastUpdated = System.currentTimeMillis();
				}
			}).start();
		}
	}

	private static int lastDevicesVisible = 0;
	private static int lastLeases = 0;
	private static final int DEVICE_POLL_PERIOD = 5000;
	
	public void updateDevices() {
		if (role == 2 && System.currentTimeMillis() - lastUpdated > DEVICE_POLL_PERIOD) {
			new Thread(new Runnable() {
				public void run () {
					double r = 0;
					double g = 0;
					double b = 0;
					
					Map<String, Pair> ids = feed.getDevices();
		
					if (lastDevicesVisible < ids.size()) {
						b = 1;
					}
					lastDevicesVisible = ids.size();
					Iterator<Pair> pairs = ids.values().iterator();
		
					int leases = 0;
					while (pairs.hasNext()) {
						Pair pair = pairs.next();
						if (pair.getLink().getIpAddress() != null) {
							leases++;
						}
						if (pair.getLink().getRetryCount() > ((double)pair.getLink().getPacketCount() * 0.25)) {
							r = 1;
						}
					}
					
					if (lastLeases < leases) {
						g = 1;
					}
					lastLeases = leases;
					if (r == 0 && g == 0 && b == 0) {
						lightsOff();
					} else {
						lightProportionSequential(r, g, b);
					}
					
					lastUpdated = System.currentTimeMillis();
				}
			}).start();
		}
	}

}