package readers;

import frames.Frame;
import frames.FrameLogin;

public class TestReader {

    public static void main(String[] args) {
        FrameLogin frameLogin=new FrameLogin("mon pseudo");
        Reader frameReader = new FrameReader(frameLogin.asBuffer());
        Reader.ProcessStatus ok = Reader.ProcessStatus.REFILL;
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
            //System.out.println("ok");
        }
       
        
        Frame frame= (Frame) frameReader.get();
        System.out.println(frame.getLoginSender());
    }
}
