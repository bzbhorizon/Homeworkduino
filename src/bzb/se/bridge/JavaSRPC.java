package bzb.se.bridge;

/**
 * 
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaSRPC {
	private static final Logger logger = Logger.getLogger(JavaSRPC.class
			.getName());

	private enum Command {
		ERROR, CONNECT, CACK, QUERY, QACK, RESPONSE, RACK, DISCONNECT, DACK, FRAGMENT, FACK, PING, PACK, SEQNO, SACK,
	}

	private class ReceiverThread extends Thread {
		@Override
		public void run() {
			while (socket != null && socket.isConnected()) {
				final byte[] buffer = new byte[FRAGMENT_SIZE * 10];
				final DatagramPacket packet = new DatagramPacket(buffer,
						buffer.length);
				try {
					socket.receive(packet);

					final DataInputStream is = new DataInputStream(
							new ByteArrayInputStream(buffer));

					// Payload Header
					final int rsubport = is.readInt();
					assert (rsubport == subport);
					final int rseqno = is.readInt();
					assert (rseqno == seqno);
					final Command command = getCommand(is.readUnsignedShort());
					// logger.info("Received " + command);
					final int fragment = is.readUnsignedByte();
					final int fragmentCount = is.readUnsignedByte();

					// Data
					switch (command) {
					case CONNECT:
						// Do we need to implement this?
						break;

					case CACK:
						setState(RPCState.IDLE);
						break;

					case QUERY:
						// Do we need to implement this?
						sendCommand(Command.QACK);
						setState(RPCState.QACK_SENT);
						break;

					case QACK:
						setState(RPCState.AWAITING_RESPONSE);
						break;

					case RESPONSE:
						if (readData(is, fragment)) {
							sendCommand(Command.RACK, null, fragment,
									fragmentCount);
							setState(RPCState.IDLE);
						}
						break;

					case RACK:
						setState(RPCState.IDLE);
						break;

					case DISCONNECT:
						sendCommand(Command.DACK);
						setState(RPCState.TIMEDOUT);
						break;

					case DACK:
						setState(RPCState.TIMEDOUT);
						break;

					case FRAGMENT:
						if (readData(is, fragment)) {
							sendCommand(Command.FACK, null, fragment,
									fragmentCount);
							setState(RPCState.FACK_SENT);
						}
						break;

					case FACK:
						if (state == RPCState.FRAGMENT_SENT) {
							// TODO Check frag no.
							setState(RPCState.FACK_RECEIVED);
						}
						break;

					case PING:
						sendCommand(Command.PACK);
						break;

					case PACK:
						// TODO Reset ping state
						break;

					case SEQNO:
						// Do we need to implement this?
						setState(RPCState.IDLE);
						break;

					case SACK:
						if (state == RPCState.SEQNO_SENT) {
							setState(RPCState.IDLE);
						}
						break;

					default:
						break;
					}
				} catch (final IOException e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	private enum RPCState {
		IDLE, QACK_SENT, RESPONSE_SENT, CONNECT_SENT, QUERY_SENT, AWAITING_RESPONSE, TIMEDOUT, DISCONNECT_SENT, FRAGMENT_SENT, FACK_RECEIVED, FRAGMENT_RECEIVED, FACK_SENT, SEQNO_SENT,
	}

	private class TimerThread extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					sleep(TICK_LENGTH);
				} catch (final InterruptedException e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}

				if (state == RPCState.TIMEDOUT) {
					break; // ?
				} else if (EnumSet.of(RPCState.CONNECT_SENT,
						RPCState.QUERY_SENT, RPCState.RESPONSE_SENT,
						RPCState.DISCONNECT_SENT, RPCState.FRAGMENT_SENT,
						RPCState.SEQNO_SENT).contains(state)) {
					// Tick down to retry
					ticksLeft--;
					if (ticksLeft <= 0) {
						attemptsLeft--;
						if (attemptsLeft > 0) {
							logger.info("Resending" + attemptsLeft);
							try {
								resend();
								ticksLeft = TICKS;
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							logger.info("TIMED OUT!");
							setState(RPCState.TIMEDOUT);
						}
					}
				} else {
					// Tick down to ping
					ticksLeft--;
					if (ticksLeft <= 0) {
						// try
						// {
						// sendCommand(Command.PING);
						// ticksLeft = PING_TICKS;
						// }
						// catch (IOException e)
						// {
						// logger.log(Level.SEVERE, e.getMessage(), e);
						// }
					}
				}
			}
		}
	}

	private static Command getCommand(final int commandID) {
		for (final Command command : EnumSet.allOf(Command.class)) {
			if (command.ordinal() == commandID) {
				return command;
			}
		}
		return null;
	}

	private DatagramSocket socket = null;

	private RPCState state;

	private int seqno = 0;
	private int subport;
	private int ticksLeft;
	private int lastFragment;
	private int attemptsLeft;

	private ByteArrayOutputStream responseData;
	private byte[] lastPayload;

	private InetAddress address;
	private int port = 987;

	private static final int TICKS = 10;
	// private static final int PING_TICKS = 2;
	private static final int TICK_LENGTH = 20;
	private static final int ATTEMPTS = 7;
	private static final int FRAGMENT_SIZE = 1024;

	public String call(final String query) throws IOException {
		if (socket != null) {
			sendCommand(Command.SEQNO);
			setState(RPCState.SEQNO_SENT);
			waitForState(EnumSet.of(RPCState.IDLE, RPCState.TIMEDOUT));

			seqno++;

			final byte[] queryBytes = (query + "\0").getBytes("US-ASCII");
			final int fragmentCount = queryBytes.length / FRAGMENT_SIZE + 1;

			for (int fragment = 1; fragment < fragmentCount; fragment++) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream os = new DataOutputStream(bos);
				final int start = (fragment - 1) * FRAGMENT_SIZE;
				os.writeShort(queryBytes.length);
				os.writeShort(FRAGMENT_SIZE);
				os.write(queryBytes, start, FRAGMENT_SIZE);
				os.flush();

				sendCommand(Command.FRAGMENT, bos.toByteArray(), fragment,
						fragmentCount);
				setState(RPCState.FRAGMENT_SENT);
				waitForState(EnumSet.of(RPCState.FACK_RECEIVED,
						RPCState.TIMEDOUT));
				if (state == RPCState.TIMEDOUT) {
					throw new IOException();
				}
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream os = new DataOutputStream(bos);
			int start = (fragmentCount - 1) * FRAGMENT_SIZE;
			int finalLength = queryBytes.length - start;
			os.writeShort(queryBytes.length);
			os.writeShort(finalLength);
			os.write(queryBytes, start, finalLength);
			os.flush();

			sendCommand(Command.QUERY, bos.toByteArray(), fragmentCount,
					fragmentCount);
			setState(RPCState.QUERY_SENT);
			waitForState(EnumSet.of(RPCState.IDLE, RPCState.TIMEDOUT));

			if (state == RPCState.IDLE) {
				return new String(responseData.toByteArray());
			}
		}

		return null;
	}

	public void connect(final InetAddress address, final int port)
			throws SocketException, IOException {
		// logger.info(address.toString() + ":" + port);
		socket = new DatagramSocket();
		socket.connect(address, port);

		this.subport = 8888;
		this.address = address;
		this.port = port;

		sendCommand(Command.CONNECT, "HWDB\0".getBytes("US-ASCII"), 1, 1);
		setState(RPCState.CONNECT_SENT);

		new ReceiverThread().start();
		new TimerThread().start();

		waitForState(EnumSet.of(RPCState.IDLE, RPCState.TIMEDOUT));
		if (state == RPCState.TIMEDOUT) {
			// TODO Cleanup
			throw new IOException();
		}
	}

	public void disconnect() throws IOException {
		sendCommand(Command.DISCONNECT);
		setState(RPCState.DISCONNECT_SENT);
		waitForState(EnumSet.of(RPCState.IDLE, RPCState.TIMEDOUT));
	}

	private byte[] getBytes(final Command command) throws IOException {
		return getBytes(command, null, (byte) 1, (byte) 1);
	}

	private byte[] getBytes(final Command command, final byte[] data,
			final int fragment, final int fragmentCount) throws IOException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final DataOutputStream os = new DataOutputStream(bos);
		os.writeInt(subport);
		os.writeInt(seqno);
		os.writeShort(command.ordinal());
		os.writeByte(fragment);
		os.writeByte(fragmentCount);
		if (data != null) {
			os.write(data);
		}
		os.flush();
		return bos.toByteArray();
	}

	private void sendBytes(final byte[] bytes) throws IOException {
		attemptsLeft = ATTEMPTS;
		lastPayload = bytes;
		ticksLeft = TICKS;
		socket.send(new DatagramPacket(bytes, bytes.length, address, port));
	}

	public boolean isConnected() {
		return socket != null && state != RPCState.TIMEDOUT;
	}

	private void resend() throws IOException {
		socket.send(new DatagramPacket(lastPayload, lastPayload.length));
	}

	private void sendCommand(final Command command) throws IOException {
		// logger.info("Send " + command);
		sendBytes(getBytes(command));
	}

	private void sendCommand(final Command command, final byte[] data,
			final int fragment, final int fragmentCount) throws IOException {
		// logger.info("Send " + command);
		sendBytes(getBytes(command, data, fragment, fragmentCount));
	}

	private synchronized final void setState(final RPCState newState) {
		this.state = newState;
		notify();
	}

	private synchronized void waitForState(final EnumSet<RPCState> set) {
		// logger.info("Waiting for " + set);
		while (!set.contains(state)) {
			try {
				wait();
			} catch (final InterruptedException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		// logger.info("Waiting over, state = " + state);
	}

	private boolean readData(final DataInputStream is, final int fragment)
			throws IOException {
		final int dataLength = is.readUnsignedShort();
		final int fragmentLength = is.readUnsignedShort();
		final byte[] data = new byte[Math.min(is.available(), fragmentLength)];
		is.readFully(data);

		if (state == RPCState.QUERY_SENT || state == RPCState.AWAITING_RESPONSE) {
			responseData = new ByteArrayOutputStream(dataLength);
			responseData.write(data);
			lastFragment = fragment;
			return true;
		} else if (state == RPCState.FACK_SENT) {
			if (fragment - lastFragment == 1) {
				responseData.write(data);
				lastFragment = fragment;
				return true;
			} else if (fragment == lastFragment) {
				resend();
				setState(RPCState.FACK_SENT);
			} else {
				// TODO Exception?
			}
		}
		return false;
	}
}
