public class Par {
    private int a;
    private int b;

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
