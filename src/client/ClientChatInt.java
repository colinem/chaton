package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class ClientChatInt {

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ClientChatInt.class.getName());

	private final SocketChannel socketChannel;
	private final Selector selector;
	
	private SelectionKey key;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Queue<Frame> queue = new LinkedList<>();
	private final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(100);
	private boolean closed = false;

	public ClientChatInt(String host, int port) throws IOException {
		socketChannel = SocketChannel.open();
		selector = Selector.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(host, port));
	}

	public void launch() throws IOException {
		key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
		var selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
			selector.select();
			
			while (!blockingQueue.isEmpty())
				queueMessage(blockingQueue.poll());
			
			processSelectedKeys();
			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selector.selectedKeys()) {
			if (key.isValid() && key.isConnectable()) {
				doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				doRead();
			}
		}
	}

	private void doConnect() throws IOException {
		if (!socketChannel.finishConnect())
			return;
		updateInterestOps();
	}

	private void doRead() throws IOException {
		if (socketChannel.read(bbin) == -1)
			closed = true;
		processIn();
		updateInterestOps();
	}

	private void doWrite() throws IOException {
		socketChannel.write(bbout.flip());
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	private void queueMessage(String line) {
		Frame frame;
		switch (line.charAt(0)) {
		case '@':
			frame = new PrivateMessage(line);
			break;
		case '/':
			frame = new PrivateConnectionNegociation(line);
			break;
		default:
			frame = new PrivateMessage(line);
		}
		queue.add(frame);
		processOut();
		updateInterestOps();
	}

	private void processIn() {
		while (true)
			switch (intReader.process()) {
			case DONE:
				System.out.println("Received : " + intReader.get());
				intReader.reset();
				break;
			case ERROR:
				silentlyClose();
			case REFILL:
				return;
			}
	}

	private void processOut() {
		while (!queue.isEmpty()) {
			var frameBuffer = queue.element().asByteBuffer();
			if (bbout.remaining() < frameBuffer.capacity())
				return;
			bbout.putInt();
			queue.remove();
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

	private void silentlyClose() {
		try {
			socketChannel.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 2){
			usage();
			return;
		}
		
		var client = new ClientChatInt(args[0], Integer.parseInt(args[1]));
		
		new Thread(() -> {
			try (var scan = new Scanner(System.in)) {
				while (scan.hasNext()) {
					client.blockingQueue.offer(scan.next());
					client.selector.wakeup();
				}
			}
		}).start();
		
		client.launch();
	}

	private static void usage(){
		System.out.println("Usage : ClientChatInt host port");
	}
}
