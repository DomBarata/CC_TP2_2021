import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class HTTPgw {
    // Socket UDP para comunicar com o 'FastFileServer'
    public static Map<String, FSChunkProtocol> socketInterno = new HashMap();
    public static Map<String,List<String>> ficheirosServer = new HashMap<>();
    private final static String password = "PASSWORD";

    public static void main(String[] args) throws IOException {
        ArrayList<String> filename = new ArrayList<>();
        Map<String,byte[]> fileData = new HashMap<>();

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

        Thread parser = new Thread(() -> {
            try {
                //HTTP GET
                Socket client = serversocket.accept();
                DataInputStream clienteIn = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                DataOutputStream clienteOut = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                BufferedReader br = new BufferedReader(new InputStreamReader(clienteIn));
                String httprequest = br.readLine();
                String []request = httprequest.split(" ");
                String ficheiro = request[1].substring(1);

                System.out.println(ficheiro);
                filename.add(ficheiro);

                //request UDP ir e vir
                //await()
                //vars de condição?

                //HTTP RESPONSE
                clienteOut.write("HTTP/1.1 200 OK\r\n".getBytes());
                clienteOut.write(("Content-Length: " + fileData.get(ficheiro).length  + "\r\n").getBytes());
                clienteOut.write(("Content-Type: " + getExtensao(ficheiro) + ";\r\n\r\n").getBytes());
                clienteOut.write(fileData.get(ficheiro));
                clienteOut.flush();
                filename.remove(ficheiro);

            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        parser.start();


        Thread udps = new Thread(() -> {
            while(true){
                FSChunk f = null;
                try {
                    f = protocol.receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(f!=null) {
                    switch (f.tag) {
                        case "A":  // Autenticação by Server
                            if (!socketInterno.containsKey(f.senderIpAddress) && password.equals(new String(f.data))) {
                                try {
                                    FSChunkProtocol newCon = new FSChunkProtocol(new DatagramSocket(f.senderPort, InetAddress.getByName(f.senderIpAddress)),f.senderIpAddress,f.senderPort);
                                    socketInterno.put(f.senderIpAddress, newCon);
                                    System.out.println("Novo servidor autenticado: " + f.senderIpAddress);
                                    FSChunk listOfFiles = new FSChunk("LR", "".getBytes());
                                    newCon.send(listOfFiles);
                                } catch (SocketException | UnknownHostException e) {
                                    e.printStackTrace();
                                }
                            } else
                                System.out.println("Tentativa da ataque!!!!");
                            break;
                        case "EMPTY":
                            break;
                        default:
                            System.out.println("ERRO HTTPGW:89 (switch udps)");
                            System.out.println("TAG recebida : " + f.tag);
                            break;
                    }
                }
            }
        });
        udps.start();

        Thread udpsender = new Thread(() -> {
            while(true){
                FSChunkProtocol destino = null;
                if(!filename.isEmpty()) {
                    String file = filename.get(0);
                    FSChunk toSend = new FSChunk("FR", file, "".getBytes());
                    Random random = new Random();
                    int r = random.nextInt(ficheirosServer.get(file).size());
                    String s = ficheirosServer.get(file).get(r);
                    destino = socketInterno.get(s);
                    destino.send(toSend);
                }
                FSChunk f = null;
                try {
                    f = protocol.receive();
                } catch (IOException e) {
                    FSChunkProtocol fs = socketInterno.remove(destino.socket.getLocalAddress().getHostName());
                    ficheirosServer.remove(destino.socket.getLocalAddress().getHostName());
                    try {
                        fs.close();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                if(f!=null) {
                    switch (f.tag) {
                        case "FS": // File send by Server
                            fileData.put(f.file, f.data);
                            break;
                        case "LR": // List of files sent by Server
                            List<String> ficheiros = f.getDataList();
                            for (String fich : ficheiros) {
                                if (ficheirosServer.containsKey(fich))
                                    ficheirosServer.get(fich).add(f.senderIpAddress);
                                else {
                                    List<String> l = new ArrayList<>();
                                    l.add(f.senderIpAddress);
                                    ficheirosServer.put(fich, l);
                                }
                            }
                            break;
                        case "CLOSE":
                            FSChunkProtocol fs = socketInterno.remove(f.senderIpAddress);
                            ficheirosServer.remove(f.senderIpAddress);
                            try {
                                fs.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case "EMPTY":
                            break;
                        default:
                            System.out.println("ERRO HTTPGW:80 (switch udps)");
                            System.out.println("TAG recebida : " + f.tag);
                            break;
                    }
                }
            }
        });
        udpsender.start();
    }

    private static String getExtensao(String filename){
        String extensao = "";
        try {
            extensao = filename.split(".")[1];
        }catch (ArrayIndexOutOfBoundsException E){
            return "text/plain";
        }

        return switch (extensao) {
            case "txt" -> "text/plain";
            case "html" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "text/javascript";
            case "gif" -> "image/gif";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "wav" -> "audio/wav";
            case "mp2", "mp3" -> "audio/mpeg";
            case "mp4", "avi" -> "video/webm";
            case "xml" -> "application/xml";
            case "pdf" -> "application/pdf";
            default -> "text/plain";
        };

    }
}
