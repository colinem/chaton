package readers;

import frames.*;

public class TestReader {

    public static void main(String[] args) {
        FrameLogin frameLogin=new FrameLogin("mon pseudo");
        Reader frameReader = new FrameReader(frameLogin.asBuffer());
        Reader.ProcessStatus ok = Reader.ProcessStatus.REFILL;
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        Frame frame= (Frame) frameReader.get();
        System.out.println(frame.getLoginSender());

        ok = Reader.ProcessStatus.REFILL;
        FrameLoginAccepted frameLoginAccepted=new FrameLoginAccepted();
        frameReader = new FrameReader(frameLoginAccepted.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println("1 : "+frame.getOpcode());

        ok = Reader.ProcessStatus.REFILL;
        FrameLoginRefused frameLoginRefused=new FrameLoginRefused();
        frameReader = new FrameReader(frameLoginRefused.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println("2 : "+frame.getOpcode());

        ok = Reader.ProcessStatus.REFILL;
        FrameMessage frameMessage=new FrameMessage("mon pseudo", "mon message a tout les copains");
        frameReader = new FrameReader(frameMessage.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println(frame);

        ok = Reader.ProcessStatus.REFILL;
        FrameMessagePrivate frameMessagePrivate=new FrameMessagePrivate("Gimli","Legolas", "that still count's as one");
        frameReader = new FrameReader(frameMessagePrivate.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println(frame +" to "+ frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameRequestPrivate frameRequestPrivate=new FrameRequestPrivate("Gimli","Legolas");
        frameReader = new FrameReader(frameRequestPrivate.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.println(frame +" to "+ frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameOkPrivate frameOKPrivate=new FrameOkPrivate(frameRequestPrivate);
        frameReader = new FrameReader(frameOKPrivate.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());
        System.out.print(frame.getLoginSender());
        System.out.println(frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameKoPrivate frameKoPrivate=new FrameKoPrivate(frameRequestPrivate);
        frameReader = new FrameReader(frameKoPrivate.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());
        System.out.print(frame.getLoginSender());
        System.out.println(frame.getLoginTarget());

        ok = Reader.ProcessStatus.REFILL;
        FrameIdPrivate frameIDPrivate=new FrameIdPrivate(frameRequestPrivate,  15);
        frameReader = new FrameReader(frameIDPrivate.asBuffer());
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
        frameReader = new FrameReader(frameLoginPrivate.asBuffer());
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
        frameReader = new FrameReader(frameEstablished.asBuffer());
        while (ok!= Reader.ProcessStatus.DONE){
            ok=frameReader.process();
        }
        frame= (Frame) frameReader.get();
        System.out.print(frame.getOpcode());


    }
}
