package server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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


public class ServerChat {

	static private class Context implements PublicConnectionVisitor, Connection {

		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<Frame> queue = new LinkedList<>();
		final private ServerChat server;
		private boolean closed = false;
		private final Reader reader = new FrameReader(bbin);
		private String login;
		private ArrayList<String> connectionAsked=new ArrayList<>();
		private Context(ServerChat server, SelectionKey key){
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.server = server;
		}

		private void addToconnectionAsked(String string){
			connectionAsked.add(string);
		}
		/**
		 * Process the content of bbin
		 *
		 * The convention is that bbin is in write-mode before the call
		 * to process and after the call
		 *
		 */
		private void processIn() {
			while (true)
				switch (reader.process()) {
					case DONE:
//					System.out.println(bbin.toString());
						((Frame) reader.get()).accept(this);
						reader.reset();
						break;
					case REFILL:
						return;
					case ERROR:
						silentlyClose();
						return;
				}
		}

		/**
		 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
		 *
		 * @param msg
		 */
		private void queueMessage(Frame msg) {
//			System.out.println("queueMessage");
			queue.add(msg);
			processOut();
			updateInterestOps();
		}

		/**
		 * Try to fill bbout from the message queue
		 *
		 */
		private void processOut() {
//			System.out.println("OK");
			while (!queue.isEmpty()) {
				var frameBuff = queue.element().asBuffer();
				if (bbout.remaining() < frameBuff.capacity())
					return;
				bbout.put(frameBuff.flip());
				queue.remove();
			}
		}

		/**
		 * Update the interestOps of the key looking
		 * only at values of the boolean closed and
		 * of both ByteBuffers.
		 *
		 * The convention is that both buffers are in write-mode before the call
		 * to updateInterestOps and after the call.
		 * Also it is assumed that process has been be called just
		 * before updateInterestOps.
		 */

		private void updateInterestOps() {
//			System.out.println("updateInterestOps : closed ? " + closed
//					+ "\n ; bbin.remaining = " + bbin.remaining()
//					+ "\n ; bbbin.position = " + bbin.position());
			var interestOps = 0;
			if (!closed && bbin.hasRemaining())
				interestOps = SelectionKey.OP_READ;
			if (bbout.position() != 0)
				interestOps |= SelectionKey.OP_WRITE;
			if (interestOps == 0){
				silentlyClose();
			}
			else
				key.interestOps(interestOps);

		}

