package frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FrameWriter {

    public static ByteBuffer stringToBB(String string) {
        ByteBuffer bbString= StandardCharsets.UTF_8.encode(string);
        ByteBuffer toRet=ByteBuffer.allocate(bbString.remaining()+1);
        toRet.put(ByteBuffer.allocateDirect(bbString.remaining()));
        toRet.put(bbString);
        return toRet.flip();
    }

    public static boolean testMsg(String string) {
        return StandardCharsets.UTF_8.encode(string).remaining()<=1024;
    }
}
