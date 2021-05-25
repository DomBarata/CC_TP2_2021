import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FSChunk {
    private int isfragmented;
    public final String tag;
    public String file;
    public byte[] data;
    public final String senderIpAddress;
    public final int senderPort;

    public FSChunk(String tag, String senderIpAddress, int senderPort, String file, byte[] data) {
        this.tag = tag;
        this.senderIpAddress = senderIpAddress;
        this.senderPort = senderPort;
        this.file = file;
        this.data = data;
        this.isfragmented = 0;
    }

    public FSChunk(String tag, String file, byte[] data) {
        this.tag = tag;
        this.senderIpAddress = "";
        this.senderPort = -1;
        this.file = file;
        this.data = data;
        this.isfragmented = 0;
    }

    public FSChunk(String tag, byte[] data) {
        this.tag = tag;
        this.senderIpAddress = "";
        this.senderPort = -1;
        this.file = "";
        this.data = data;
        this.isfragmented = 0;
    }

    public FSChunk(byte[] array){
        this.senderIpAddress = "";
        this.senderPort = -1;
        String string = new String(array);
        String[] str = string.split("::");
        this.isfragmented = Integer.parseInt(str[0]);
        this.tag = str[1];
        this.file = str[2];
        try{
            this.data = str[3].getBytes();
        }catch (ArrayIndexOutOfBoundsException e){
            this.data = "".getBytes();
        }
    }

    public FSChunk(String address, int port, byte[] array){
        this.senderIpAddress = address;
        this.senderPort = port;
        String string = new String(array);
        String[] str = string.split("::");
        this.isfragmented = Integer.parseInt(str[0]);
        this.tag = str[1];
        this.file = str[2];
        try{
            this.data = str[3].getBytes();
        }catch (ArrayIndexOutOfBoundsException e){
            this.data = "".getBytes();
        }
    }

    public int quantosPackets(int max){
        int tamFixo = 1 + 2 + this.tag.length() + 2 + this.file.length() + 4 + 2;
        float capacidade = max - tamFixo;
        float qtd;
        if(this.data.length == 0) qtd = 1;
        else qtd = this.data.length/capacidade;

        return (int) Math.ceil(qtd);
    }

    public byte[] getData(int pos, int max){ //pos começa em 0
        int estafragmentado = 1;

        if(quantosPackets(max)-1 == pos)
            estafragmentado = 0;

        String str;

        str = estafragmentado + "::" + this.tag + "::" + this.file + String.format("%04d", pos) + "::";
        int tam = max-str.length();

        byte[] frag = Arrays.copyOfRange(this.data, pos*tam, pos*tam+tam);

        str += new String(frag);

        return str.getBytes();
    }

    public void setData(List<String> ficheiros){
        String data = "";
        for (String s : ficheiros){
            data += ":/" + s;
        }
        this.data = data.getBytes();
    }

    /**
     * Método usado para fazer parse ao byteArray no chunk, quando a tag é LS
     * @return List<String>
     */
    public List<String> getDataList(){
        String data = new String(this.data);
        String[] ficheiros = data.split(":/");

        List<String> files = new ArrayList<>();

        for(String s : ficheiros){
            files.add(s);
        }

        return files;
    }

    public void complete(FSChunk aux) {
        String data = new String(this.data);

        data += new String(aux.data);

        this.data = data.getBytes();

        if(aux.isfragmented==0) {
            this.isfragmented = 0;
        }
    }

    public int getFragmentNumber(){
        int num;
        try {
            num = Integer.parseInt(this.file.substring(this.file.length() - 4));
        }catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            num = -1;
        }
        return num; //-1 means error
    }

    public void filenameClean(){
        try {
            this.file = this.file.substring(0, this.file.length() - 4);
        }catch (ArrayIndexOutOfBoundsException ignored){

        }
    }

    public String getFileClean(){
        try {
            return this.file.substring(0, this.file.length() - 4);
        }catch (ArrayIndexOutOfBoundsException e){
            return this.file;
        }
    }

    public boolean isfragmented(){
        if(this.isfragmented == 1) return true;
        else return false;
    }
}
