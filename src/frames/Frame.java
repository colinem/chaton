package frames;


import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

import visitors.PrivateConnectionVisitor;
import visitors.PublicConnectionVisitor;

public interface Frame {
    byte getOpcode();
    Optional<String> getLoginSender();
    Optional<String> getLoginTarget();
    Optional<String> getMessage();
    OptionalLong getLong();
    ByteBuffer asBuffer();
    public void accept(PublicConnectionVisitor visitor);
    public void accept(PrivateConnectionVisitor visitor);
}
