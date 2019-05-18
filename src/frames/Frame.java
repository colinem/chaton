package frames;


import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public interface Frame {
    byte getOpcode();
    Optional<String> getLoginSender();
    Optional<String> getLoginTarget();
    Optional<String> getMessage();
    OptionalLong getLong();
    ByteBuffer getBuffer();
    @Override
    String toString();
}
