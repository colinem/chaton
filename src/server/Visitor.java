package server;

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

public interface Visitor {
	
	public void visit(FrameLogin frameLogin);
	public void visit(FrameMessage frameMessage);
	public void visit(FrameMessagePrivate frameMessagePrivate);
	public void visit(FrameEstablished frameEstablished);
	public void visit(FrameIdPrivate frameIdPrivate);
	public void visit(FrameKoPrivate frameKoPrivate);
	public void visit(FrameLoginAccepted frameLoginAccepted);
	public void visit(FrameLoginPrivate frameLoginPrivate);
	public void visit(FrameLoginRefused frameLoginRefused);
	public void visit(FrameOkPrivate frameOkPrivate);
	public void visit(FrameRequestPrivate frameRequestPrivate);
	
}
