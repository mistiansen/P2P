
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

    private BlockingQueue<Message> inbox; //one inbox for this process
    private HashMap<String, BlockingQueue> outboxes; //maps a peerID to its outbox

    private HashSet<IncomingHandler> inHandlers;
    private HashSet<OutgoingHandler> outHandlers;
//    private HashMap<String, IncomingHandler> peerInHandlers; //maps from peerID to IncomingHandler for that peer (unnecessary because just need a peer's outbox)
//    private HashMap<String, OutgoingHandler> peerOutHandlers; //maps from peerID to OutgoingHandler for that peer (unnecessary because just need a peer's outbox)

    private HashSet<String> peers;
    private HashSet<String> choked;
    private HashSet<String> unchoked;
    private HashSet<String> interested;
    private HashSet<String> chokingMe;
//    private ConcurrentHashMap<String, BitSet> peerPieces; //maps from peerID to the bitfield for that peer (which pieces that peer has)
    private HashMap<String, BitSet> peerPieces; //don't actually see a need for concurrency here, because processing inbox serially in 1 thread.
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

    /* could potentially move all of this functionality into IncomingHandler, and pass in self so each Handler has access to data structures
    * Would need to make the data structures concurrent, but then would be multithreaded processing of messages */
    public void dispatch() {
        try {
            Message message = inbox.take();
            processMessage(message);
        } catch(InterruptedException e) {
            e.printStackTrace();
            System.out.println("Interruped exception in peer process dispatch method");
        }
    }


    /* could potentially move all of this functionality into IncomingHandler, and pass in self so each Handler has access to data structures
    * Would need to make the data structures concurrent, but then would be multithreaded processing of messages */
    public void processMessage(Message message) {

        switch (message.getType()) {
            case -1: //unfinished
                break;
            case Constants.CHOKE:
                processChoke(message);
                break;
            case Constants.UNCHOKE:
                processUnchoke(message);
                break;
            case Constants.INTERESTED:
                processInterested(message);
                break;
            case Constants.NOT_INTERESTED:
                processNotInterested(message);
                break;
            case Constants.HAVE:
                try {
                    processHave(message);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception in processMessage's processHave section");
                }
                break;
            case Constants.BITFIELD:
                processBitField(message);
                break;
            case Constants.REQUEST:
                processRequest(message);
                break;
            case Constants.PIECE:
                processPiece(message);
                break;

        }
    }

    private void processChoke(Message message) {
        chokingMe.add(message.getFrom());
    }

    private void processUnchoke(Message message) {
        chokingMe.remove(message.getFrom());
    }

    private void processInterested(Message message) { //Does an "interested" only apply to a single piece index? Or every piece? Looks like every piece.
        interested.add(message.getFrom());
    }

    private void processNotInterested(Message message) {
        interested.remove(message.getFrom());
    }

    private void processHave(Message message) throws InterruptedException {
        int pieceIndex = Util.bytesToInt(message.getPayload());
        int responseType;
        int responseLength = 1; // 1 byte for message type. No payload for 'interested' and not_interested' messages.
        if(bitfield.get(pieceIndex)) {
            responseType = Constants.NOT_INTERESTED;
        } else {
            responseType = Constants.INTERESTED;
        }
        Message response = new Message(this.peerID, responseLength, responseType);
        outboxes.get(message.getFrom()).put(response); //get the outbox for the peerID associated with the received message and put in a response.
        // put throws InterruptedException. add(response) should also work, but I guess this is preferred.
    }

    private void processBitField(Message message) {
        BitSet peerBitfield = BitSet.valueOf(message.getPayload());
        peerPieces.putIfAbsent(message.getFrom(), peerBitfield);
    }

    private void processRequest(Message message) {

    }

    private void processPiece(Message message) {

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
