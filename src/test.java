import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class test {
    public static void main(String[] args) throws UnknownHostException {
       // System.out.println(InetAddress.getLocalHost());

        System.out.println(InetAddress.getByName(InetAddress.getLocalHost().getHostName()));
        System.out.println(InetAddress.getByName("10.1.1.1"));
    }
}
