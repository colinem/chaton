package readers;


import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class StringReader implements Reader {

	private enum State { DONE, WAITING_SIZE, WAITING_CONTENT, ERROR };

	private final ByteBuffer bb;
	private State state = State.WAITING_SIZE;
	private int size;
	private String value;
	private final Charset utf8 = Charset.forName("UTF-8");

	public StringReader(ByteBuffer bb) {
		this.bb = bb;
	}

	@Override
	public ProcessStatus process() {
		try {
			bb.flip();
			switch (state) {
			case WAITING_SIZE:
				if (bb.remaining() < Integer.BYTES)
					return ProcessStatus.REFILL;
				size = bb.getInt();
				state = State.WAITING_CONTENT;
			case WAITING_CONTENT:
				if (bb.remaining() < size)
					return ProcessStatus.REFILL;
				var initialLimit = bb.limit();
				value = utf8.decode(bb.limit(bb.position() + size)).toString();
				bb.limit(initialLimit);
				state = State.DONE;
				return ProcessStatus.DONE;
			default:
				throw new IllegalStateException();
			}
		} finally {
			bb.compact();
		}


	}

	@Override
	public Object get() {
		if (state != State.DONE)
			throw new IllegalStateException();
		return value;
	}

	@Override
	public void reset() {
		state = State.WAITING_SIZE;
	}

	public static void main(String[] args) {
		var utf8 = Charset.forName("UTF-8");
		var bb = ByteBuffer.allocate(1_024);
		var login = utf8.encode("coline");
		bb.putInt(login.remaining()).put(login).putInt(login.capacity());
		var messageReader = new StringReader(bb);
		System.out.println("status : " + messageReader.process());
		System.out.println((String) messageReader.get()+"\n");
		messageReader.reset();
		System.out.println("status : " + messageReader.process());
		bb.put(utf8.encode("c"));
		System.out.println("status : " + messageReader.process());
		bb.put(utf8.encode("oline"));
		System.out.println("status : " + messageReader.process());
		System.out.println((String) messageReader.get()+"\n");
	}
}
