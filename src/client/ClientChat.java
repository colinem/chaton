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
import server.ServerChat;
import server.Visitor;

public class ClientChat {
	
	class Context implements Visitor {

		private final ClientChat client;
		private final SocketChannel sc;
		private SelectionKey key;
		private String login;
		private boolean loginAccepted;
		final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<Frame> queue = new LinkedList<>();
		private final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(100);
		private boolean closed = false;
		private final Reader reader = new FrameReader(bbin);
		
		private Context(ClientChat client, SelectionKey key){
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.client = client;
		}

		private void doConnect() throws IOException {
//			System.out.println("doConnect");
			if (!socketChannel.finishConnect())
				return;
			updateInterestOps();
		}

		private void doRead() throws IOException {
//			System.out.println("doRead");
			if (socketChannel.read(bbin) == -1)
				closed = true;
			processIn();
			updateInterestOps();
		}

		private void doWrite() throws IOException {
//			System.out.println("doWrite");
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
//					System.out.println("ligne commence par @ --> new FrameMessagePrivate");
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
					else
						frame = new FrameLogin(login = line);
				}
				queue.add(frame);
				processOut();
				updateInterestOps();
			} catch (IndexOutOfBoundsException e) {
				return; // dans le cas ou le user tape, par exemple, une truc comme "@ blabla" ou "@login"
			}
		}

		private void processIn() {
//			System.out.println("processIn");
			while (true)
				switch (reader.process()) {
				case DONE:
//					System.out.println("reader DONE");
					((Frame) reader.get()).accept(this);
					reader.reset();
					break;
				case ERROR:
					silentlyClose();
				case REFILL:
//					System.out.println("reader REFILL");
					return;
				}
		}

		private void processOut() {
//			System.out.println("processOut");
			while (!queue.isEmpty()) {
				var frameBuffer = queue.element().asBuffer();
				if (bbout.remaining() < frameBuffer.capacity())
					return;
//				System.out.println("bbout position = " + bbout.position());
				bbout.put(frameBuffer.flip());
//				System.out.println("bbout position = " + bbout.position());
				queue.remove();
			}
		}

		private void updateInterestOps() {
//			System.out.println("updateInterestOps : closed ? " + closed + " ; bbin.remaining = " + bbin.remaining());
			var interestOps = 0;
			if (!closed && bbin.hasRemaining()) {
//				System.out.println("OP_READ");
				interestOps = SelectionKey.OP_READ;
			}
			if (bbout.position() != 0) {
//				System.out.println("OP_WRITE");
				interestOps |= SelectionKey.OP_WRITE;
			}
			if (interestOps == 0)
				silentlyClose();
			else
				key.interestOps(interestOps);
		}

		private void silentlyClose() {
//			System.out.println("silentlyClose");
			try {
				socketChannel.close();
			} catch (IOException e) {
				// ignore exception
			}
		}
		
		
		// FRAME TREATMENT METHODS //

		@Override
		public void visit(FrameLogin frameLogin) {
			// TODO
		}

		@Override
		public void visit(FrameMessage frameMessage) {
//			System.out.println("FrameMessage");
			System.out.println(frameMessage);
		}

		@Override
		public void visit(FrameMessagePrivate frameMessagePrivate) {
//			System.out.println("FrameMessagePrivate mmh");
			System.out.println("wtf " + frameMessagePrivate);
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
			System.out.println(" >>> You enter the chat.");
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

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ClientChat.class.getName());

	private final SocketChannel socketChannel;
	private final Selector selector;
	private Context publicConnection;
	

	public ClientChat(String host, int port) throws IOException {
		socketChannel = SocketChannel.open();
		selector = Selector.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(host, port));
		var key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
		key.attach(publicConnection = new Context(this, key));
	}

	public void launch() throws IOException {
		var selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
			selector.select();
			while (!publicConnection.blockingQueue.isEmpty()) {
				publicConnection.queueMessage(publicConnection.blockingQueue.poll());
			}
			processSelectedKeys();
			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selector.selectedKeys()) {
//			System.out.println("processSelectedKeys");
			if (key.isValid() && key.isConnectable()) {
				((Context) key.attachment()).doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		try {
			var client = new ClientChat(args[0], Integer.parseInt(args[1]));
			
			new Thread(() -> {
				client.publicConnection.blockingQueue.offer(args[2]);
				try (var scan = new Scanner(System.in)) {
					while (scan.hasNextLine()) {
						client.publicConnection.blockingQueue.offer(scan.nextLine());
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
}
