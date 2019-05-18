package server;

import frames.FrameLogin;
import frames.FrameMessage;
import frames.FrameMessagePrivate;

public interface Visitor {
	
	public void visit(FrameLogin frameLogin);
	public void visit(FrameMessage frameMessage);
	public void visit(FrameMessagePrivate frameMessagePrivate);
	
}
