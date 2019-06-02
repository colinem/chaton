package client;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;


public class HTTPReader {

	private final SocketChannel sc;
	private final ByteBuffer buff;
	private final byte cr = '\r';
	private final byte lf = '\n';

	public HTTPReader(SocketChannel sc, ByteBuffer buff) {
		this.sc = sc;
		this.buff = buff;
	}

	private static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		while (bb.hasRemaining())
			if (sc.read(bb) == -1)
				return false;
		return true;
	}

	/**
	 * @return The ASCII string terminated by CRLF
	 * <p>
	 * The method assume that buff is in write mode and leave it in write-mode
	 * The method never reads from the socket as long as the buffer is not empty
	 * @throws IOException HTTPException if the connection is closed before a line could be read
	 */
	public String readLineCRLF() throws IOException {
		buff.flip();
		var line = new StringBuilder();
		byte currentByte;
		var precededByCr = false;
		while (true) {
			while (!buff.hasRemaining()) {
				if (sc.read(buff.clear()) == -1)
					throw new HTTPException();
				buff.flip();
			}
			currentByte = buff.get();
			if (precededByCr && currentByte == lf) {
				line.delete(line.length()-1, line.length());
				break;
			}
			precededByCr = currentByte == cr;
			line.append((char) currentByte);
		}
		buff.compact();
		return line.toString();
	}

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException HTTPException if the connection is closed before a header could be read
	 *                     if the header is ill-formed
	 */
	public HTTPHeader readHeader() throws IOException {
		var fields = new HashMap<String, String>();
		String firstLine = readLineCRLF();
		for (var line = readLineCRLF() ; !line.isEmpty() ; line = readLineCRLF()) {
			var tokens = line.split(": ", 2);
			if (tokens.length >= 2)
				fields.merge(tokens[0], tokens[1], (s1, s2) -> s1 + ";" + s2);
		}
		return HTTPHeader.create(firstLine, fields);
	}

	/**
	 * @param size
	 * @return a ByteBuffer in write-mode containing size bytes read on the socket
	 * @throws IOException HTTPException is the connection is closed before all bytes could be read
	 */
	public ByteBuffer readBytes(int size) throws IOException {
		var bb = ByteBuffer.allocate(size).put(buff.flip());
		if (!readFully(sc, bb))
			throw new HTTPException();
		return bb;
	}

	/**
	 * @return a ByteBuffer in write-mode containing a content read in chunks mode
	 * @throws IOException HTTPException if the connection is closed before the end of the chunks
	 *                     if chunks are ill-formed
	 */

	public ByteBuffer readChunks() throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1024);
		int size;
		while ((size = Integer.parseInt(readLineCRLF(), 16)) != 0) {
			if (bb.remaining() < size)
				bb = ByteBuffer.allocate(bb.capacity()+size).put(bb.flip());
			bb.put(readBytes(size).flip());
			readLineCRLF();
		}
		return bb;
	}


	public static void main(String[] args) throws IOException {
		Charset charsetASCII = Charset.forName("UTF-8");
		String request = "GET / HTTP/1.1\r\n"
				+ "Host: www.w3.org\r\n"
				+ "\r\n";
		SocketChannel sc;
		ByteBuffer bb;
		HTTPReader reader;
		HTTPHeader header;
		ByteBuffer content;
		
//		sc = SocketChannel.open();
//		sc.connect(new InetSocketAddress("www.w3.org", 80));
//		sc.write(charsetASCII.encode(request));
//		bb = ByteBuffer.allocate(50);
//		reader = new HTTPReader(sc, bb);
//		System.out.println(reader.readLineCRLF());
//		System.out.println(reader.readLineCRLF());
//		System.out.println(reader.readLineCRLF());
//		sc.close();

//		bb = ByteBuffer.allocate(50);
//		sc = SocketChannel.open();
//		sc.connect(new InetSocketAddress("www.w3.org", 80));
//		reader = new HTTPReader(sc, bb);
//		sc.write(charsetASCII.encode(request));
//		System.out.println(reader.readHeader());
//		sc.close();

//		bb = ByteBuffer.allocate(50);
//		sc = SocketChannel.open();
//		sc.connect(new InetSocketAddress("www.w3.org", 80));
//		reader = new HTTPReader(sc, bb);
//		sc.write(charsetASCII.encode(request));
//		header = reader.readHeader();
//		System.out.println(header);
//		content = reader.readBytes(header.getContentLength());
//		content.flip();
//		System.out.println(header.getCharset().decode(content));
//		sc.close();

		bb = ByteBuffer.allocate(50);
		request = "GET / HTTP/1.1\r\n"
				+ "Host: www.u-pem.fr\r\n"
				+ "\r\n";
		sc = SocketChannel.open();
		sc.connect(new InetSocketAddress("www.u-pem.fr", 80));
		reader = new HTTPReader(sc, bb);
		sc.write(charsetASCII.encode(request));
		header = reader.readHeader();
		System.out.println(header);
		content = reader.readChunks();
		content.flip();
		System.out.println(header.getCharset().decode(content));
		sc.close();
	}
}
