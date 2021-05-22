import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HTTPgw {
    // Socket UDP para comunicar com o 'FastFileServer'
    static Map<String, FSChunkProtocol> socketInterno = new HashMap();
    private static Socket socket;

    // TODO - Socket tcp para comunicar com o HTTP
    // recebe o pedido e envia a resposta


    public static void main(String[] args) throws IOException {

        //TODO - Conexão com o exterior
        //ServerSocket serversocket;
        //serversocket = new ServerSocket(8080);

        DatagramSocket socket = new DatagramSocket(8888);
        String ip = InetAddress.getLocalHost().getHostAddress();
        int port = socket.getLocalPort();
        FSChunkProtocol protocol = new FSChunkProtocol(socket,ip,port);

        System.out.println("Ativo em " + ip + " " + port);

        //i) fazer o parsing do pedido HTTP GET para extração do nome do ficheiro solicitado;
        //ii) pedir os metadados desse ficheiro a um ou mais dos servidores FastFileSrv


        Thread parser = new Thread(() -> {
            try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost",8080),0);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
            while (true) {
                //try {
                /*
                    Socket client = serversocket.accept();
                    System.out.println("connected");
                    DataInputStream clienteIn = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                    DataOutputStream clienteOut = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
*/


                    //String httprequest = clienteIn.readUTF();
                    //String[] lista = httprequest.split("\n");
                    //String[] pedido = lista[0].split(" ");
                    //System.out.println(pedido[1]);

                    //AtomicInteger i = new AtomicInteger(0);
                    //socketInterno.forEach((key, value) -> {
                    //    FSChunk chunk = new FSChunk(key, value.getPorta(), pedido[1], i.incrementAndGet(), socketInterno.size(), "".getBytes());
                    //    value.send(chunk);
                    //});

               // } catch (IOException e) {
                //    e.printStackTrace();
                //}
            }
        });
        parser.setName("parser");
        parser.start();

        Thread udp = new Thread(() -> {
            while(true){
                FSChunk f = protocol.receive();
                System.out.println("teste");
                if(!socketInterno.containsKey(f.ipAdress)) {
                    try {
                        socketInterno.put(f.ipAdress, new FSChunkProtocol(new DatagramSocket(),f.ipAdress,f.port));
                        System.out.println(f.ipAdress);
                    } catch (SocketException | UnknownHostException e) {
                        e.printStackTrace();
                    }
                }else{

                }
            }
        });
        udp.setName("udp");
        udp.start();
    }
}
