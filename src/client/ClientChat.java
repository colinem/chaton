package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import frames.Frame;
import frames.FrameIdPrivate;
import frames.FrameKoPrivate;
import frames.FrameLogin;
import frames.FrameLoginAccepted;
import frames.FrameLoginPrivate;
import frames.FrameLoginRefused;
import frames.FrameMessage;
import frames.FrameMessagePrivate;
import frames.FrameOkPrivate;
import frames.FrameRequestPrivate;
import readers.FrameReader;
import readers.Reader;
import visitors.PublicConnectionVisitor;

public class ClientChat implements PublicConnectionVisitor {


	static private int BUFFER_SIZE = 1_024;

	//	static private Logger logger = Logger.getLogger(ClientChat.class.getName());

	private final SocketChannel socketChannel;
	private final Selector selector;

	private final String host;
	private final int port;
	private SelectionKey key;
	private String login;
	private boolean loginAccepted;
	private FrameRequestPrivate requestPrivateReceived;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Queue<Frame> queue = new LinkedList<>();
	private final BlockingQueue<Frame> blockingQueue = new ArrayBlockingQueue<>(100);
	private boolean closed = false;
	private final Reader reader = new FrameReader(bbin);
	private final Map<String, String> privateConnections = new HashMap<>();
	private final String directory;
	

