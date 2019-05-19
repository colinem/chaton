package readers;

import java.nio.ByteBuffer;

import frames.*;

public class TestReader {

    public static void main(String[] args) {
    	var bb = ByteBuffer.allocate(1_024);
    	
        FrameLogin frameLogin=new FrameLogin("mon pseudo");
        bb.put(frameLogin.asBuffer().flip());
        Reader frameReader = new FrameReader(bb);
        Reader.ProcessStatus ok = Reader.ProcessStatus.REFILL;
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        Frame frame= (Frame) frameReader.get();
        System.out.println(frame.getLoginSender().get());

        ok = Reader.ProcessStatus.REFILL;
        FrameLoginAccepted frameLoginAccepted=new FrameLoginAccepted();
        bb.put(frameLoginAccepted.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println("1 : "+frame.getOpcode());

        ok = Reader.ProcessStatus.REFILL;
        FrameLoginRefused frameLoginRefused=new FrameLoginRefused();
        bb.put(frameLoginRefused.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println("2 : "+frame.getOpcode());

        ok = Reader.ProcessStatus.REFILL;
        FrameMessage frameMessage=new FrameMessage("mon pseudo", "mon message a tout les copains");
        bb.put(frameMessage.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println(frame);

        ok = Reader.ProcessStatus.REFILL;
        FrameMessagePrivate frameMessagePrivate=new FrameMessagePrivate("Gimli","Legolas", "that still count's as one");
        bb.put(frameMessagePrivate.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println(frame +" to "+ frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameRequestPrivate frameRequestPrivate=new FrameRequestPrivate("Gimli","Legolas");
        bb.put(frameRequestPrivate.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println(frame +" to "+ frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameOkPrivate frameOKPrivate=new FrameOkPrivate(frameRequestPrivate);
        bb.put(frameOKPrivate.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());
        System.out.print(frame.getLoginSender());
        System.out.println(frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameKoPrivate frameKoPrivate=new FrameKoPrivate(frameRequestPrivate);
        bb.put(frameKoPrivate.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());
        System.out.print(frame.getLoginSender());
        System.out.println(frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameIdPrivate frameIDPrivate=new FrameIdPrivate(frameRequestPrivate,  15);
        bb.put(frameIDPrivate.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());
        System.out.print(frame.getLoginSender());
        System.out.print(frame.getLoginTarget());
        System.out.println(frame.getLong());

        ok = Reader.ProcessStatus.REFILL;
        FrameLoginPrivate frameLoginPrivate=new FrameLoginPrivate(15);
        bb.put(frameLoginPrivate.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());
        System.out.print(frame.getLoginSender());
        System.out.print(frame.getLoginTarget());
        System.out.println(frame.getLong());

        ok = Reader.ProcessStatus.REFILL;
        FrameEstablished frameEstablished=new FrameEstablished();
        bb.put(frameEstablished.asBuffer().flip());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());


    }
}
