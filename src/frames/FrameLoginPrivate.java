package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

import visitors.PrivateConnectionVisitor;
import visitors.PublicConnectionVisitor;

public class FrameLoginPrivate implements Frame {

    private final byte opcode=9;
    private final long connect_id;

    public FrameLoginPrivate(FrameOkPrivate ok_private) {
        this.connect_id = ok_private.getLong().getAsLong();
    }

    public FrameLoginPrivate(long getaLong) {
        this.connect_id=getaLong;
    }

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getLoginTarget() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.empty();
    }

    @Override
    public OptionalLong getLong() {
        return OptionalLong.of(connect_id);
    }

    @Override
    public ByteBuffer asBuffer() {
        ByteBuffer toRet=ByteBuffer.allocate(1+Long.BYTES);
        toRet.put(opcode);
        toRet.putLong(connect_id);
        return toRet;
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
