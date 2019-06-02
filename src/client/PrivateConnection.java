package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import frames.Frame;
import frames.FrameEstablished;
import frames.FrameLoginPrivate;
import frames.StringToBbManager;
import readers.FrameReader;
import readers.Reader;
import visitors.PrivateConnectionVisitor;

class PrivateConnection implements PrivateConnectionVisitor {

	static private Logger logger = Logger.getLogger(PrivateConnection.class.getName());
	static private int BUFFER_SIZE = 1_024;

	private SelectionKey key;
	private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	private final Reader reader = new FrameReader(bbin);
	private boolean closed = false;
	private boolean privateConnectionEstablished;
	private final String distantClient;
	private final long connectId;
	private LinkedList<ByteBuffer> queue = new LinkedList<>();
	private final String directory;
	private String file;

	public PrivateConnection(String host, int port, Selector selector, String distantClient, long connectId, String directory) {
		this.distantClient = distantClient;
		this.connectId = connectId;
		this.directory = directory;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress(host, port));
			key = sc.register(selector, SelectionKey.OP_CONNECT);
			key.attach(this);
		} catch (IOException e) {
			logger.severe("Connection closed due to IOException");
			silentlyClose();
		}
	}

	public PrivateConnection(String host, int port, Selector selector, String target, long connectId, String directory,
			String file) {
		this(host, port, selector, target, connectId, directory);
		this.file = file;
	}

	private void updateInterestOps() {
		var interestOps = 0;
		if (!closed && bbin.hasRemaining()) {
			System.out.println(" [debug] OP_______READ ");
			interestOps = SelectionKey.OP_READ;
		}
		if (bbout.position() != 0) {
			System.out.println(" [debug] OP_______WRITE ");
			interestOps |= SelectionKey.OP_WRITE;
		}
		if (interestOps == 0)
			silentlyClose();
		else
			key.interestOps(interestOps);
	}

	private void processIn() throws IOException {
		while (true)
			if (!privateConnectionEstablished)
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
			else if (file == null) { // if client is target
				var httpReader = new HTTPReader(sc, bbin);
				var request = httpReader.readHeader().getResponse();
				var response = new StringBuilder();
				try {
					var tmp = new Object() { ByteBuffer bb = ByteBuffer.allocate(2_048); };
					Files.lines(Paths.get(directory + "/" + request.split(" ")[1]))
					.forEach(l -> {
						var encodedLine = StandardCharsets.UTF_8.encode(l);
						if (tmp.bb.remaining() < encodedLine.capacity())
							tmp.bb = ByteBuffer.allocate(tmp.bb.capacity()*2).put(tmp.bb.flip());
						tmp.bb.put(encodedLine);
					});
					response.append("HTTP/1.1 200 OK\r\n")
					.append("Date: ").append(new Date()).append("\r\n")
					.append("Content-Length: ").append(tmp.bb.flip().remaining()).append("\r\n")
					.append("Content-Type: text/html;charset=UTF-8\r\n\r\n")
					.append(StandardCharsets.UTF_8.decode(tmp.bb));
				} catch (InvalidPathException | IndexOutOfBoundsException e) { // if the file does not exist or the request is ill-formed.
					response.append("HTTP/1.1 404 KO\r\n\r\n");
				}
				queueMessage(response.toString());
			}
			else { // if client is requester
				var httpReader = new HTTPReader(sc, bbin);
				var header = httpReader.readHeader();
				if (header.getCode() == 200)
					System.out.println(header.getCharset().decode(httpReader.readBytes(header.getContentLength()).flip()));
				else
					System.out.println(header);
			}
	}

	private void processOut() {
		while (!queue.isEmpty()) {
			var toSend = queue.element();
			if (bbout.remaining() < toSend.capacity())
				return;
			System.out.println(" [debug] processOut");
			System.out.println(" [debug] toSend = " + StandardCharsets.UTF_8.decode(toSend));
			toSend.flip(); // a enlever avec la ligne du dessus
			System.out.println(" [debug] bbout = " + StandardCharsets.UTF_8.decode(bbout.flip()));
			bbout.limit(bbout.capacity()); // a enlever avec la ligne du dessus
			bbout.put(toSend);
			System.out.println(" [debug] bbout = " + StandardCharsets.UTF_8.decode(bbout.flip()));
			bbout.limit(bbout.capacity()); // a enlever avec la ligne du dessus
			queue.remove();
		}
	}

	public void queueMessage(String msg) {
		System.out.println(" [debug] queueMessage : " + msg);
		queue.add(StringToBbManager.stringToBB(msg));
		processOut();
		updateInterestOps();
	}

	public void queueMessage(Frame frame) {
		System.out.println(" [debug] queueMessage frame.asBuffer = " + StandardCharsets.UTF_8.decode(frame.asBuffer().flip()));
		queue.add(frame.asBuffer().flip());
		processOut();
		updateInterestOps();
	}

	void doRead() throws IOException {
		System.out.println(" [debug] private connection doRead");
		if (sc.read(bbin) == -1)
			closed = true;
		processIn();
		updateInterestOps();
	}

	void doWrite() throws IOException {
		System.out.println(" [debug] private connection doWrite --> " + StandardCharsets.UTF_8.decode(bbout.flip()));
		sc.write(bbout.flip());
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	void doConnect() throws IOException {
		//		System.out.println("doConnect");
		if (!sc.finishConnect())
			return;
		queueMessage(new FrameLoginPrivate(connectId));
		if (file != null) {
			queueMessage("GET " + file + " HTTP/1.1\r\n"
					+ "Host: localhost\r\n"
					+ "\r\n");
		}
	}

	private void silentlyClose() {
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}



	@Override
	public void visit(FrameEstablished frameEstablished) {
		System.out.println(" >>> Private connection established with " + distantClient + ".");
		privateConnectionEstablished = true;
	}


}