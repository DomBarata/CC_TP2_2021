import java.net.InetAddress;
import java.util.Arrays;

public class FSChunk {
    public final String ipAdress;
    public final int port;
    public final String file;
    public final byte[] data;


    public FSChunk(String ipAdress, int port, String file, byte[] data) {
        this.ipAdress = ipAdress;
        this.port = port;
        this.file = file;
        this.data = data;
    }

    public FSChunk(String ip, int port, byte[] array){
        String[] str = new String(array).split("::");

        this.ipAdress = ip;
        this.port = port;
        this.file = str[0];
        this.data = str[1].getBytes();
    }

    public byte[] getBytes(){
        String str;

        str =   this.file + "::" +
                new String(data);

        return str.getBytes();
    }

    public int quantosPackets(int max){
        int tam = max-this.file.length() + 5;
        float payload = tam-max;

        return (int) Math.ceil(this.data.length/payload);
    }

    public byte[] getData(int pos, int max){ //pos começa em 0
        String str;

        str = this.file + String.format("%03d", pos) + "::";
        int tam = max-str.length();

        byte[] frag = Arrays.copyOfRange(this.data, pos*tam, pos*tam+tam);

        str += new String(frag);

        return str.getBytes();
    }
}
