package readers;


import java.nio.ByteBuffer;

public class FrameReaderAux implements Reader {


    private enum State {DONE,WAITING,ERROR};
    private final ByteBuffer bb;
    private State state = State.WAITING;
    private int step =0;
    private StringsAndLong stringsAndLong=new StringsAndLong();
    private StringReader stringReader;
    private LongReader longReader;
    private int numberOfStrings;
    private boolean isThereLong;


    public FrameReaderAux(ByteBuffer bb,int numberOfStrings, boolean isThereLong) {
        this.bb = bb;
        stringReader=new StringReader(this.bb);
        longReader=new LongReader(this.bb);
        this.isThereLong = isThereLong;
        this.numberOfStrings=numberOfStrings;

    }



    @Override
    public ProcessStatus process() {

        if (state==State.DONE || state==State.ERROR) {
            throw new IllegalStateException();
        }
        while (step<numberOfStrings){
            Reader.ProcessStatus status = stringReader.process();
            switch (status){
                case DONE:
                    String current= (String) stringReader.get();
                    stringsAndLong.addString(current);
                    stringReader.reset();
                    step++;
                    break;
                case REFILL:
                    return  ProcessStatus.REFILL;
                case ERROR:
                    return  ProcessStatus.ERROR;

            }
        }

        if(isThereLong) {
            Reader.ProcessStatus status = longReader.process();
            switch (status) {
                case DONE:
                    stringsAndLong.setaLong((long) longReader.get());
                    longReader.reset();
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    return ProcessStatus.ERROR;
            }
        }

        return ProcessStatus.DONE;


    }

    @Override
    public Object get() {
        if (state!=State.DONE) {
            throw new IllegalStateException();
        }
        return stringsAndLong;
    }

    @Override
    public void reset() {
        state=State.WAITING;
        step =0;
        stringReader.reset();
        longReader.reset();
        stringsAndLong=new StringsAndLong();
    }


}
