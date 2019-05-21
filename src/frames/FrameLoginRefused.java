package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

import visitors.PrivateConnectionVisitor;
import visitors.PublicConnectionVisitor;

public class FrameLoginRefused implements Frame {

    private final byte opcode=2;

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender(){
        return Optional.empty();
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
        return ByteBuffer.allocate(1).put(opcode);
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
