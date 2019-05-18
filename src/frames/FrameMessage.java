package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameMessage implements Frame {

    private final byte opcode=3;
    private final String login;
    private final String message;

    public FrameMessage(String login, String message) {
        if(login.isBlank() || message.isBlank()) throw new IllegalArgumentException();
        if(!FrameWriter.testMsg(message))throw new IllegalArgumentException("too long message");
        this.login = login;
        this.message = message;
    }

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender() {
        return Optional.ofNullable(login);
    }

    @Override
    public Optional<String> getLoginTarget() {
        return Optional.empty();
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
        StringBuilder sb = new StringBuilder(login).append(" send to all : ").append(message);
        return sb.toString();
    }

    @Override
    public ByteBuffer asBuffer() {
        ByteBuffer log= FrameWriter.stringToBB(login);
        ByteBuffer msg= FrameWriter.stringToBB(message);
        ByteBuffer toRet=ByteBuffer.allocate(1+log.remaining()+msg.remaining()+2*Integer.BYTES);
        toRet.put(opcode);
        toRet.put(ByteBuffer.allocateDirect(log.remaining())).put(log);
        toRet.put(ByteBuffer.allocateDirect(msg.remaining())).put(msg);
        return toRet.flip();
    }
}
