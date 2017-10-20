
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class peerProcess {

    private String peerID;
    private int port;
    private int numConnecting;
    private int numConnectTo;

    private BlockingQueue<byte[]> inbox; //one inbox for this process
    private HashMap<String, BlockingQueue> outboxes; //maps a peerID to its outbox

    private HashSet<IncomingHandler> inHandlers;
    private HashSet<OutgoingHandler> outHandlers;
//    private HashMap<String, IncomingHandler> peerInHandlers; //maps from peerID to IncomingHandler for that peer (unnecessary because just need a peer's outbox)
//    private HashMap<String, OutgoingHandler> peerOutHandlers; //maps from peerID to OutgoingHandler for that peer (unnecessary because just need a peer's outbox)

    private HashSet<String> peers;
    private HashSet<String> choked;
    private HashSet<String> unchoked;
    private HashSet<String> interested;
    private ConcurrentHashMap<String, BitSet> peerPieces; //maps from peerID to the bitfield for that peer (which pieces that peer has)

    /* Do we need to know bitfields for each peer? I guess so to be able to make requests. Updated on receipt of "have" message. */

    private AtomicIntegerArray pieces; //create a new class with this underneath? Then use it like a bitset? No no no...don't need concurrency here. Just update as get pieces.
    // so when a piece is received this bitfield is adjusted. If get multiple pieces? I guess doesn't matter. Or can safely overwrite? Not an issue I think
    // remember, this is for just 1 peer. so we choose who gets requested and whether to send multiple requests. Have a requested BitSet?
    private BitSet bitfield;
    private BitSet requested; // So maybe only send 1 request for a piece per round. Then when receive or at timeout, unset the bit.


    public peerProcess(String peerID) {
        this.peerID = peerID;
    }

    public void processConfig() {


    }


    public void acceptConnections() {
        try {
            ServerSocket welcomeSocket = new ServerSocket(port);
            for (int i = 0; i < numConnecting; i++) {
                Socket socket = welcomeSocket.accept();
//                Connection peerConnection = new Connection(socket, )
            }
        } catch (IOException e) {
            System.out.println("Exception in peerProcess acceptConnections() method");
        }




    }

    public void requestConnections() {

    }

    public void dispatch() {
        byte[] message = new byte[Constants.MESSAGE_SIZE];
        try {
            message = inbox.take();
        } catch(InterruptedException e) {
            e.printStackTrace();
            System.out.println("Interruped exception in peer process dispatch method");
        }
        processMessage(message);

    }

    public void processMessage(byte[] message) {
        byte[] length = new byte[4];

        int messageType = -1;
        InputStream is = new ByteArrayInputStream(message);
        try {
            is.read(length, 0, 4);
            messageType = is.read();
            int msgLength = Util.bytesToInt(length);
//            byte[] payload = ByteBuffer.allocate(msgLength).array(); //does this work?
            byte[] payload = new byte[msgLength];
            is.read(payload, 0, msgLength); //or payload = is.readAllBytes()? Or does that only work with Java 9?
        } catch(IOException e) {
            e.printStackTrace();
        }
        switch (messageType) {
            case -1: //unfinished

        }


    }

    private void timeout() {

        requested.clear();

    }
    
    private void run() {
        processConfig();
        acceptConnections();
        requestConnections();
        Iterator peerIterator = peers.iterator();
        while(peerIterator.hasNext()) {

        }
    }


    public static void main(String[] args) {

        String peerID = args[0];
        peerProcess me = new peerProcess(peerID);
        me.run();


    }

}
