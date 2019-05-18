package frames;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

public class FrameLogin implements Frame {

<<<<<<< HEAD
    private final byte opcode=0;
    private final String login;

    public FrameLogin(String login) {
        this.login = login;
    }
=======
    private final byte opcode = 0;
    private final String login;
    
    public FrameLogin(String login) {
		this.login = login;
	}
>>>>>>> f6814e79a04d9f605e3f23fbbccc54d45155621a

    @Override
    public byte getOpcode() {
        return opcode;
    }

    @Override
    public Optional<String> getLoginSender(){
<<<<<<< HEAD
        return Optional.ofNullable(login);
=======
        return Optional.of(login);
>>>>>>> f6814e79a04d9f605e3f23fbbccc54d45155621a
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
<<<<<<< HEAD
    public ByteBuffer getBuffer() {
        ByteBuffer log= StringToBbManager.stringToBB(login);
        ByteBuffer toRet= ByteBuffer.allocate(1+log.remaining());
        toRet.put(opcode);
        toRet.put(log);
        return toRet.flip();
=======
    public ByteBuffer asBuffer() {
        return ByteBuffer.allocate(Byte.BYTES)
        		.put(opcode)
        		.put(StringToBbManager.stringToBB(login))
        		.flip();
>>>>>>> f6814e79a04d9f605e3f23fbbccc54d45155621a
    }
}
