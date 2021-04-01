import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class FastFileServer {
    public static void main(String[] args) {
        String ip = args[1];
        int port = Integer.parseInt(args[2]);
        DatagramSocket socket = null;

        try {
             socket = new DatagramSocket(port, InetAddress.getByName(ip));
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

        if(socket == null) { System.out.println("Erro a conectar com o gateway"); return;}

        FSChunkProtocol protocol = new FSChunkProtocol(socket);
        protocol.receive();

        try {
            protocol.send(new FSChunkProtocol.Frame(InetAddress.getLocalHost().getHostAddress(), 80, "", "".getBytes()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }
}
