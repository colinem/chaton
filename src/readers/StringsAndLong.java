package readers;

import java.util.ArrayList;

public class StringsAndLong {
    private ArrayList<String> arrayList=new ArrayList<>();
    private long aLong;

    public ArrayList<String> getArrayList() {
        return arrayList;
    }

    public void addString(String toAdd) {
        arrayList.add(toAdd);
    }

    public long getaLong() {
        return aLong;
    }

    public void setaLong(long aLong) {
        this.aLong = aLong;
    }
}
