package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import frames.FrameEstablished;

public class PrivateConnection implements Connection {

	static private int BUFFER_SIZE = 1_024;

	private SelectionKey keyA;
	private SocketChannel scA;
	private SelectionKey keyB;
	private SocketChannel scB;
	final private ByteBuffer bbA = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbB = ByteBuffer.allocate(BUFFER_SIZE);
	private boolean closed = false;
	
	public void connect(SelectionKey key, SocketChannel sc) {
		if (keyA == null) {
			keyA = key;
			scA = sc;
		}
		else if (keyB == null) {
			keyB = key;
			scB = sc;
//			System.out.println("[debug] creation of FrameEstablished");
			var establishedBB = new FrameEstablished().asBuffer();
			bbA.put(establishedBB.flip());
			bbB.put(establishedBB.flip());
			updateInterestOps();
		}
		else
			System.out.println("A third person tries to enter the private connection.");
		key.attach(this);
	}

	private void updateInterestOps() {
		var interestOps = 0;
		if (!closed && bbA.hasRemaining())
			interestOps = SelectionKey.OP_READ;
		if (bbB.position() != 0)
			interestOps |= SelectionKey.OP_WRITE;
		if (interestOps == 0)
			silentlyClose();
		else
			keyA.interestOps(interestOps);
		interestOps = 0;
		if (!closed && bbB.hasRemaining())
			interestOps = SelectionKey.OP_READ;
		if (bbA.position() != 0)
			interestOps |= SelectionKey.OP_WRITE;
		if (interestOps == 0)
			silentlyClose();
		else
			keyB.interestOps(interestOps);
//		System.out.println("[debug] keyA interestOps = " + keyA.interestOps());
//		System.out.println("[debug] keyB interestOps = " + keyB.interestOps());
	}

	@Override
	public void doRead() throws IOException {
		if (scA.read(bbA) == -1)
			closed = true;
		if (scB.read(bbB) == -1)
			closed = true;
//		System.out.println(" [debug] doRead " + StandardCharsets.UTF_8.decode(bbA.flip()));
//		System.out.println(" [debug] doRead " + StandardCharsets.UTF_8.decode(bbB.flip()));
		updateInterestOps();

	}

	@Override
	public void doWrite() throws IOException {
//		System.out.println(" [debug] doWrite position=" + bbB.position() + " --> " + StandardCharsets.UTF_8.decode(bbB.flip()));
		scA.write(bbB.flip());
		bbB.compact();
//		System.out.println(" [debug] doWrite position=" + bbA.position() + " --> " + StandardCharsets.UTF_8.decode(bbA.flip()));
		scB.write(bbA.flip());
		bbA.compact();
		updateInterestOps();
	}

	public void silentlyClose() {
		try {
			if (scA != null)
				scA.close();
			if (scB != null)
				scB.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

}
