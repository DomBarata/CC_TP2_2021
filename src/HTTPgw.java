import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPgw {
    // Socket UDP para comunicar com o 'FastFileServer'
    static Map<String, DatagramSocket> socketInterno = new HashMap();

    // TODO - Socket tcp para comunicar com o HHTP
    // recebe o pedido e envia a resposta

    public static void main(String[] args) throws UnknownHostException {
        System.out.println("Ativo em " + InetAddress.getLocalHost().getHostAddress() + " porta " + 80);
        //TODO - Conexão com o exterior

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(80);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        FSChunkProtocol protocol = new FSChunkProtocol(socket);

        while(true){
            FSChunkProtocol.Frame f = protocol.receive();

            if(!socketInterno.containsKey(f.ipAdress)) {
                try {
                    socketInterno.put(f.ipAdress, new DatagramSocket(f.port, InetAddress.getByName(f.ipAdress)));
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
