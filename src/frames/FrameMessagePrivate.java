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
        if(!StringToBbManager.testMsg(message))throw new IllegalArgumentException("too long message");
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
    public ByteBuffer getBuffer() {
        ByteBuffer sender= StringToBbManager.stringToBB(loginSender);
        ByteBuffer target= StringToBbManager.stringToBB(loginTarget);
        ByteBuffer msg= StringToBbManager.stringToBB(message);

        ByteBuffer toRet=ByteBuffer.allocate(1+sender.remaining()+target.remaining()+msg.remaining());
        toRet.put(opcode);
        toRet.put(sender);
        toRet.put(target);
        toRet.put(msg);
        return toRet.flip();
    }
}
