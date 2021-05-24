import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class FastFileServer {
    private static FSChunkProtocol protocolToSend;
    private static FSChunkProtocol protocolToReceive;

    public static void main(String[] args) throws UnknownHostException {
        String ipGateway = args[0];
        int portGateway = Integer.parseInt(args[1]);
        DatagramSocket socketSend = null;
        DatagramSocket socketReceive = null;

        try {
            socketSend = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Erro a conectar com o gateway");
        }

        try {
            socketReceive = new DatagramSocket(socketSend.getLocalPort()+1);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        protocolToReceive = new FSChunkProtocol(socketReceive,InetAddress.getLocalHost().getHostAddress(),socketReceive.getLocalPort());
        protocolToSend = new FSChunkProtocol(socketSend, ipGateway, portGateway);


        //protocol.receive();
        FSChunk autentica = new FSChunk("A","PASSWORD".getBytes());
        protocolToSend.send(autentica); //Autentica-se junto do HttpGw, indicando o seu IP e porta e confirmando a PASSWORD
        //get dados de file no server

        System.out.println("A autenticar-se ao gateway");


        Thread response = new Thread(() -> {
            while(true) {
                try {
                    FSChunk chunk = protocolToReceive.receive();
                    processChunks(chunk, protocolToSend);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        response.start();

    }


    //FALTA RECEBER O PATH
    private static void processChunks(FSChunk received,FSChunkProtocol connection){
        String tag = received.tag;
        try {
            switch (tag) {
                case "LR":
                    if(received.senderIpAddress.equals(protocolToSend.ipDestino.getHostAddress())) {
                        System.out.println("A receber pedido de ficheiros");
                        //GET LIST OF FILE NAMES
                        List<String> ficheiros = new ArrayList<String>();
                        FSChunk fsc = new FSChunk("LR", getFiles());
                        fsc.setData(ficheiros);
                        connection.send(fsc);
                        System.out.println("Lista de ficheiros enviada!!");
                    }
                    break;
                case "FR" :
                    if(received.senderIpAddress.equals(protocolToSend.ipDestino.getHostAddress())) {
                        System.out.println("Ficheiro pedido: " + "\"" + received.file + "\"");
                        //FILE REQUEST
                        byte[] filebytes = Files.readAllBytes(Paths.get(received.file));
                        FSChunk fsck = new FSChunk("FR", filebytes);
                        connection.send(fsck);
                        System.out.println("Ficheiro enviado!");
                    }
                    break;
                case "RESEND" :
                    int pos = Integer.parseInt(received.file.substring(received.file.length()-4));
                    if(!received.file.isEmpty()) {
                        FSChunk fs = new FSChunk("FR", received.file, Files.readAllBytes(Paths.get(received.file)));
                        byte[] dadosEmFalta = fs.getData(pos,connection.safeSize);
                        connection.send(dadosEmFalta);
                    }else{
                        FSChunk fs = new FSChunk("LR", getFiles());
                        byte[] dadosEmFalta = fs.getData(pos,connection.safeSize);
                        connection.send(dadosEmFalta);
                    }
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

    private static byte[] getFiles(){
        List<String> ficheiros = new ArrayList<String>();
        File[] files = new File(".").listFiles();
        ByteArrayOutputStream bytearray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytearray);
        System.out.println("Lista de ficheiros: ");
        for (File f : files) {
            if (f.isFile()) {
                ficheiros.add(f.getName());
                System.out.println("\t- " + f.getName());
            }
        }
        String data = "";
        for (String s : ficheiros){
            data += ":/" + s;
        }
        return data.getBytes();
    }

}