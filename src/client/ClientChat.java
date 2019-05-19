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

import frames.Frame;
import frames.FrameEstablished;
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
import server.Visitor;

public class ClientChat implements Visitor {

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ClientChat.class.getName());

	private final SocketChannel socketChannel;
	private final Selector selector;
	
	private SelectionKey key;
	private String login;
	private boolean loginAccepted;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Queue<Frame> queue = new LinkedList<>();
	private final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(100);
	private boolean closed = false;
	private final Reader reader = new FrameReader(bbin);

	public ClientChat(String host, int port) throws IOException {
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
			System.out.println("processSelectedKeys");
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
		System.out.println("doConnect");
		if (!socketChannel.finishConnect())
			return;
		updateInterestOps();
	}

	private void doRead() throws IOException {
		System.out.println("doRead");
		if (socketChannel.read(bbin) == -1)
			closed = true;
		processIn();
		updateInterestOps();
	}

	private void doWrite() throws IOException {
		System.out.println("doWrite");
		socketChannel.write(bbout.flip());
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	private void queueMessage(String line) {
		try {
			Frame frame;
			switch (line.charAt(0)) {
			case '@':
				var tokens = line.split(" ", 2);
				frame = new FrameMessagePrivate(login, tokens[0].substring(1), tokens[1]);
				break;
			case '/':
				tokens = line.split(" ", 2);
				frame = new FrameRequestPrivate(login, tokens[0].substring(1));
				break;
			default:
				if (loginAccepted)
					frame = new FrameMessage(login, line);
				else {
					System.out.println("queueMessage : FrameLogin");
					frame = new FrameLogin(login = line);
				}
			}
			queue.add(frame);
			processOut();
			updateInterestOps();
		} catch (IndexOutOfBoundsException e) {
			return; // dans le cas ou le user tape, par exemple, une truc comme "@ blabla" ou "@login"
		}
	}

	private void processIn() {
		System.out.println("processIn");
		while (true)
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
	}

	private void processOut() {
		System.out.println("processOut");
		while (!queue.isEmpty()) {
			var frameBuffer = queue.element().asBuffer();
			if (bbout.remaining() < frameBuffer.capacity())
				return;
			System.out.println("bbout position = " + bbout.position());
			bbout.put(frameBuffer.flip());
			System.out.println("bbout position = " + bbout.position());
			queue.remove();
		}
	}

	private void updateInterestOps() {
		System.out.println("updateInterestOps");
		var interestOps = 0;
		if (!closed && bbin.hasRemaining())
			interestOps = SelectionKey.OP_READ;
		if (bbout.position() != 0) {
			System.out.println("OP_WRITE");
			interestOps |= SelectionKey.OP_WRITE;
		}
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

	public static void main(String[] args) throws IOException {
		try {
			var client = new ClientChat(args[0], Integer.parseInt(args[1]));
			
			new Thread(() -> {
				client.blockingQueue.offer(args[2]);
				try (var scan = new Scanner(System.in)) {
					while (scan.hasNext()) {
						client.blockingQueue.offer(scan.next());
						client.selector.wakeup();
					}
				}
			}).start();
			
			client.launch();
			
		} catch (IndexOutOfBoundsException | NumberFormatException e) {
			usage();
		}
	}

	private static void usage(){
		System.out.println("Usage : ClientChat host port login");
	}
	
	
	// FRAME TREATMENT METHODS //

	@Override
	public void visit(FrameLogin frameLogin) {
		// TODO
	}

	@Override
	public void visit(FrameMessage frameMessage) {
		System.out.println(frameMessage.getLoginSender().get() + " to everyone : " + frameMessage.getMessage().get());
	}

	@Override
	public void visit(FrameMessagePrivate frameMessagePrivate) {
		System.out.println(frameMessagePrivate.getLoginSender() + " to you : " + frameMessagePrivate.getMessage().get());
	}

	@Override
	public void visit(FrameEstablished frameEstablished) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FrameIdPrivate frameIdPrivate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FrameKoPrivate frameKoPrivate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FrameLoginAccepted frameLoginAccepted) {
		loginAccepted = true;
		System.out.println("You enter the chat.");
	}

	@Override
	public void visit(FrameLoginRefused frameLoginRefused) {
		System.out.println("Your login is already used.\n"
				+ "Enter another one");
	}

	@Override
	public void visit(FrameLoginPrivate frameLoginPrivate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FrameOkPrivate frameOkPrivate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FrameRequestPrivate frameRequestPrivate) {
		// TODO Auto-generated method stub
		
	}
}
