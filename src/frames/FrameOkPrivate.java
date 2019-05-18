package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameOkPrivate implements Frame {
    private byte opcode=6;
    private final String login_requester;
    private final String login_target;

    public FrameOkPrivate(FrameRequestPrivate request) {
        this.login_requester = request.getLoginSender().toString();
        this.login_target = request.getLoginTarget().toString();
    }

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender() {
        return Optional.ofNullable(login_requester);
    }

    @Override
    public Optional<String> getLoginTarget() {
        return Optional.ofNullable(login_target);
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.empty();
    }

    @Override
    public OptionalLong getLong() {
        return OptionalLong.empty();
    }

    @Override
    public ByteBuffer getBuffer() {
        ByteBuffer requester= FrameWriter.stringToBB(login_requester);
        ByteBuffer target= FrameWriter.stringToBB(login_target);
        ByteBuffer toRet=ByteBuffer.allocate(1+requester.remaining()+target.remaining()+2*Integer.BYTES);
        toRet.put(opcode);
        toRet.put(ByteBuffer.allocateDirect(requester.remaining())).put(requester);
        toRet.put(ByteBuffer.allocateDirect(target.remaining())).put(target);
        return toRet.flip();
    }
}
