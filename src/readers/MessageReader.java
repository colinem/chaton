package readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MessageReader implements Reader {

	public class Message {
		String login;
		String content;
	}

	private enum State { DONE, WAITING_LOGIN, WAITING_CONTENT, ERROR};

	private State state = State.WAITING_LOGIN;
	private Message message = new Message();
	private StringReader stringReader;

	public MessageReader(ByteBuffer bb) {
		stringReader = new StringReader(bb);
	}

	@Override
	public ProcessStatus process() {
		switch (state) {
		case WAITING_LOGIN:
			switch (stringReader.process()) {
			case DONE:
				message.login = (String) stringReader.get();
				stringReader.reset();
				state = State.WAITING_CONTENT;
				break;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				return ProcessStatus.ERROR;
			}
		case WAITING_CONTENT:
			switch (stringReader.process()) {
			case DONE:
				message.content = (String) stringReader.get();
				stringReader.reset();
				state = State.DONE;
				return ProcessStatus.DONE;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				return ProcessStatus.ERROR;
			}
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	public Object get() {
		if (state != State.DONE)
			throw new IllegalStateException();
		return message;
	}

	@Override
	public void reset() {
		stringReader.reset();
		state = State.WAITING_LOGIN;
	}

	public static void main(String[] args) {
		var utf8 = Charset.forName("UTF-8");
		var bb = ByteBuffer.allocate(1_024);
		var login = utf8.encode("coline");
		var content = utf8.encode("cc");
		bb.putInt(login.remaining()).put(login).putInt(content.remaining()).put(content)
		.putInt(login.flip().remaining()).put(utf8.encode("co"));
		var messageReader = new MessageReader(bb);
		System.out.println("status : " + messageReader.process());
		var message = (Message) messageReader.get();
		System.out.println(message.login + " : " + message.content + "\n");
		messageReader.reset();

		System.out.println("status : " + messageReader.process());
		bb.put(utf8.encode("line")).putInt(utf8.encode("test").remaining()).put(utf8.encode("test"));
		System.out.println("status : " + messageReader.process());
		message = (Message) messageReader.get();
		System.out.println(message.login + " : " + message.content + "\n");
	}
}
