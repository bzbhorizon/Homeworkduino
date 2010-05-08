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

public class Bridge implements Runnable, SerialPortEventListener {
	static CommPortIdentifier portId;

	InputStream inputStream;
	OutputStream outputStream;
	SerialPort serialPort;
	
	DataSender ds;
	
	static final String configFileURL = "res/config.xml";
	
	int role;
	
	public Bridge (int commPort) {
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
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
	}
	
	public static void main(String args[]) {
		new Bridge(Integer.parseInt(args[0]));
	}
	
	public void end() {
		if (ds != null) {
			ds.end();
		}
		
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
			
			ds = new DataSender();
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
				for (int i = 48; i < 61; i++) {
					send(i);
				}
			}
		}).start();
	}
	
	public void send (int data) {
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
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class DataSender implements Runnable {
		
		private boolean end = false;
		
		public DataSender () {
			updateRole();
			new Thread(this).start();
		}
		
		public void run () {
			while (!end) {
				send(role + 48);
			}
		}
		
		public void end () {
			end = true;
		}
		
	}
}