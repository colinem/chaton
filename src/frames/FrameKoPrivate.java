package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameKoPrivate implements Frame {
    private byte opcode=7;
    private final String login_requester;
    private final String login_target;

    public FrameKoPrivate(FrameRequestPrivate request) {
        this.login_requester = request.getLoginSender().toString();
        this.login_target = request.getLoginTarget().toString();
    }

    public FrameKoPrivate(String s, String s1) {
        this.login_requester = s;
        this.login_target = s1;
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
        ByteBuffer requester= StringToBbManager.stringToBB(login_requester);
        ByteBuffer target= StringToBbManager.stringToBB(login_target);
        ByteBuffer toRet=ByteBuffer.allocate(1+requester.remaining()+target.remaining());
        toRet.put(opcode);
        toRet.put(requester);
        toRet.put(target);
        return toRet.flip();
    }
}
