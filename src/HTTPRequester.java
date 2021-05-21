import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

public class HTTPRequester implements AutoCloseable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public HTTPRequester() throws IOException {
        socket = new Socket("localhost" ,8080);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public void send(String fich) throws IOException {
        URL url = new URL("http://localhost:8080");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("accept", "application/json");
        InputStream responseStream = con.getInputStream();
        System.out.println(responseStream);

        //out.writeUTF("GET " +fich+" HTTP/1.1");
        //out.flush();
    }

    private void receive() throws IOException {
        String answer = in.readUTF();
        System.out.println(answer);
    }


    @Override
    public void close() throws Exception {
        this.socket.close();
    }


    public static void main(String[] args) throws IOException {
        HTTPRequester request = new HTTPRequester();
        request.send("img.png");
        //request.receive();
    }


}
