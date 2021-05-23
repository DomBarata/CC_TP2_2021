import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FSChunkProtocol implements AutoCloseable {
    public final DatagramSocket socket;
    public final int safeSize = 508;
    public final InetAddress ipDestino;
    public final int portaDestino;

    public FSChunkProtocol(DatagramSocket datagramSocket, int timedout, String ip, int port) throws UnknownHostException {
        this.socket = datagramSocket;
        try {
            this.socket.setSoTimeout(timedout);
        } catch (SocketException e) {
            socket.close();
            System.out.println("Socket fechado");
        }

        this.ipDestino = InetAddress.getByName(ip);

        this.portaDestino = port;
    }
    public FSChunkProtocol(DatagramSocket datagramSocket) throws UnknownHostException {
        this.socket = datagramSocket;

        this.ipDestino = InetAddress.getByName("");

        this.portaDestino = -1;
    }

    public FSChunkProtocol(DatagramSocket datagramSocket, String ip, int port) throws UnknownHostException {
        this.socket = datagramSocket;
        this.ipDestino = InetAddress.getByName(ip);
        this.portaDestino = port;
    }

    public void send(FSChunk frame){
        byte[][] aEnviar = fragmenta(frame);

        try {
            for(int i = 0; i < aEnviar.length; i++) { //falta ver o safe size
                DatagramPacket pedido = new DatagramPacket(aEnviar[i], aEnviar[i].length, this.ipDestino, this.portaDestino);
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
        byte[][] fragmentado = new byte[tam][];

        for(int i = 0; i < tam; i++){
            fragmentado[i] = frame.getData(i,safeSize);
        }

        return fragmentado;
    }

    public FSChunk receive() throws IOException {

        byte[] aReceber = new byte[safeSize];

        DatagramPacket pedido = new DatagramPacket(aReceber, safeSize);


        socket.receive(pedido);

        FSChunk pacote = new FSChunk(pedido.getAddress().getHostAddress(), pedido.getPort(), trim(pedido.getData()));

        while(pacote.isfragmented){
            try {
                socket.receive(pedido);
                FSChunk aux = new FSChunk(pedido.getData());
                pacote.complete(aux);
                socket.send(new DatagramPacket("mandaMais".getBytes(), "mandaMais".getBytes().length , socket.getInetAddress(), socket.getPort()));
            }catch (SocketTimeoutException E){
                socket.send(new DatagramPacket("mandaMais".getBytes(), "mandaMais".getBytes().length , socket.getInetAddress(), socket.getPort()));
            }
        }

        return pacote;
    }

    @Override
    public void close() throws Exception {
        this.socket.close();
    }

    static byte[] trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
}
