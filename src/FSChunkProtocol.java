import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FSChunkProtocol implements AutoCloseable {

    private final DatagramSocket socket;

    public FSChunkProtocol(DatagramSocket datagramSocket){
        this.socket = datagramSocket;
    }

    public void send(FSChunk frame){
        byte[][] aEnviar = fragmenta(frame);

        try {
            for(int i = 0; i < aEnviar.length; i++) {
                DatagramPacket pedido = new DatagramPacket(aEnviar[i], 1024, socket.getInetAddress(), socket.getPort());
                socket.send(pedido);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[][] fragmenta(FSChunk frame) {
        int tam = frame.quantosPackets(1024);
        byte[][] fragmentado = new byte[tam][1024];

        for(int i = 0; i < tam; i++){
            fragmentado[i] = frame.getData(i,1024);
        }

        return fragmentado;
    }

    public FSChunk receive(){
        byte[] aReceber = new byte[1024];

        DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);

        try {
            socket.receive(pedido);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new FSChunk(pedido.getAddress().getHostAddress(), pedido.getPort(), pedido.getData());
    }


    @Override
    public void close() throws Exception {
        this.socket.close();
    }

    public void send(byte[] bytes) {

    }
}
