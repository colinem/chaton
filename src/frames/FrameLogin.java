package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

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
        ByteBuffer log= StringToBbManager.stringToBB(login);
        ByteBuffer toRet= ByteBuffer.allocate(1+log.remaining());
        toRet.put(opcode);
        toRet.put(log);
        return toRet.flip();
    }
}
