package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.logging.Logger;

import frames.Frame;
import frames.FrameEstablished;
import frames.FrameLoginPrivate;
import readers.FrameReader;
import readers.Reader;
import readers.StringReader;
import visitors.PrivateConnectionVisitor;

class PrivateConnection implements PrivateConnectionVisitor {

	static private Logger logger = Logger.getLogger(PrivateConnection.class.getName());
	static private int BUFFER_SIZE = 1_024;

	private SelectionKey key;
	private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	private final Reader reader = new FrameReader(bbin);
	private final Reader Sreader = new StringReader(bbin);

	private boolean closed = false;
	private boolean connectionEstablished;
	private final String distantClient;
	private LinkedList<ByteBuffer> queue=new LinkedList<>();

	public PrivateConnection(String host, int port, Selector selector, String distantClient, long connectId) {
		this.distantClient = distantClient;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress(host, port));
			key = sc.register(selector, SelectionKey.OP_CONNECT);
			key.attach(this);
			bbout.put(new FrameLoginPrivate(connectId).asBuffer().flip());
		} catch (IOException e) {
			logger.severe("Connection closed due to IOException");
			silentlyClose();
		}
	}

	private void updateInterestOps() {
		var interestOps = 0;
		if (!closed && bbin.hasRemaining())
			interestOps = SelectionKey.OP_READ;
		if (bbout.position() != 0)
			interestOps |= SelectionKey.OP_WRITE;
		if (interestOps == 0)
			silentlyClose();
		else
			key.interestOps(interestOps);
	}

	private void processIn() {
		if (!connectionEstablished)
			switch (reader.process()) {
			case DONE:
				((Frame) reader.get()).accept(this);
				reader.reset();
				break;
			case ERROR:
				silentlyClose();
			case REFILL:
				return;
			}
		else { // TODO
			// en attendant l'implementation client http //
			switch (Sreader.process()) {
				case DONE:
					System.out.println("Received from private connection with "+distantClient+": " + Sreader.get());

					reader.reset();
					break;
				case ERROR:
					silentlyClose();
				case REFILL:
					return;
			}
			//System.out.println("Received from private connection with"++": " + Charset.forName("UTF-8").decode(bbin.flip()));
			//bbin.compact();
			// ----------------------------------------- //
		}
	}
	private void processOut() {
		while (!queue.isEmpty()) {
			ByteBuffer toSend = queue.element();
			if (bbout.remaining() < toSend.capacity())
				return;
						System.out.println("bbout position = " + bbout.position());
			bbout.put(toSend);
						System.out.println("bbout position = " + bbout.position());
			bbout.compact();
			queue.remove();
		}
	}
	void doRead() throws IOException {
		if (sc.read(bbin) == -1)
			closed = true;
		processIn();
		updateInterestOps();
	}

	void doWrite() throws IOException {
		sc.write(bbout.flip());
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	void doConnect() throws IOException {
		//		System.out.println("doConnect");
		if (!sc.finishConnect())
			return;
		updateInterestOps();
	}

	private void silentlyClose() {
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public void sendContent(String string){
		System.out.println(string);
		queue.add(frames.StringToBbManager.stringToBB(string));
		processOut();
		updateInterestOps();
	}



	@Override
	public void visit(FrameEstablished frameEstablished) {
		System.out.println(" >>> Private connection established with " + distantClient + ".");
		connectionEstablished = true;
	}


}