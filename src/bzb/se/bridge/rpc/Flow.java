/**
 * 
 */
package bzb.se.bridge.rpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.StringTokenizer;

public class Flow {
	public static Iterable<Flow> parseResultSet(final String results) {
		final Collection<Flow> links = new ArrayList<Flow>();

		final String[] lines = results.split("\n");
		for (int index = 2; index < lines.length; index++) {
			try {
				final String[] columns = lines[index].split("<\\|>");
				final Flow link = new Flow();
				final String time = columns[0].substring(1,
						columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				link.timeStamp = new Date(timeLong / 1000000);
				link.protocolNumber = Integer.parseInt(columns[1]);
				link.sourceIP = columns[2];
				link.sourcePort = Integer.parseInt(columns[3]);
				link.destIP = columns[4];
				link.destPort = Integer.parseInt(columns[5]);
				link.packetCount = Integer.parseInt(columns[6]);
				link.byteCount = Integer.parseInt(columns[7]);

				links.add(link);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return links;
	}

	// protocol number, source IP address, source port, destination IP address,
	// destination port,
	// number of packets, number of bytes
	private Date timeStamp;
	private int protocolNumber;
	private String sourceIP;
	private int sourcePort;
	private String destIP;
	private String classification;
	private int destPort;
	private int packetCount;
	private int byteCount;

	public Flow() {

	}

	public String getDestIP() {
		return destIP;
	}

	public int getDestPort() {
		return destPort;
	}

	public int getByteCount() {
		return byteCount;
	}

	public int getPacketCount() {
		return packetCount;
	}

	public int getProtocolNumber() {
		return protocolNumber;
	}

	public String getSourceIP() {
		return sourceIP;
	}

	public int getSourcePort() {
		return sourcePort;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public static Flow parse(final String logLine) {
		final Flow flow = new Flow();
		final int start = logLine.indexOf('@');
		final int end = logLine.indexOf('@', start + 1);
		final String time = logLine.substring(start + 1, end);
		final long timeLong = Long.parseLong(time, 16);
		flow.timeStamp = new Date(timeLong / 1000000);
		final StringTokenizer tokenizer = new StringTokenizer(logLine
				.substring(end + 1).trim(), ";");

		flow.protocolNumber = Integer.parseInt(tokenizer.nextToken());
		flow.sourceIP = tokenizer.nextToken();
		flow.destIP = tokenizer.nextToken();
		flow.sourcePort = Integer.parseInt(tokenizer.nextToken(), 16);
		flow.destPort = Integer.parseInt(tokenizer.nextToken(), 16);
		flow.classification = tokenizer.nextToken();
		flow.packetCount = Integer.parseInt(tokenizer.nextToken());
		flow.byteCount = Integer.parseInt(tokenizer.nextToken());

		return flow;
	}

	public String getClassification() {
		return classification;
	}
}
