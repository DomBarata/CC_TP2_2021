import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FSChunk {
    public boolean isfragmented;
    public final String tag;
    public final String file;
    public byte[] data;
    public final String senderIpAddress;
    public final int senderPort;

    public FSChunk(String tag, String senderIpAddress, int senderPort, String file, byte[] data) {
        this.tag = tag;
        this.senderIpAddress = senderIpAddress;
        this.senderPort = senderPort;
        this.file = file;
        this.data = data;
        this.isfragmented = false;
    }

    public FSChunk(String tag, String file, byte[] data) {
        this.tag = tag;
        this.senderIpAddress = "";
        this.senderPort = -1;
        this.file = file;
        this.data = data;
        this.isfragmented = false;
    }

    public FSChunk(String tag, byte[] data) {
        this.tag = tag;
        this.senderIpAddress = "";
        this.senderPort = -1;
        this.file = "";
        this.data = data;
        this.isfragmented = false;
    }

    public FSChunk(byte[] array){
        if(array.length != 0){
            String[] str = new String(array).split("::");

            this.isfragmented = Boolean.parseBoolean(str[0]);
            this.tag = str[1];
            this.senderIpAddress = str[2];
            this.senderPort = Integer.parseInt(str[3]);
            this.file = str[2];
            this.data = str[5].getBytes();
        }else{
            this.isfragmented = false;
            this.tag = "EMPTY";
            this.senderIpAddress = "";
            this.senderPort = -1;
            this.file = "";
            this.data = new byte[0];
        }
    }

    public int quantosPackets(int max){
        int tam = max-this.file.length() + 5 + 1;
        float payload = tam-max;

        return (int) Math.ceil(this.data.length/payload);
    }

    public byte[] getData(int pos, int max){ //pos começa em 0
        boolean estafragmentado = true;

        if(quantosPackets(max)-1 == pos)
            estafragmentado = false;

        String str;

        str = estafragmentado + "::" + this.tag + "::" + this.file + String.format("%03d", pos) + "::";
        int tam = max-str.length();

        byte[] frag = Arrays.copyOfRange(this.data, pos*tam, pos*tam+tam);

        str += new String(frag);

        return str.getBytes();
    }

    public int getIndex(){
        return Integer.parseInt(this.file.substring(this.file.length()-3));
    }

    public void setData(List<String> ficheiros){
        String data = "";
        for (String s : ficheiros){
            data += "::" + s;
        }
        this.data = data.getBytes();
    }

    /**
     * Método usado para fazer parse ao byteArray no chunk, quando a tag é LS
     * @return List<String>
     */
    public List<String> getDataList(){
        String data = new String(this.data);
        String[] ficheiros = data.split("::");

        List<String> files = new ArrayList<>();

        for(String s : ficheiros){
            files.add(s);
        }

        return files;
    }
}
