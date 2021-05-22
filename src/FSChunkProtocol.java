import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class FSChunkProtocol implements AutoCloseable {
    public final DatagramSocket socket;
    public final int safeSize = 508;

    public FSChunkProtocol(DatagramSocket datagramSocket){
        this.socket = datagramSocket;
    }

    public void send(FSChunk frame){
        byte[][] aEnviar = fragmenta(frame);

        try {
            for(int i = 0; i < aEnviar.length; i++) {
                DatagramPacket pedido = new DatagramPacket(aEnviar[i], safeSize, socket.getInetAddress(), 8888/*socket.getPort()*/);
                socket.send(pedido);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

/*
    public void send(byte[] bytes) {
        byte[] aEnviar = new byte[0];
        DatagramPacket pedido = new DatagramPacket(aEnviar, aEnviar.length, socket.getInetAddress(), socket.getPort());
        try {
            socket.send(pedido);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/

    private byte[][] fragmenta(FSChunk frame) {
        int tam = frame.quantosPackets(safeSize);
        byte[][] fragmentado = new byte[tam][safeSize];

        for(int i = 0; i < tam; i++){
            fragmentado[i] = frame.getData(i,safeSize);
        }

        return fragmentado;
    }

    public FSChunk receive(){
        byte[] aReceber = new byte[safeSize];

        DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);

        try {
            socket.receive(pedido);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FSChunk pacote = new FSChunk(pedido.getData());
        List<FSChunk> lista = new ArrayList<>();
        lista.add(pacote.getIndex(), pacote);

        while(pacote.isfragmented){
            try {
                socket.receive(pedido);
            } catch (IOException e) {
                e.printStackTrace();
            }

            pacote = new FSChunk(pedido.getData());
            lista.add(pacote.getIndex(), pacote);
        }

        return new FSChunk(pedido.getData());
    }

    @Override
    public void close() throws Exception {
        this.socket.close();
    }

}
