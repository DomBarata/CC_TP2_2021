import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class test {
/*    public static void main(String[] args) throws UnknownHostException {
       // System.out.println(InetAddress.getLocalHost());

        System.out.println(InetAddress.getByName(InetAddress.getLocalHost().getHostName()));
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        System.out.println(InetAddress.getByName("10.1.1.1"));

        int[] a = new int[3];
        int[][] b = new int [5][6];
        System.out.println(a.length);
        System.out.println(b.length);

        int a = 2;
        int b = 59;

        System.out.println(String.format("%03d", a));

        System.out.println(String.format("%03d", b));


        boolean flag = false;
        int i = 123;
        String a = "abcd";

        String stri = new String();

        stri += flag + "::" + i + "::" + a;

        byte[] array = stri.getBytes();

        String[] str = new String(array).split("::");

        System.out.println(str[0] + " " + str[1] + " " + str[2] + " ");

    */
/*
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(8181, InetAddress.getByName("192.168.56.1"));

            System.out.println("porta: " + socket.getPort());
            System.out.println("porta2: " + socket.getLocalPort());
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

    }
*/
public static void main(String[] args) throws Exception {
    InetSocketAddress adress = new InetSocketAddress(8000);
    HttpServer server = HttpServer.create(adress, 0);
    server.createContext("/test", new MyHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
}

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}

