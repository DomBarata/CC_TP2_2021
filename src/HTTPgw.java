import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// HÁ UM "ERRO" NOS FICHEIROS RECEBIDOS, UMA DAS STRINGS É VAZIA, É NECESSÁRIO VER - not important
// AS COMUNICAÇÕES ESTÃO OK, MESMO AQUELAS COM OS PEDIDOS HTTP
// PARA OS ENVIOS FRAGMENTADOS, TALVEZ ALTERAR A FRAGMENTAÇAO PARA UM BYTE[][] E FAZER FORA (SRV) - acho que isto está resolvido com o metodo getfragment do fschunk

public class HTTPgw {
    // Socket UDP para comunicar com o 'FastFileServer'
    public static Map<String, FSChunkProtocol> socketInterno = new HashMap();
    private final static String password = "PASSWORD";
    public static Map<String,List<String>> ficheirosServer = new HashMap<>();

    private static Map<String,byte[]> fileData = new HashMap<>(); // ficheiro :: Dados
    private static FSChunkProtocol udpReceiveProtocol;

    private final static ReentrantLock clientlock = new ReentrantLock();
    private static final Condition condition = clientlock.newCondition();

    private static ArrayDeque<Pedido> pedidosPendentes = new ArrayDeque<>();
    private static final int timedout = 60;

