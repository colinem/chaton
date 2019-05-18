package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameRequestPrivate implements Frame {

    private final byte opcode=5;
    private final String login_requester;
    private final String login_target;

    public FrameRequestPrivate(String login_requester, String login_target) {
        if(login_requester.isBlank() || login_target.isBlank()) throw new IllegalArgumentException();
        this.login_requester = login_requester;
        this.login_target = login_target;
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
    public ByteBuffer asBuffer() {
        ByteBuffer requester= FrameWriter.stringToBB(login_requester);
        ByteBuffer target= FrameWriter.stringToBB(login_target);
        ByteBuffer toRet=ByteBuffer.allocate(1+requester.remaining()+target.remaining()+2*Integer.BYTES);
        toRet.put(opcode);
        toRet.put(ByteBuffer.allocateDirect(requester.remaining())).put(requester);
        toRet.put(ByteBuffer.allocateDirect(target.remaining())).put(target);
        return toRet.flip();
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder(login_requester).append(" want to have a private connection with you");
        return sb.toString();
    }
}