		private void silentlyClose() {
//			System.out.println(login + " : silentlyClose");
			try {
				server.clients.remove(login);
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Performs the read action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call
		 * to doRead and after the call
		 *
		 * @throws IOException
		 */
		@Override
		public void doRead() throws IOException {
			if (sc.read(bbin) == -1)
				closed = true;
			processIn();
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call
		 * to doWrite and after the call
		 *
		 * @throws IOException
		 */
		@Override
		public void doWrite() throws IOException {
//			System.out.println("doWrite");
//			System.out.println("bbout = " + bbout);
			sc.write(bbout.flip());
			bbout.compact();
//			System.out.println("bbout = " + bbout);
			processOut();
			updateInterestOps();
		}

		@Override
		public void visit(FrameLogin frameLogin) {
			if (login != null || frameLogin.getLoginSender().isEmpty()) {
				silentlyClose();
				return;
			}
			var login = frameLogin.getLoginSender().get();
			if (server.clients.containsKey(login))
				queue.add(new FrameLoginRefused());
			else {
//				System.out.println("login accepted");
				server.clients.put(this.login = login, key);
				queue.add(new FrameLoginAccepted());
			}
//			System.out.println("visit FrameLogin");
			processOut();
			updateInterestOps();
		}

		@Override
		public void visit(FrameMessage frameMessage) {
//			System.out.println("FrameMessage");
			var senderLogin = frameMessage.getLoginSender();
			if (senderLogin.isPresent() && senderLogin.get().equals(login))
				server.broadcast(frameMessage);
		}

		@Override
		public void visit(FrameMessagePrivate frameMessagePrivate) {
			//Todo pas sure si necessaire verifier que personne existe
//			System.out.println("FrameMessagePrivate");
			var targetLogin = frameMessagePrivate.getLoginTarget();
			if (frameMessagePrivate.getLoginSender().get().equals(login) && server.clients.containsKey(targetLogin.get()))
				((Context) server.clients.get(targetLogin.get()).attachment()).queueMessage(frameMessagePrivate);
		}

		@Override
		public void visit(FrameIdPrivate frameIdPrivate) {
			// DO NOTHING
		}

		@Override
		public void visit(FrameKoPrivate frameKoPrivate) {
			var senderLogin = frameKoPrivate.getLoginSender();
			var targetLogin = frameKoPrivate.getLoginTarget();
			if (targetLogin.get().equals(login))
				((Context) server.clients.get(senderLogin.get()).attachment()).queueMessage(frameKoPrivate);
		}

		@Override
		public void visit(FrameLoginAccepted frameLoginAccepted) {
			// DO NOTHING
		}

		@Override
		public void visit(FrameLoginPrivate frameLoginPrivate) {
//			System.out.println(" [debug] received private login from client");
			var pc = server.privateConnections.get(frameLoginPrivate.getLong().getAsLong());
			if (pc != null){
				pc.connect(key, sc);
			}else {
				silentlyClose();
			}

		}

		@Override
		public void visit(FrameLoginRefused frameLoginRefused) {

		}

		@Override
		public void visit(FrameOkPrivate frameOkPrivate) {
			var senderLogin = frameOkPrivate.getLoginSender();
		/*	System.out.println(senderLogin.get());
			System.out.println(login);
			System.out.println(connectionAsked);*/
			if(!connectionAsked.contains(senderLogin.get())){

				return;
			}

			var targetLogin = frameOkPrivate.getLoginTarget();
			var id = (long)  new Random().nextLong();
			var frameIdPrivate = new FrameIdPrivate(frameOkPrivate, id);
			if (targetLogin.get().equals(login)) {
				((Context) server.clients.get(senderLogin.get()).attachment()).queueMessage(frameIdPrivate);
				((Context) server.clients.get(targetLogin.get()).attachment()).queueMessage(frameIdPrivate);
			}
			server.privateConnections.put(id, new PrivateConnection());
		}

		@Override
		public void visit(FrameRequestPrivate frameRequestPrivate) {
			var senderLogin = frameRequestPrivate.getLoginSender();
			var targetLogin = frameRequestPrivate.getLoginTarget();
			if (senderLogin.get().equals(login) && server.clients.containsKey(targetLogin.get())){
				//System.out.println(senderLogin.get());
				//System.out.println(login);
				((Context)server.clients.get(frameRequestPrivate.getLoginTarget().get()).attachment()).addToconnectionAsked(login);
				//System.out.println(((Context)server.clients.get(frameRequestPrivate.getLoginTarget().get()).attachment()).connectionAsked);
				((Context) server.clients.get(targetLogin.get()).attachment()).queueMessage(frameRequestPrivate);

			}

		}

	}

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ServerChat.class.getName());

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Map<String, SelectionKey> clients = new HashMap<>();
	private final Map<Long, PrivateConnection> privateConnections = new HashMap<>();

	public ServerChat(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		while(!Thread.interrupted()) {


			printKeys(); // for debug
			System.out.println("Starting select");
			try {
				selector.select(this::treatKey);

			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
		printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch(IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isWritable()) {
				((Connection) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Connection) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO,"Connection closed with client due to IOException",e);
			silentlyClose(key);
		}


	}

	private void doAccept(SelectionKey key) throws IOException {
		var socket = serverSocketChannel.accept();
		if (socket != null) {
			socket.configureBlocking(false);
			var newKey = socket.register(selector, SelectionKey.OP_READ);
			newKey.attach(new Context(this, newKey));
		}
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	/**
	 * Add a message to all connected clients queue
	 *
	 * @param msg
	 */
	private void broadcast(Frame msg) {
		for (var key : clients.values())
			((Context) key.attachment()).queueMessage(msg);
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length!=1){
			usage();
			return;
		}
		new ServerChat(Integer.parseInt(args[0])).launch();
	}

	private static void usage(){
		System.out.println("Usage : ServerChat port");
	}

	/***
	 *  Theses methods are here to help understanding the behavior of the selector
	 ***/

	private String interestOpsToString(SelectionKey key){
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
		if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
		if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
		return String.join("|",list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}

		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet){
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e){
			return "???";

		}
	}

	public void printSelectedKey(SelectionKey key) {
		SelectableChannel channel = key.channel();
		if (channel instanceof ServerSocketChannel) {
			System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
		} else {
			SocketChannel sc = (SocketChannel) channel;
			System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable()) list.add("ACCEPT");
		if (key.isReadable()) list.add("READ");
		if (key.isWritable()) list.add("WRITE");
		return String.join(" and ",list);
	}
}