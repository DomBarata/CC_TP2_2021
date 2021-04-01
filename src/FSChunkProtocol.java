import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class FSChunkProtocol implements AutoCloseable {
    public static class Frame {
        public final String ipAdress;
        public final int port;
        public final String file;
        public final byte[] data;
        public Frame(String ipAdress, int port, String file, byte[] data) {
            this.ipAdress = ipAdress;
            this.port = port;
            this.file = file;
            this.data = data;
        }

        public Frame(byte[] array){
            String[] str = new String(array).split("::");

            this.ipAdress = str[0];
            this.port = Integer.parseInt(str[1]);
            this.file = str[2];
            this.data = str[3].getBytes();
        }

        public byte[] getBytes(){
            String str;

            str =   this.ipAdress + "::" +
                    this.port + "::" +
                    this.file + "::" +
                    new String(data);

            return str.getBytes();
        }
    }

    private final DatagramSocket socket;

    public FSChunkProtocol(DatagramSocket datagramSocket){
        this.socket = datagramSocket;
    }

    public void send(Frame frame){
        DatagramPacket pedido = new DatagramPacket(frame.getBytes(), frame.getBytes().length, socket.getInetAddress(), socket.getPort());

        try {
            socket.send(pedido);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Frame receive(){
        byte[] aReceber = new byte[1024];

        DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);

        try {
            socket.receive(pedido);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Frame(pedido.getData());
    }


    @Override
    public void close() throws Exception {
        this.socket.close();
    }
}
