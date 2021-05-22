import java.io.*;
import java.net.*;
import java.util.Arrays;

public class FSChunkProtocol implements AutoCloseable {

    public final DatagramSocket socket;

    public InetAddress getIp() {
        return ip;
    }

    public int getPorta() {
        return porta;
    }

    public final InetAddress ip;
    public final int porta;
    public final int safeSize = 508;

    public FSChunkProtocol(DatagramSocket datagramSocket, String ip, int port) throws UnknownHostException {
        this.socket = datagramSocket;
        this.ip = InetAddress.getByName(ip);
        this.porta = port;
    }

    public void send(FSChunk frame){
        byte[][] aEnviar = fragmenta(frame);
        try {
            for(int i = 0; i < aEnviar.length; i++) {
                DatagramPacket pedido = new DatagramPacket(aEnviar[i], safeSize, ip, porta);
                System.out.println("sending...");
                socket.send(pedido);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("files were sent!");
    }

    public void send(byte[] bytes) {
        byte[] aEnviar = new byte[0];
        DatagramPacket pedido = new DatagramPacket(aEnviar, aEnviar.length, ip, porta);
        System.out.println("sending...");
        try {
            socket.send(pedido);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("files were sent!");
    }

    public FSChunk receive(){
        byte[] aReceber = new byte[safeSize];
        DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);
        try {
            System.out.println("listening...");
            socket.receive(pedido);
            System.out.println("data received: " + Arrays.toString(pedido.getData()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new FSChunk(pedido.getAddress().getHostAddress(), pedido.getPort(), pedido.getData());
    }

    private byte[][] fragmenta(FSChunk frame) {
        int tam = frame.quantosPackets(safeSize);
        byte[][] fragmentado = new byte[tam][safeSize];

        for(int i = 0; i < tam; i++){
            fragmentado[i] = frame.getData(i,safeSize);
        }

        return fragmentado;
    }
    @Override
    public void close() throws Exception {
        this.socket.close();
    }
}
