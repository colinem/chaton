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
	
	public void complete(SelectionKey key, SocketChannel sc) {
		if (keyA == null) {
			keyA = key;
			scA = sc;
		}
		else if (keyB == null) {
			keyB = key;
			scB = sc;
			var establishedBB = new FrameEstablished().asBuffer().flip();
			bbA.put(establishedBB);
			bbB.put(establishedBB);
		}
		else
			throw new IllegalStateException();
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
	}

	@Override
	public void doRead() throws IOException {
		if (scA.read(bbA) == -1)
			closed = true;
		if (scB.read(bbB) == -1)
			closed = true;
		updateInterestOps();
	}

	@Override
	public void doWrite() throws IOException {
		scA.write(bbB.flip());
		bbB.compact();
		scB.write(bbA.flip());
		bbA.compact();
		updateInterestOps();
	}

	public void silentlyClose() {
		try {
			scA.close();
			scB.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

}
