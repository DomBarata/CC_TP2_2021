import java.util.concurrent.locks.Condition;

public class Par {
    private final String fileName;
    private final Condition cond;


    public Par(String filename, Condition cond) {
        this.fileName = filename;
        this.cond = cond;
    }
    public String getFilename(){
        return this.fileName;
    }

    public Condition getCondition() {
        return this.cond;
    }

}
