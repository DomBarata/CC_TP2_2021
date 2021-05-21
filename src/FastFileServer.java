import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        System.out.println(file);

        try{
            //file = getficheiro
            byte[] metadados = Files.readAllBytes(Paths.get(file));
            //getPar2
            int de = par.getA();
            int partitions = par.getB();

            //nr_byte = file.data/par.b
            //inicio = nr_bytes*par.a
            //fim = nr_bytes*(par.a +1)
            //bytes_read = byte[] frag = Arrays.copyOfRange(file.data, inicio,fim);
            //FSChunk fc = new FSChunk(file.getname(), par.a, par.b, bytes_read)
            //protocol.send(bytes_read)



        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
