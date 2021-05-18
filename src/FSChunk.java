import java.util.Arrays;

public class FSChunk {
    public final String ipAdress;
    public final int port;
    public final String file;
    public byte[] data;
    public Par par;
    public boolean isfragmented;


    public FSChunk(String ipAdress, int port, String file,  int de, int quanto, byte[] data) {
        this.ipAdress = ipAdress;
        this.port = port;
        this.file = file;
        this.data = data;
        this.par = new Par(de,quanto);
    }

    public FSChunk(String ip, int port, byte[] array){
        this.ipAdress = ip;
        this.port = port;

        if(array.length != 0){
            String[] str = new String(array).split("::");

            this.isfragmented = Boolean.parseBoolean(str[0]);
            this.file = str[1];
            this.data = str[2].getBytes();
        }else{
            this.file = "";
            this.data = new byte[0];
        }
    }

/*    public byte[] getBytes(){
        String str;

        str =   this.file + "::" +
                new String(data);

        return str.getBytes();
    }*/

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

        str = estafragmentado + this.file + String.format("%03d", pos) + "::";
        int tam = max-str.length();

        byte[] frag = Arrays.copyOfRange(this.data, pos*tam, pos*tam+tam);

        str += new String(frag);

        return str.getBytes();
    }

    public boolean putData(byte[] data){
        return true;
    }

}
