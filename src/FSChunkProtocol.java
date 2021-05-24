import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FSChunkProtocol implements AutoCloseable {
    public final DatagramSocket socket;
    public final int safeSize = 508;
    public final InetAddress ipDestino;
    public final int portaDestino;
    private boolean isOcupied;

    public FSChunkProtocol(DatagramSocket datagramSocket, int timedout) throws UnknownHostException {
        this.socket = datagramSocket;
        try {
            this.socket.setSoTimeout(timedout);
        } catch (SocketException e) {
            socket.close();
            System.out.println("Socket fechado");
        }
        this.ipDestino = InetAddress.getByName("");
        this.portaDestino = -1;
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
            System.out.println("A enviar pacote de dados...");
            for(int i = 0; i < aEnviar.length; i++) {
                DatagramPacket pedido = new DatagramPacket(aEnviar[i], aEnviar[i].length, this.ipDestino, this.portaDestino);
                socket.send(pedido);
            }
            System.out.println("Pacote de dados enviado com sucesso");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.isOcupied = true;
    }

    private byte[][] fragmenta(FSChunk frame) {
        int tam = frame.quantosPackets(safeSize);
        byte[][] fragmentado = new byte[tam][];

        for(int i = 0; i < tam; i++){
            fragmentado[i] = frame.getData(i,safeSize);
        }

        return fragmentado;
    }

    public FSChunk receive() throws IOException { //TODO - Retransmissão

        System.out.println("À escuta...");
        byte[] aReceber = new byte[safeSize];

        DatagramPacket pedido = new DatagramPacket(aReceber, safeSize);


        socket.receive(pedido);
        int pckNum = 0;
        FSChunk pacote = new FSChunk(pedido.getAddress().getHostAddress(), pedido.getPort(), trim(pedido.getData()));
       // pckNum  = Integer.parseInt(pacote.file.substring(pacote.file.length()-4));
        while(pacote.isfragmented){
            System.out.println("A unir fragmentos de dados...");
            try {
                socket.send(new DatagramPacket(String.valueOf(pckNum+1).getBytes(), String.valueOf(pckNum+1).getBytes().length , socket.getInetAddress(), socket.getPort()));
                socket.receive(pedido);
                FSChunk aux = new FSChunk(trim(pedido.getData()));
                pckNum = Integer.parseInt(aux.file.substring(aux.file.length()-4));
                pacote.complete(aux);
            }catch (SocketTimeoutException E){
                System.out.println("Pacote de dados perdido, pedir retransmissão de fragmento...");
                socket.send(new DatagramPacket((String.valueOf(pckNum)).getBytes(), (String.valueOf(pckNum)).getBytes().length , socket.getInetAddress(), socket.getPort()));
            }
        }
        System.out.println("Pacote de dados recebido com sucesso");
        this.isOcupied = false;
        return pacote;
    }

    public boolean isOcupied(){
        return this.isOcupied;
    }

    public void setOcupied(boolean isOcupied){
        this.isOcupied = isOcupied;
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
