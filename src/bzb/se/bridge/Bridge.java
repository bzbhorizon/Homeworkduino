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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TooManyListenersException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import bzb.se.Utility;
import bzb.se.bridge.offline.Feed;

public class Bridge implements Runnable, SerialPortEventListener {
	static CommPortIdentifier portId;

	InputStream inputStream;
	OutputStream outputStream;
	SerialPort serialPort;

	static final String configFileURL = "res/config.xml";
	public static final float MIN_RSSI = 20;
	public static final float MAX_RSSI = 100;

	int role;
	String currentDevice;

	public Feed feed;

	private Heartbeat heartbeat;

	public Bridge(int commPort) {
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier
				.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals("COM" + commPort)) {
					new Thread(this).start();
					break;
				}
			}
		}
		System.out.println("Search ended");

		feed = new Feed(this);
		feed.run();
		//new Thread(feed).start();

		heartbeat = new Heartbeat((MAX_RSSI - MIN_RSSI) / 2 + MIN_RSSI);
		new Thread(heartbeat).start();
	}

	public static void main(String args[]) {
		new Bridge(Integer.parseInt(args[0]));
	}

	public void end() {
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

			updateRole();
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

	public void testPattern() {
		new Thread(new Runnable() {
			public void run() {
				byte[] testData = new byte[256];
				for (int i = 0; i < 256; i++) {
					testData[i] = (byte) i;
				}
				send(testData);
			}
		}).start();
	}

	public void send(byte[] data) {
		if (outputStream != null) {
			try {
				outputStream.write(data);
				/*System.out.print("written ");
				for (int i = 0; i < data.length; i++) {
					System.out.print(data[i] + " ");
				}
				System.out.println();*/
				Thread.sleep(500);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static final int LEDS_PER_ROW = 8;
	public static final int TOTAL_LEDS = 20;
	public static final int ROWS = (int) Math.ceil((double) TOTAL_LEDS
			/ (double) LEDS_PER_ROW);
	public static byte[] leds = new byte[ROWS * 3];
	static int lastGreenLEDs = 0;

	public void sendStrength(double strength, double maxStrength) {
		double percentStr = 1 - strength / maxStrength;
		System.out.println(percentStr);

		int greenLEDs = (int) (percentStr * TOTAL_LEDS);
		if (greenLEDs != lastGreenLEDs) {
			lastGreenLEDs = greenLEDs;
			int redLEDs = 0;//TOTAL_LEDS - greenLEDs;

			for (int i = 0; i < ROWS; i++) {
				int greenThisRow = 0;
				if (greenLEDs > LEDS_PER_ROW) {
					greenThisRow = LEDS_PER_ROW;
				} else {
					greenThisRow = greenLEDs;
				}
				greenLEDs -= greenThisRow;
				leds[i * 3 + 1] = (byte) Utility.toBinary(greenThisRow);

				int remainingThisRow = LEDS_PER_ROW - greenThisRow;
				int redThisRow = 0;
				if (redLEDs > remainingThisRow) {
					redThisRow = remainingThisRow;
				} else {
					redThisRow = redLEDs;
				}
				redLEDs -= redThisRow;
				leds[i * 3] = (byte) Utility.toBinary(greenThisRow, redThisRow);
			}
			send(leds);
		} else {
			System.out.println("No significant change to signal strength");
		}
	}
	
	class Heartbeat implements Runnable {
		
		private double strength;
		private boolean running;
		private boolean panicking;
		
		public Heartbeat (double strength) {
			setRunning(true);
			setPanicking(false);
			setStrength(strength);
		}

		public void run() {
			while (isRunning()) {
				for (int i = 0; i < TOTAL_LEDS; i++) {
					/*if (isPanicking()) {
						sendStrength(0, 1);
						sendStrength(1, 1);
					} else {*/
						sendStrength(i, TOTAL_LEDS);
						System.out.println("thump");
						try {
							long delay = (long) (4000.0 * (getStrength() / (MAX_RSSI - MIN_RSSI)));
							System.out.println("waiting " + delay);
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					//}
				}
			}
		}

		public void setStrength(double strength) {
			this.strength = strength;
		}

		public double getStrength() {
			return strength;
		}

		public void setRunning(boolean running) {
			this.running = running;
		}

		public boolean isRunning() {
			return running;
		}

		public void setPanicking(boolean panicking) {
			this.panicking = panicking;
		}

		public boolean isPanicking() {
			return panicking;
		}
		
	}
	
	static double[] lastProportions;

	public void sendProportions(double[] proportions) {
		boolean same = true;
		if (lastProportions == null) {
			same = false;
		} else {
			for (int i = 0; i < proportions.length; i++) {
				if (proportions[i] != lastProportions[i]) {
					same = false;
					break;
				}
			}
		}
		if (!same) {
			lastProportions = proportions;

			/*
			 * int greenLEDs = (int)(percentStr * TOTAL_LEDS); lastGreenLEDs =
			 * greenLEDs; int redLEDs = TOTAL_LEDS - greenLEDs;
			 * 
			 * for (int i = 0; i < ROWS; i++) { int greenThisRow = 0; if
			 * (greenLEDs > LEDS_PER_ROW) { greenThisRow = LEDS_PER_ROW; } else
			 * { greenThisRow = greenLEDs; } greenLEDs -= greenThisRow; leds[i *
			 * 3 + 1] = (byte) Utility.toBinary(greenThisRow);
			 * 
			 * int remainingThisRow = LEDS_PER_ROW - greenThisRow; int
			 * redThisRow = 0; if (redLEDs > remainingThisRow) { redThisRow =
			 * remainingThisRow; } else { redThisRow = redLEDs; } redLEDs -=
			 * redThisRow; leds[i * 3] = (byte) Utility.toBinary(greenThisRow,
			 * redThisRow); }
			 */
			for (int i = 0; i < leds.length; i++) {
				leds[i] = 0;
			}
			send(leds);
		} else {
			System.out.println("No significant change to activity");
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
				/*System.out.println("Received \""
						+ new String(readBuffer).trim() + "\"");*/
			} catch (IOException e) {
				System.out.println(e);
			}
			break;
		}
	}

	public void updateRole() {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(configFileURL));
			doc.getDocumentElement().normalize();
			Element config = (Element) doc.getElementsByTagName("config").item(
					0);
			role = Integer.parseInt(config.getAttribute("role"));
			currentDevice = config.getAttribute("monitoring");
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	long lastUpdated = 0;
	int pollPeriod = 1000;

	public void updateFlows() {
		if (role == 1) {
			if (System.currentTimeMillis() - lastUpdated > pollPeriod) {
				//sendProportions(new double[] { 10, 20, 15 });
				lastUpdated = System.currentTimeMillis();
			}
		}
	}

	public void updateLinks() {
		if (role == 0 || role == 2) {
			if (System.currentTimeMillis() - lastUpdated > pollPeriod) {
				Iterator<Link> links = feed.getLinks().iterator();
				while (links.hasNext()) {
					Link link = links.next();
					if (currentDevice != null
							&& link.getMacAddress().equals(currentDevice)) {
						//sendStrength(0 - link.getRssi() - MIN_RSSI, MAX_RSSI);
						heartbeat.setStrength(0 - link.getRssi() - MIN_RSSI);
						System.out.println("str " + heartbeat.getStrength());
						if (heartbeat.getStrength() < (MAX_RSSI - MIN_RSSI) / 4) {
							heartbeat.setPanicking(true);
						} else if (heartbeat.isPanicking()) {
							heartbeat.setPanicking(false);
						}
						break;
					}
				}
				lastUpdated = System.currentTimeMillis();
			}
		}
	}

	public void updateDevices() {
		// System.out.println("device update");
	}

}