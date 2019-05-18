package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameMessagePrivate implements Frame {

    private final byte opcode=4;
    private final String loginSender;
    private final String loginTarget;
    private final String message;

    public FrameMessagePrivate(String loginSender, String loginTarget, String message) {
        if(loginSender.isBlank() || loginTarget.isBlank() || message.isBlank()) throw new IllegalArgumentException();
        if(!FrameWriter.testMsg(message))throw new IllegalArgumentException("too long message");
        this.loginSender = loginSender;
        this.loginTarget = loginTarget;
        this.message = message;
    }

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender() {
        return Optional.ofNullable(loginSender);
    }

    @Override
    public Optional<String> getLoginTarget() {
        return Optional.ofNullable(loginTarget);
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    @Override
    public OptionalLong getLong() {
        return OptionalLong.empty();
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder(loginSender).append(" send to you : ").append(message);
        return sb.toString();
    }

    @Override
    public ByteBuffer asBuffer() {
        ByteBuffer sender= FrameWriter.stringToBB(loginSender);
        ByteBuffer target= FrameWriter.stringToBB(loginTarget);
        ByteBuffer msg= FrameWriter.stringToBB(message);

        ByteBuffer toRet=ByteBuffer.allocate(1+sender.remaining()+target.remaining()+msg.remaining()+3*Integer.BYTES);
        toRet.put(opcode);
        toRet.put(ByteBuffer.allocateDirect(sender.remaining())).put(sender);
        toRet.put(ByteBuffer.allocateDirect(target.remaining())).put(target);
        toRet.put(ByteBuffer.allocateDirect(msg.remaining())).put(msg);
        return toRet.flip();
    }
}
