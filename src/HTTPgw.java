import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// TODO - CORRIGIR - DEPOIS DO CICLO WHILE DE PACOTES FRAGMENTADOS, VAI SEGUIR PARA O SWITCH E NÃO É SUSPOSTO
// TODO - ALTERAR - TIMEOUT NOT WORKING - GUARDAR NUMA LISTA MAYBE INTEIROS DO RECEBIDO E NO ÚLTIMO CHECKAR SE
//  O TAMANHO DA LISTA É IGUAL AO NR ESPERADO DE FRAGMENTOS ESPERADOS, SE NÃO FOR, VER O QUE FALTA E PEDIR
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
                try {
                    f = udpReceiveProtocol.receive();
                    FSChunk pacote = new FSChunk(f.senderIpAddress, f.senderPort, FSChunkProtocol.trim(f.data));
                    while(pacote.isfragmented){
                        if (socketInterno.containsKey(f.senderIpAddress)){
                            FSChunkProtocol conn = new FSChunkProtocol(new DatagramSocket(), f.senderIpAddress, f.senderPort+1);
                            System.out.println("A unir fragmentos de dados...");
                            try {
                                FSChunk aux = udpReceiveProtocol.receive();
                                pacote.complete(aux);
                                conn.setOcupied(false);
                            }catch (SocketTimeoutException E){
                                System.out.println("Pacote de dados perdido, pedir retransmissão de fragmento...");
                                FSChunk askMore = new FSChunk("RESEND",f.file,"".getBytes());
                                conn.send(askMore);
                                conn.setOcupied(true);
                            }
                        }else
                            System.out.println("Pedido malicoso: Servidor não autenticado");
                    } System.out.println("Pacote de dados recebido com sucesso");
                    if (f != null) {
                        switch (f.tag) {
                            case "A":  // Autenticação by Server
                                System.out.println("A receber pedido de autenticação...");
                                if (!socketInterno.containsKey(f.senderIpAddress) && password.equals(new String(f.data))) {
                                    try {
                                        FSChunkProtocol newCon = new FSChunkProtocol(new DatagramSocket(), f.senderIpAddress, f.senderPort+1);
                                        socketInterno.put(f.senderIpAddress, newCon);
                                        System.out.println("Novo servidor autenticado: " + f.senderIpAddress + " " + (f.senderPort+1));
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
                                if(socketInterno.containsKey(f.senderIpAddress)){
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
                                if(socketInterno.containsKey(f.senderIpAddress)) {
                                    System.out.println("A receber ficheiro...");
                                    fileData.put(f.file, f.data);
                                    condition.signalAll();
                                    System.out.println("Ficheiro recebido!");
                                }
                                break;
                            case "CLOSE":
                                if(socketInterno.containsKey(f.senderIpAddress)) {
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
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("A verificar servidores...");
                    int i = 0;
                    if(!pedidosPendentes.isEmpty())
                        while(pedidosPendentes.getLast().getTime()>timedout){
                            Pedido old = pedidosPendentes.removeLast();
                            socketInterno.remove(old.getServer());
                            FSChunkProtocol fs = selectServer(old.getFilename());
                            fs.send(new FSChunk("FR", old.getFilename(), "".getBytes()));
                            pedidosPendentes.push(new Pedido(old.getFilename(), fs.ipDestino.getHostAddress()));
                            i++;
                        }
                    System.out.println("Servidores verificados. Foram removidos " + i + " servidores!");
                } catch (IOException e) {
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
        if(!ficheirosServer.isEmpty() && ficheirosServer.containsKey(ficheiro)) {
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
            if(!ficheirosServer.isEmpty() && ficheirosServer.containsKey(ficheiro)) {
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