    public static void main(String[] args) throws IOException {
        ServerSocket httpSocket;
        httpSocket = new ServerSocket(8080);

        DatagramSocket udpReceiveSocket = null;
        try {
            udpReceiveSocket = new DatagramSocket(8888);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        udpReceiveProtocol = new FSChunkProtocol(udpReceiveSocket, timedout * 1000);

        System.out.println("Ativo em " + InetAddress.getLocalHost().getHostAddress() + " " + udpReceiveSocket.getLocalPort());

        Thread parser = new Thread(() -> {
            while (true) {
                try {
                    //HTTP GET
                    Socket client = httpSocket.accept();
                    System.out.println("A receber pedido HTTP...");
                    Thread t = new Thread(new WorkerHTTP(client));
                    t.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        parser.start();

        Thread udpReceiveThread = new Thread(() -> {
            while (true) {
                FSChunk f;
                List<FSChunk> listaFragmentos = new ArrayList<>();
                try {
                    System.out.println("À escuta...1");
                    f = udpReceiveProtocol.receive();
                    if (socketInterno.containsKey(f.senderIpAddress) && f.isfragmented()) {
                        FSChunkProtocol conn = socketInterno.get(f.senderIpAddress);
                        listaFragmentos.add(f);

                        System.out.println("A unir fragmentos de dados...");
                        int t = 0;
                        while (f.isfragmented()) {
                            udpReceiveProtocol.socket.setSoTimeout(timedout*1000);
                            System.out.println("À escuta... "+t++);
                            f = udpReceiveProtocol.receive();
                            listaFragmentos.add(f);
                        }
                        System.out.println("A verificar dados completos...");
                        listaFragmentos.sort((o1, o2) -> {
                            int r = -2;
                            if (o1.getFragmentNumber() < o2.getFragmentNumber()) r = -1;
                            if (o1.getFragmentNumber() == o2.getFragmentNumber()) r = 0;
                            if (o1.getFragmentNumber() > o2.getFragmentNumber()) r = 1;
                            return r;
                        });
                        boolean flag = true;

                        while(flag) {
                            if (listaFragmentos.get(listaFragmentos.size() - 1).getFragmentNumber() == (listaFragmentos.size() - 1)) {
                                f = listaFragmentos.get(0);

                                for (int i = 1; i < listaFragmentos.size(); i++)
                                    f.complete(listaFragmentos.get(i));


                                System.out.println("Pacotes de dados unidos com sucesso!");
                                flag = false;
                            } else {
                                for (int i = 0; i < listaFragmentos.size(); i++) {
                                    System.out.println("Falha a receber dados, a pedir fragmentos em falta...");
                                    if (listaFragmentos.get(i).getFragmentNumber() != i) {
                                        FSChunk resend = new FSChunk("RESEND", f.file, "".getBytes());
                                        conn.send(resend);
                                        f = udpReceiveProtocol.receive();
                                    }
                                }
                            }
                        }
                        System.out.println("Pacote de dados recebido com sucesso");
                    }
                    if (f != null) {
                        switch (f.tag) {
                            case "A":  // Autenticação by Server
                                System.out.println("A receber pedido de autenticação...");
                                if (!socketInterno.containsKey(f.senderIpAddress) && password.equals(new String(f.data))) {
                                    try {
                                        FSChunkProtocol newCon = new FSChunkProtocol(new DatagramSocket(), f.senderIpAddress, f.senderPort + 1);
                                        socketInterno.put(f.senderIpAddress, newCon);
                                        System.out.println("Novo servidor autenticado: " + f.senderIpAddress + " " + (f.senderPort + 1));
                                        FSChunk listOfFiles = new FSChunk("LR", "".getBytes());
                                        newCon.send(listOfFiles);
                                        newCon.setOcupied(true);
                                    } catch (SocketException | UnknownHostException e) {
                                        e.printStackTrace();
                                    }
                                } else
                                    System.out.println("Server já autenticado ou password errada!");
                                break;
                            case "LR": // List of files sent by Server
                                if (socketInterno.containsKey(f.senderIpAddress)) {
                                    System.out.println("A receber lista de nomes de ficheiros de servidor...");
                                    List<String> ficheiros = f.getDataList();
                                    for (String fich : ficheiros) {
                                        System.out.println("\t- " + fich);
                                        if (ficheirosServer.containsKey(fich))
                                            ficheirosServer.get(fich).add(f.senderIpAddress);
                                        else {
                                            List<String> l = new ArrayList<>();
                                            l.add(f.senderIpAddress);
                                            ficheirosServer.put(fich, l);
                                        }
                                    }
                                }
                                break;
                            case "FR":
                                clientlock.lock();
                                try{
                                    if (socketInterno.containsKey(f.senderIpAddress)) {
                                        f.filenameClean();
                                        fileData.put(f.file, f.data);
                                        condition.signalAll();
                                        System.out.println("Ficheiro recebido!");
                                    }
                                }finally {
                                    clientlock.unlock();
                                }

                                break;
                            case "CLOSE":
                                if (socketInterno.containsKey(f.senderIpAddress)) {
                                    FSChunkProtocol fs = socketInterno.remove(f.senderIpAddress);
                                    ficheirosServer.remove(f.senderIpAddress);
                                    try {
                                        fs.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            case "EMPTY":
                            default:
                                System.out.println("ERRO HTTPGW: 114 (switch udps)");
                                System.out.println("TAG recebida : " + f.tag);
                                break;
                        }
                        socketInterno.get(f.senderIpAddress).setOcupied(false);
                        listaFragmentos.clear();
                    }
                } catch(SocketTimeoutException e){
                    System.out.println("A verificar servidores...");
                    int i = 0;
                    if (!pedidosPendentes.isEmpty())
                        while (pedidosPendentes.getLast().getTime() > timedout) {
                            Pedido old = pedidosPendentes.removeLast();
                            socketInterno.remove(old.getServer());
                            FSChunkProtocol fs = selectServer(old.getFilename());
                            if(!(fs==null)) {
                                fs.send(new FSChunk("FR", old.getFilename(), "".getBytes()));
                                pedidosPendentes.push(new Pedido(old.getFilename(), fs.ipDestino.getHostAddress()));
                                i++;
                            }else{
                                condition.signalAll();//TODO
                                break;
                            }
                        }
                    System.out.println("Servidores verificados. Foram removidos " + i + " servidores!");
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        });
        udpReceiveThread.start();
/*
        Thread udpsender = new Thread(() -> {
            while(true){
                FSChunk f = null;
                try {
                    f = udpReceiveProtocol.receive();
                } catch (IOException e) {
                    e.printStackTrace();
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
*/
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

    private static FSChunkProtocol selectServer(String ficheiro){
        FSChunkProtocol destino = null;
        Random random = new Random();
        if(!socketInterno.isEmpty() && !ficheirosServer.isEmpty() && ficheirosServer.containsKey(ficheiro)) {
            do {
                int r = random.nextInt(ficheirosServer.get(ficheiro).size());
                String s = ficheirosServer.get(ficheiro).get(r);
                destino = socketInterno.get(s);
            }while(destino.isOcupied());
        }
        return destino;
    }

    static class WorkerHTTP implements Runnable{
        private Socket client;

        public WorkerHTTP(Socket socket){
            this.client = socket;
        }

        private FSChunkProtocol selectServer(String ficheiro){
            System.out.println("A selecionar servidor para pedir ficheiro...");
            FSChunkProtocol destino = null;
            Random random = new Random();
            if(!socketInterno.isEmpty() && !ficheirosServer.isEmpty() && ficheirosServer.containsKey(ficheiro)) {
                do {
                    int r = random.nextInt(ficheirosServer.get(ficheiro).size());
                    String s = ficheirosServer.get(ficheiro).get(r);
                    destino = socketInterno.get(s);
                }while(destino.isOcupied());
            }
            System.out.println("Servidor para pedir ficheiro: " + destino.ipDestino);
            return destino;
        }

        public void run() {
            clientlock.lock();
            try {
                DataInputStream clienteIn = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                DataOutputStream clienteOut = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                BufferedReader br = new BufferedReader(new InputStreamReader(clienteIn));
                String httprequest = br.readLine();
                String[] request = httprequest.split(" ");
                String ficheiro = request[1].substring(1);

                System.out.println("Pedido HTTP: " + "\"" + ficheiro + "\"");

                try {
                    FSChunkProtocol fs = null;
                    if (!fileData.containsKey(ficheiro))
                        fs = selectServer(ficheiro);
                        fs.send(new FSChunk("FR", ficheiro, "".getBytes()));
                        pedidosPendentes.add(new Pedido(ficheiro, fs.ipDestino.getHostAddress()));
                    while(!fileData.containsKey(ficheiro) && !socketInterno.isEmpty()) {
                        condition.await();
                    }

                    fileData.get(ficheiro);

                    byte [] ficheiroQuePreciso = Files.readAllBytes(Path.of("test.html"));

                    System.out.println("Envio do ficheiro solicitado: \"" + ficheiro + "\"");
                    //HTTP RESPONSE
                    clienteOut.write("HTTP/1.1 200 OK\r\n".getBytes());
                    clienteOut.write(("Content-Length: " + fileData.get(ficheiro).length + "\r\n").getBytes());
                    clienteOut.write(("Content-Type: " + getExtensao(ficheiro) + ";\r\n\r\n").getBytes());
                    clienteOut.write(fileData.get(ficheiro));
                    clienteOut.flush();
                } catch (NullPointerException e){
                    System.out.println("Ficheiro \"" + ficheiro + "\"" + " não encontrado.\n Resposta de erro.");
                    clienteOut.write("HTTP/1.1 404 Not Found\r\n".getBytes());
                    clienteOut.write(("Content-Length: " + 0 + "\r\n").getBytes());
                    clienteOut.write(("Content-Type: " + "text/html" + ";\r\n\r\n").getBytes());
                    clienteOut.flush();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            finally{
                clientlock.unlock();
            }
                
        }
    }

}
