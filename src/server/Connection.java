package server;

import java.io.IOException;

public interface Connection {
	
	void doRead() throws IOException;
	
	void doWrite() throws IOException;
	
	void silentlyClose();

}
