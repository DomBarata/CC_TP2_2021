public class Par {
    private int a;
    private int b;

    public Par(int a, int b){this.a=a; this.b=b;}

    public Par(String[] str){
        this.a = Integer.parseInt(str[0]);
        this.b = Integer.parseInt(str[1]);
    }

    public Par(byte[] array){
        String msg = new String(array);

        int i = 0;
        int tamA = Integer.parseInt(msg.substring(i,i+1));
        this.a = Integer.parseInt(msg.substring(i+1,i+tamA+1));
        i = i+tamA+1;
        tamA = Integer.parseInt(msg.substring(i,i+1));
        this.b = Integer.parseInt(msg.substring(i+1,i+tamA+1));

        tamA++;
    }

    public int getA() { return a; }

    public int getB() { return b; }

    public byte[] getBytes() /*throws Exception*/{
        /*ServerSocket serverSocket = new ServerSocket(23456);
        Socket socket1 = new Socket("localhost",23456);
        Socket socket = serverSocket.accept();
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket1.getOutputStream()));

        out.writeInt(a);
        out.writeInt(b);
        out.flush();
        byte[] array = new byte[8];
        in.readFully(array);*/

        String a = String.valueOf(this.a);
        String b = String.valueOf(this.b);

        String tamA = String.valueOf(a.length());
        String tamB = String.valueOf(b.length());

        byte[] bi = tamA.concat(a).concat(tamB).concat(b).getBytes();
        String s = new String(bi);

        return bi;
    }

    @Override
    public String toString() {
        return "("+ a +", " + b +")";
    }

    public boolean equals(Par par) {
        return this.a == par.getA() && this.b == par.getB();
    }


}