	public ClientChat(String host, int port, String dirName) throws IOException {
		socketChannel = SocketChannel.open();
		selector = Selector.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(this.host = host, this.port = port));
		directory = dirName;
	}

	public void launch() throws IOException {
		key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
		var selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
			selector.select();
//			System.out.println(" [debug] sort du select");
			while (!blockingQueue.isEmpty()) {
				queueMessage(blockingQueue.poll());
			}
			processSelectedKeys();
			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
//		System.out.println(" [debug] processSelectedKeys");
		for (SelectionKey key : selector.selectedKeys()) {
			System.out.println(" [debug] processSelectedKeys boucle");
			if (key.isValid() && key.isConnectable()) {
				System.out.println(" [debug] processSelectedKeys --> doConnect ");
				if (key == this.key)
					doConnect();
				else {
					System.out.println(" [debug] processSelectedKeys --> doConnect for private connections ");
					((PrivateConnection) key.attachment()).doConnect();
				}
			}
			if (key.isValid() && key.isWritable()) {
				System.out.println(" [debug] processSelectedKeys --> doWrite ");
				if (key == this.key)
					doWrite();
				else {
					System.out.println(" [debug] processSelectedKeys --> doWrite for private connection ");
					((PrivateConnection) key.attachment()).doWrite();
				}
			}
			if (key.isValid() && key.isReadable()) {
				System.out.println(" [debug] processSelectedKeys --> doRead");
				if (key == this.key)
					doRead();
				else {
					System.out.println(" [debug] processSelectedKeys --> doRead for private connection ");
					((PrivateConnection) key.attachment()).doRead();
				}
			}
		}
	}

	private void doConnect() throws IOException {
		//		System.out.println("doConnect");
		if (!socketChannel.finishConnect())
			return;
		updateInterestOps();
	}

	private void doRead() throws IOException {
		//		System.out.println("doRead");
		if (socketChannel.read(bbin) == -1)
			closed = true;
		processIn();
		updateInterestOps();
	}

	private void doWrite() throws IOException {
		//		System.out.println("doWrite");
		socketChannel.write(bbout.flip());
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	private void queueMessage(Frame frame) {
		queue.add(frame);
		processOut();
		updateInterestOps();
	}

	private void processIn() {
		//		System.out.println("processIn");
		while (true)
			switch (reader.process()) {
			case DONE:
				//				System.out.println("reader DONE");
				((Frame) reader.get()).accept(this);
				reader.reset();
				break;
			case ERROR:
				silentlyClose();
			case REFILL:
				//				System.out.println("reader REFILL");
				return;
			}
	}

	private void processOut() {
		//		System.out.println("processOut");
		while (!queue.isEmpty()) {
			var frameBuffer = queue.element().asBuffer();
			if (bbout.remaining() < frameBuffer.capacity())
				return;
			//			System.out.println("bbout position = " + bbout.position());
			bbout.put(frameBuffer.flip());
			//			System.out.println("bbout position = " + bbout.position());
			queue.remove();
		}
	}

	private void updateInterestOps() {
		//		System.out.println("updateInterestOps : closed ? " + closed + " ; bbin.remaining = " + bbin.remaining());
		var interestOps = 0;
		if (!closed && bbin.hasRemaining()) {
			//			System.out.println("OP_READ");
			interestOps = SelectionKey.OP_READ;
		}
		if (bbout.position() != 0) {
			//			System.out.println("OP_WRITE");
			interestOps |= SelectionKey.OP_WRITE;
		}
		if (interestOps == 0)
			silentlyClose();
		else
			key.interestOps(interestOps);
	}

	private void silentlyClose() {
		//		System.out.println("silentlyClose");
		try {
			socketChannel.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public static void main(String[] args) throws IOException {
		try {
			var client = new ClientChat(args[0], Integer.parseInt(args[1]), args[2]);

			new Thread(() -> {
				client.blockingQueue.offer(new FrameLogin(client.login = args[3]));
				try (var scan = new Scanner(System.in)) {
					while (scan.hasNextLine()) {
						Frame frame;
						var line = scan.nextLine();
						var tokens = line.split(" ", 2);

						if (!client.loginAccepted)
							frame = new FrameLogin(client.login = tokens[0]);
						else if (client.requestPrivateReceived != null) {
							switch (line.toUpperCase()) {
							case "Y":
								frame = new FrameOkPrivate(client.requestPrivateReceived);
								client.requestPrivateReceived = null;
								break;
							case "N":
								frame = new FrameKoPrivate(client.requestPrivateReceived);
								client.requestPrivateReceived = null;
								break;
							default:
								System.out.println(" >>> You didn't answer.");
								continue;
							}
						}

						else
							try {
								switch (line.charAt(0)) {
								case '@':
									//	System.out.println("ligne commence par @ --> new FrameMessagePrivate");
									var target = tokens[0].substring(1);
									if (target.isBlank())
										continue;
									frame = new FrameMessagePrivate(client.login, target, tokens[1]);
									break;
									
								case '/':
									target = tokens[0].substring(1);
									frame = new FrameRequestPrivate(client.login, target);
									client.privateConnections.put(target, tokens[1]);
									break;

								default:
									frame = new FrameMessage(client.login, line);
								}
							} catch (IndexOutOfBoundsException e) {
								continue;
							}

						if (frame != null) {
							client.blockingQueue.offer(frame);
							client.selector.wakeup();
						}

					}
				}
			}).start();

			client.launch();

		} catch (IndexOutOfBoundsException | NumberFormatException e) {
			usage();
		}
	}



	private static void usage(){
		System.out.println("Usage : ClientChat host port login directory");
	}


	// FRAME TREATMENT METHODS //

	@Override
	public void visit(FrameLogin frameLogin) {
		// DO NOTHING
	}

	@Override
	public void visit(FrameMessage frameMessage) {
		//		System.out.println("FrameMessage");
		System.out.println(frameMessage);
	}

	@Override
	public void visit(FrameMessagePrivate frameMessagePrivate) {
		//		System.out.println("FrameMessagePrivate mmh");
		System.out.println(frameMessagePrivate);
	}

	@Override
	public void visit(FrameIdPrivate frameIdPrivate) {
		var requester = frameIdPrivate.getLoginSender().get();
		var target = frameIdPrivate.getLoginTarget().get();
//		System.out.println(" [debug] received private id from server");
		if (requester.equals(login))
			new PrivateConnection(host, port, selector, target, frameIdPrivate.getLong().getAsLong(), directory, privateConnections.get(target));
		else
			new PrivateConnection(host, port, selector, requester, frameIdPrivate.getLong().getAsLong(), directory);
	}

	@Override
	public void visit(FrameKoPrivate frameKoPrivate) {
		var target = frameKoPrivate.getLoginTarget().get();
		System.out.println(" >>> " + target + " refused to establish a private connection with you. What a mean person.");
		privateConnections.remove(target);
	}

	@Override
	public void visit(FrameLoginAccepted frameLoginAccepted) {
		loginAccepted = true;
		System.out.println(" >>> You enter the chat.");
	}

	@Override
	public void visit(FrameLoginRefused frameLoginRefused) {
		System.out.println("Your login is already used.\n"
				+ "Enter another one");
	}

	@Override
	public void visit(FrameLoginPrivate frameLoginPrivate) {
		// DO NOTHING
	}

	@Override
	public void visit(FrameOkPrivate frameOkPrivate) {
		// DO NOTHING
	}

	@Override
	public void visit(FrameRequestPrivate frameRequestPrivate) {
		System.out.println(" >>> " + frameRequestPrivate.getLoginSender().get() + " would like to establish a private connection with you.\n"
				+ " >>> Do you accept ? (Y/N)");
		requestPrivateReceived = frameRequestPrivate;
	}
}
