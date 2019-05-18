package readers;

import frames.*;

import java.nio.ByteBuffer;

public class FrameReader implements Reader{


    private enum State { DONE, WAITING_ID, WAITING_CONTENT, ERROR };

    private final ByteBuffer bb;
    private State state = State.WAITING_ID;
    private int ID;
    private FrameReaderAux frameReaderAux=null;
    private Frame frame;
    public FrameReader(ByteBuffer bb) {
        this.bb = bb;

    }

    @Override
    public ProcessStatus process() {
        try {
            bb.flip();
            switch (state) {
                case WAITING_ID:
                    if (bb.remaining() < Byte.BYTES)
                        return ProcessStatus.REFILL;
                    ID = bb.get();
                    bb.compact();
                    state = State.WAITING_CONTENT;
                    switch (ID){
                        case 0:
                            frameReaderAux=new FrameReaderAux(bb,1,false);
                            break;
                        case 3:
                        case 5:
                        case 6:
                        case 7:
                            frameReaderAux=new FrameReaderAux(bb,2,false);
                            break;
                        case 4:
                            frameReaderAux=new FrameReaderAux(bb,3,false);
                            break;
                        case 8:
                            frameReaderAux=new FrameReaderAux(bb,2,true);
                            break;
                        case 9:
                            frameReaderAux=new FrameReaderAux(bb,0,true);
                            break;

                    }
                case WAITING_CONTENT:
                    if (state==State.DONE || state==State.ERROR) {
                        throw new IllegalStateException();
                    }
                    Reader.ProcessStatus status = frameReaderAux.process();
                    switch (status){
                        case DONE:
                            StringsAndLong stringsAndLong= (StringsAndLong) frameReaderAux.get();

                            switch (ID){
                                case 0:
                                    frame=new FrameLogin(stringsAndLong.getArrayList().get(0));
                                    break;
                                case 1:
                                    frame=new FrameLoginAccepted();
                                    break;
                                case 2:
                                    frame=new FrameLoginRefused();
                                    break;
                                case 3:
                                    frame=new FrameMessage(stringsAndLong.getArrayList().get(0),stringsAndLong.getArrayList().get(1));
                                    break;
                                case 4:
                                    frame=new FrameMessagePrivate(stringsAndLong.getArrayList().get(0),stringsAndLong.getArrayList().get(1),stringsAndLong.getArrayList().get(2));
                                    break;
                                case 5:
                                    frame=new FrameRequestPrivate(stringsAndLong.getArrayList().get(0),stringsAndLong.getArrayList().get(1));
                                    break;
                                case 6:
                                    frame=new FrameOkPrivate(stringsAndLong.getArrayList().get(0),stringsAndLong.getArrayList().get(1));
                                    break;
                                case 7:
                                    frame=new FrameKoPrivate(stringsAndLong.getArrayList().get(0),stringsAndLong.getArrayList().get(1));
                                    break;
                                case 8:
                                    frame=new FrameIdPrivate(stringsAndLong.getArrayList().get(0),stringsAndLong.getArrayList().get(1), stringsAndLong.getaLong());
                                    break;
                                case 9:
                                    frame=new FrameLoginPrivate(stringsAndLong.getaLong());
                                    break;
                                case 10:
                                    frame=new FrameEstablished();

                            }
                            return  ProcessStatus.DONE;
                        case REFILL:
                            return  ProcessStatus.REFILL;
                        case ERROR:
                            return  ProcessStatus.ERROR;

                    }
                default:
                    throw new IllegalStateException();
            }
        } finally {
            bb.compact();
        }
    }

    @Override
    public Object get() {
        if (state!=State.DONE) {
            throw new IllegalStateException();
        }
        return frame;
    }

    @Override
    public void reset() {
        frameReaderAux=null;
        state = State.WAITING_ID;
        frame=null;
    }
}
