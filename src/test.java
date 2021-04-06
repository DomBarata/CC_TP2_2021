import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class test {
    public static void main(String[] args) throws UnknownHostException {
       // System.out.println(InetAddress.getLocalHost());
/*
        System.out.println(InetAddress.getByName(InetAddress.getLocalHost().getHostName()));
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        System.out.println(InetAddress.getByName("10.1.1.1"));

        int[] a = new int[3];
        int[][] b = new int [5][6];
        System.out.println(a.length);
        System.out.println(b.length);
  */
        int a = 2;
        int b = 59;

        System.out.println(String.format("%03d", a));

        System.out.println(String.format("%03d", b));
    }
}
