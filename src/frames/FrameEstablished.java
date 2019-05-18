package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameEstablished implements Frame {

    private final byte opcode=10;

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
        return ByteBuffer.allocate(1).put(opcode).flip();
    }

    @Override
    public String toString(){
        return "connection established";
    }
}
