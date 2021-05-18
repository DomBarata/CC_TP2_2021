import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(8181, InetAddress.getByName("192.168.56.1"));

            System.out.println("porta: " + socket.getPort());
            System.out.println("porta2: " + socket.getLocalPort());
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

    }
}
