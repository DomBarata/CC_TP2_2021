import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class FastFileServer {
    private static FSChunkProtocol protocol;

    public static void main(String[] args) throws UnknownHostException {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if(socket == null) { System.out.println("Erro a conectar com o gateway"); return;}

        protocol = new FSChunkProtocol(socket, ip, port);


        //protocol.receive();
        FSChunk autentica = new FSChunk("A","PASSWORD".getBytes());
        protocol.send(autentica); //Autentica-se junto do HttpGw, indicando o seu IP e porta e confirmando a PASSWORD
        //get dados de file no server

        FSChunk chunk = null;
        try {
            chunk = protocol.receive();
        } catch (IOException e) {
            System.out.println("Conexão perdida...");
            return;
        }

        FSChunk finalChunk = chunk;
        Thread response = new Thread(() -> {
            try {
                processChunks(finalChunk, protocol);

            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        response.start();

    }

    private static void processChunks(FSChunk received,FSChunkProtocol connection){
        String tag = received.tag;
        try {
            switch (tag) {
                case "LR":
                    //GET LIST OF FILE NAMES
                    List <String> ficheiros = new ArrayList<String>();
                    File[] files = new File("/").listFiles();
                    ByteArrayOutputStream bytearray = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bytearray);
                    for(File f: files) {
                        if (f.isFile()) {
                            ficheiros.add(f.getName());
                        }
                    }
                    FSChunk fsc = new FSChunk("LR","".getBytes());
                    fsc.setData(ficheiros);
                    connection.send(fsc);
                    break;
                case "FR" :
                    //FILE REQUEST
                    byte[] filebytes = Files.readAllBytes(Paths.get("/path"+received.file));
                    FSChunk fsck = new FSChunk("FR",filebytes);
                    connection.send(fsck);
                    break;
                case "CLOSE" :
                    //SEND CLOSE TAG AND CLOSE UDP CONNECTION
                    FSChunk fecha = new FSChunk("CLOSE","".getBytes());
                    connection.send(fecha);
                    connection.close();
                    break;
                case "EMPTY": // CHUNK WITH ERROR
                default:
                    System.out.println("Erro");
                    System.out.println("Chunk com ERRO");
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}