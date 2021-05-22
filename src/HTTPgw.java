import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPgw {
    // Socket UDP para comunicar com o 'FastFileServer'
    public static Map<String, FSChunkProtocol> socketInterno = new HashMap();
    public static Map<String,List<String>> ficheirosServer = new HashMap<>();
    private static Socket socket;
    private final static String password = "PASSWORD";

    public static void main(String[] args) throws IOException {
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
                try {
                    File file = new File("logo_spc.jpg");

                    Socket client = serversocket.accept();
                    DataInputStream clienteIn = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                    DataOutputStream clienteOut = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                    BufferedReader br = new BufferedReader(new InputStreamReader(clienteIn));
                    String httprequest = br.readLine();
                    String []request = httprequest.split(" ");
                    String ficheiro = request[1].substring(1);

                    System.out.println(ficheiro);

                    byte[] bytes = Files.readAllBytes(file.toPath());

                    clienteOut.write("HTTP/1.1 200 OK\r\n".getBytes());
                    clienteOut.write(("Content-Length: " + bytes.length  + "\r\n").getBytes());
                    clienteOut.write("Content-Type: image/jpeg;\r\n\r\n".getBytes());
                    clienteOut.write(bytes);
                    clienteOut.flush();


                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
            parser.start();


        Thread udps = new Thread(() -> {
            while(true){
                FSChunk f = protocol.receive();
                switch (f.tag){
                    case "A" :  // Autenticação by Server
                        if(!socketInterno.containsKey(f.senderIpAddress) && password.equals(new String(f.data))) {
                            try {
                                socketInterno.put(f.senderIpAddress, new FSChunkProtocol(new DatagramSocket(f.senderPort, InetAddress.getByName(f.senderIpAddress))));
                                System.out.println(f.senderIpAddress);
                            } catch (SocketException | UnknownHostException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "FS" : // File send by Server

                        break;
                    case "LR" : // List of files sent by Server
                        List<String> ficheiros = f.getDataList();
                        ficheirosServer.put(f.senderIpAddress,ficheiros);
                        break;
                    case "CLOSE" :
                        FSChunkProtocol fs = socketInterno.remove(f.senderIpAddress);
                        ficheirosServer.remove(f.senderIpAddress);
                        try {
                            fs.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "EMPTY" :
                        break;
                    default :
                        System.out.println("ERRO HTTPGW:80 (switch udps)");
                        System.out.println("TAG recebida : " + f.tag);
                                break;
                }

            }
        });
        udps.start();
    }
}
