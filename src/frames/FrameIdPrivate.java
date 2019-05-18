package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameIdPrivate implements Frame {
    private byte opcode=8;
    private final String login_requester;
    private final String login_target;
    private final long connect_id;

    public FrameIdPrivate(FrameRequestPrivate request, long connect_id) {
        this.login_requester = request.getLoginSender().toString();
        this.login_target = request.getLoginTarget().toString();
        this.connect_id= connect_id;
    }
    public FrameIdPrivate(String s, String s1, long connect_id) {
        this.login_requester = s;
        this.login_target = s1;
        this.connect_id= connect_id;
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
        return OptionalLong.of(connect_id);
    }

    @Override
    public ByteBuffer asBuffer() {
        ByteBuffer requester= StringToBbManager.stringToBB(login_requester);
        ByteBuffer target= StringToBbManager.stringToBB(login_target);
        ByteBuffer toRet=ByteBuffer.allocate(1+requester.remaining()+target.remaining()+2*Integer.BYTES+Long.BYTES);
        toRet.put(opcode);
        toRet.put(requester);
        toRet.put(target);
        toRet.putLong(connect_id);
        return toRet.flip();
    }
}
