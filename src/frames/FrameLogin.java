package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

import visitors.PrivateConnectionVisitor;
import visitors.PublicConnectionVisitor;

public class FrameLogin implements Frame {

    private final byte opcode=0;
    private final String login;

    public FrameLogin(String login) {
        this.login = login;
    }

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender(){
        return Optional.ofNullable(login);
    }

    @Override
    public Optional<String> getLoginTarget() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMessage(){
        return Optional.empty();
    }

    @Override
    public OptionalLong getLong() {
        return OptionalLong.empty();
    }

    @Override
    public ByteBuffer asBuffer() {
    	var loginBB = StringToBbManager.stringToBB(login);
        return ByteBuffer.allocate(Byte.BYTES + loginBB.capacity())
        		.put(opcode)
        		.put(loginBB);
    }
    
	@Override
	public void accept(PublicConnectionVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(PrivateConnectionVisitor visitor) {
		// DO NOTHING
	}
}
