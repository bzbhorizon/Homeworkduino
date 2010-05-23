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
import java.util.TooManyListenersException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import bzb.se.Utility;
import bzb.se.bridge.offline.Feed;
import bzb.se.ui.Control;

public class Bridge implements Runnable, SerialPortEventListener {
	static CommPortIdentifier portId;

	InputStream inputStream;
	OutputStream outputStream;
	SerialPort serialPort;
		
	static final String configFileURL = "res/config.xml";
	
	int role;
	
	public Feed feed;
	
	public Bridge (int commPort) {		
		/*Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals("COM" + commPort)) {
					new Thread(this).start();
					break;
				}
			}
		}
		System.out.println("Search ended");*/
		
		//sendWireless(110, 120);
		
		feed = new Feed(this);
		feed.run();
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
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
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
	
	public void testPattern () {
		new Thread(new Runnable() {
			public void run () {
				byte[] testData = new byte[256];
				for (int i = 0; i < 256; i++) {
					testData[i] = (byte)i;
				}
				send(testData);
			}
		}).start();
	}
	
	public void send (byte[] data) {
		if (outputStream != null) {
			try {
				outputStream.write(data);
				System.out.println("written " + data);
				Thread.sleep(1500);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final int LEDS_PER_ROW = 8;
	public static final int TOTAL_LEDS = 20;
	public static final int ROWS = (int) Math.ceil((double)TOTAL_LEDS / (double)LEDS_PER_ROW);
	public static byte[] leds = new byte[ROWS * 3];
	
	public void sendWireless (double strength, double maxStrength) {
		//convert to percentage
		double percentStr = strength / maxStrength;
		
		int greenLEDs = (int)(percentStr * TOTAL_LEDS);
		int redLEDs = TOTAL_LEDS - greenLEDs;
		
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
			System.out.print(Utility.toBinary(greenThisRow, redThisRow) + " " + Utility.toBinary(greenThisRow) + " " + 0 + " ");
			leds[i * 3] = (byte) Utility.toBinary(greenThisRow, redThisRow);
		}
		System.out.println();
		for (int i = 0; i < leds.length; i++) {
			System.out.print(leds[i] + " ");
		}
		send(leds);
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
			byte[] readBuffer = new byte[10];

			try {
				while (inputStream.available() > 0) {
					inputStream.read(readBuffer);
				}
				System.out.println("Received \"" + new String(readBuffer).trim() + "\"");
			} catch (IOException e) {
				System.out.println(e);
			}
			break;
		}
	}
	
	public void updateRole () {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
		.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(configFileURL));
			doc.getDocumentElement().normalize();
			role = Integer.parseInt(((Element)(doc.getElementsByTagName("config").item(0))).getAttribute("role"));
			//send(role);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateFlows () {
		System.out.println("flow update");
	}
	
	public void updateLinks () {
		System.out.println("link update");
	}
	
	public void updateDevices () {
		System.out.println("device update");
	}

}