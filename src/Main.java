


import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;

public class Main {

    public static void main(String[] args) {
//        HashSet hs = new HashSet();
//        hs.add("1001");
//        hs.add("1002");
//        hs.add(1);
//        System.out.println(hs);
//        System.out.println(hs.contains("1002"));
//        System.out.println(hs.contains(1002));
//
//        byte [] bytes = ByteBuffer.allocate(4).putInt(17291729).array();


//        int piecesize = 32768;
//        int numPieces = (int) Math.ceil((filesize / piecesize) * 1.0);
//        System.out.println(numPieces);
////        BitSet bs = new BitSet(numPieces);
        BitSet bs = new BitSet();
//        System.out.println(bs.size());
////        System.out.println(bs.get(3345));
//        bs.set(3345);
//        bs.set(5);
        bs.set(0, 10);
        bs.set(40,50);
        bs.set(1000,1009);
//        System.out.println(bs);
        System.out.println(bs.get(45));
        System.out.println(bs.length());
        System.out.println(bs.size());
        byte[] b = bs.toByteArray();
        System.out.println("length of bytearray bitset is " + b.length);
        BitSet testing = BitSet.valueOf(b);
        System.out.println(testing);

//        BitSet received = BitSet.valueOf(b);
//        System.out.println(received);
//        byte[] zeroes = new byte[10];
//        Arrays.fill(zeroes, (byte) 0);
//        System.out.println(zeroes.length);
//        System.out.println(Util.bytesToInt(zeroes));
        String boolStr = "1";
        System.out.println(Util.strToBool(boolStr));
        System.out.println(Constants.MESSAGE_SIZE);
        Constants.setMessageSize(8766578);
        System.out.println(Constants.MESSAGE_SIZE);
        System.out.println(Constants.FILE_NAME);
        Constants.setFileName("file.dat");
        System.out.println(Constants.FILE_NAME);
        try {
            InetAddress address = InetAddress.getByName("lin114-00.cise.ufl.edu");
            SocketAddress socketAddress = new InetSocketAddress(address, 6008);
            System.out.println(address);
            System.out.println(socketAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }




////        System.out.println(received.get(45));
//        received.set(11, 22);
//        BitSet news;
//        news = (BitSet) bs.clone();
//        bs.or(received);
////        System.out.println(b);
//        System.out.println(bs.toString());
//        System.out.println(news.toString());
//        bs.andNot(news);
//        System.out.println(bs.toString());
//
//
////        System.out.println(bs.size());
////        System.out.println(bs.get(3345));


    }

}