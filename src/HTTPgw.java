import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HTTPgw {
    // Socket UDP para comunicar com o 'FastFileServer'
    static Map<String, FSChunkProtocol> socketInterno = new HashMap();
    private static Socket socket;

    // TODO - Socket tcp para comunicar com o HHTP
    // recebe o pedido e envia a resposta


    public static void main(String[] args) throws IOException {

        //TODO - Conexão com o exterior
        ServerSocket serversocket;
        serversocket = new ServerSocket(8080);

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(8888);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        FSChunkProtocol protocol = new FSChunkProtocol(socket);

        System.out.println("Ativo em " + InetAddress.getLocalHost().getHostAddress() + " " + socket.getLocalPort());


        //i) fazer o parsing do pedido HTTP GET para extração do nome do ficheiro solicitado;
        //ii) pedir os metadados desse ficheiro a um ou mais dos servidores FastFileSrv


            Thread parser = new Thread(() -> {
                while (true) {
                    try {
                        Socket client = serversocket.accept();
                        DataInputStream clienteIn = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                        DataOutputStream clienteOut = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));

                        String httprequest = clienteIn.readUTF();
                        String[] lista = httprequest.split("\n");
                        String[] pedido = lista[0].split(" ");

                        AtomicInteger i = new AtomicInteger(0);
                        socketInterno.forEach((key, value) -> {
                            FSChunk chunk = new FSChunk(key, value.socket.getPort(), pedido[1], i.incrementAndGet(), socketInterno.size(), "".getBytes());
                            value.send(chunk);
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            parser.start();

        Thread udps = new Thread(() -> {
            while(true){
                FSChunk f = protocol.receive();
                System.out.println("teste");
                if(!socketInterno.containsKey(f.ipAdress)) {
                    try {
                        socketInterno.put(f.ipAdress, new FSChunkProtocol(new DatagramSocket(f.port, InetAddress.getByName(f.ipAdress))));
                        System.out.println(f.ipAdress);
                    } catch (SocketException | UnknownHostException e) {
                        e.printStackTrace();
                    }
                }else{

                }
            }
        });
        udps.start();
    }
}
