import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class FastFileServer {
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

        FSChunkProtocol protocol = new FSChunkProtocol(socket,ip,port);
        //protocol.receive();
        protocol.send("".getBytes()); //regista-se junto do HttpGw, indicando o seu IP e porta
        //get dados de file no server

        FSChunk dados_mandar = protocol.receive();

        //Dados para saber quantos byte retirar de file
        Par par = dados_mandar.par ;
        String file = dados_mandar.file;

        try{
                File temp = File.createTempFile("pattern",".suffix");
                temp.deleteOnExit();


                //BufferedWriter out = new BufferedWriter(new FileWriter(temp));
                //out.write("aString");
                //out.close();


        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
