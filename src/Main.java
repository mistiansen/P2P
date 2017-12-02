


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.BitSet;


public class Main {

    public void BitSetTests() {

//        int piecesize = 32768;
//        int numPieces = (int) Math.ceil((filesize / piecesize) * 1.0);
//        System.out.println(numPieces);
////        BitSet bs = new BitSet(numPieces);
        BitSet bs = new BitSet();
//        System.out.println(bs.size());
////        System.out.println(bs.get(3345));
//        bs.set(3345);
//        bs.set(5);
        bs.set(0, 100);
//        bs.set(40,50);
//        bs.set(1000,1009);
//        System.out.println(bs);
        BitSet requested = new BitSet();
        requested.set(0,20);
        BitSet toRequest = (BitSet) bs.clone();
        toRequest.andNot(requested);
        System.out.println("this is toRequest bitset " + toRequest);
        System.out.println(toRequest.nextSetBit(0));

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

    public void StartPeers() {


    }

    public void inetaddressTest() {
        try {
            InetAddress address = InetAddress.getByName("lin114-00.cise.ufl.edu");
            SocketAddress socketAddress = new InetSocketAddress(address, 6008);
            System.out.println(address);
            System.out.println(socketAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        BitSet theirs = new BitSet();
        BitSet mine = new BitSet();
        theirs.set(0, 20);
        mine.set(13, 25);
        BitSet need = new BitSet();
        need.set(3,12);
//        theirs.andNot(mine);
//        mine.andNot(requested);
        System.out.println(mine);


        BitSet toRequest = (BitSet) need.clone();
        toRequest.and(theirs); //only want to request if they have it (I need and they have it)
        toRequest.andNot(mine);
        System.out.println(toRequest);
        int sex = toRequest.nextSetBit(0);
        System.out.println(sex);

        System.out.println(toRequest.length());

//        BitSet and = (BitSet) theirs.clone();
//        and.and(mine);
//        System.out.println(mine);
//        System.out.println(and);
//        System.out.println(theirs);
//        System.out.println(mine.intersects(theirs));
//        mine.clear();
//        System.out.println(mine.intersects(theirs));




    }

}