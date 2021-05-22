import java.io.*;
import java.net.*;
import java.net.http.HttpRequest;
import java.nio.file.Paths;
import java.time.Duration;

public class HTTPRequester {
    private URL url;
    private HttpURLConnection urlConnection;
    private DataInputStream in;
    private DataOutputStream out;

    public HTTPRequester() throws IOException {
        this.url = new URL("http://localhost:8080");
        this.urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        //this.in = new DataInputStream(new BufferedInputStream(urlConnection.getInputStream()));
        //this.out = new DataOutputStream(new BufferedOutputStream(urlConnection.getOutputStream()));
    }

    public void send(String fich) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/"))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json")
                .GET().build();

        //out.writeUTF("GET " +fich+" HTTP/1.1");
        //out.flush();
    }

    private void receive() throws IOException {
        String answer = in.readUTF();
        System.out.println(answer);
    }

    public static void main(String[] args) throws IOException {
        HTTPRequester request = new HTTPRequester();
        request.send("img.png");
        request.receive();
    }


}
