
import com.sun.tools.internal.jxc.ap.Const;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class peerProcess {

    private String myPeerID;
    private int port;
    private int numConnecting;
    private int numConnectTo;

    private BlockingQueue<Message> inbox; //one inbox for this process
    private HashMap<String, BlockingQueue> outboxes; //maps a peerID to its outbox

    private HashSet<IncomingHandler> inHandlers;
    private HashSet<OutgoingHandler> outHandlers;
//    private HashMap<String, IncomingHandler> peerInHandlers; //maps from peerID to IncomingHandler for that peer (unnecessary because just need a peer's outbox)
//    private HashMap<String, OutgoingHandler> peerOutHandlers; //maps from peerID to OutgoingHandler for that peer (unnecessary because just need a peer's outbox)

    private HashSet<String> toAccept; // peers to accept incoming connections from
    private HashSet<RemotePeerInfo> toInitiate; //peers to initiate connections to
    private HashSet<String> peers;
    private HashSet<String> choked;
    private HashSet<String> unchoked;
    private HashSet<String> interested;
    private HashSet<String> chokingMe;
//    private ConcurrentHashMap<String, BitSet> peerPieces; //maps from peerID to the bitfield for that peer (which pieces that peer has)
    private HashMap<String, BitSet> peerPieces; //don't actually see a need for concurrency here, because processing inbox serially in 1 thread.
    /* Do we need to know bitfields for each peer? I guess so to be able to make requests. Updated on receipt of "have" message. */
    private HashMap<String, RemotePeerInfo> peerInfo;

    private AtomicIntegerArray pieces; //create a new class with this underneath? Then use it like a bitset? No no no...don't need concurrency here. Just update as get pieces.
    // so when a piece is received this bitfield is adjusted. If get multiple pieces? I guess doesn't matter. Or can safely overwrite? Not an issue I think
    // remember, this is for just 1 peer. so we choose who gets requested and whether to send multiple requests. Have a requested BitSet?
    private boolean haveFile;
    private BitSet bitfield;
    private BitSet requested; // So maybe only send 1 request for a piece per round. Then when receive or at timeout, unset the bit.


    public peerProcess(String myPeerID) {
        this.myPeerID = myPeerID;
    }

    public void processConfig() {
        String st;
        peerInfo = new HashMap<>();
        peers = new HashSet<>();
        toInitiate = new HashSet<>(); // peers we will initiate connection to
        toAccept = new HashSet<>(); // peers we will accepts connections from

        try {
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while ((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                if (Integer.parseInt(tokens[0]) < Integer.parseInt(this.myPeerID)) { //if the peerID in the config is less than my peerID, connect to it
                    toInitiate.add(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
                    peerInfo.put(tokens[0], new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
                    peers.add(tokens[0]);
                } else if(Integer.parseInt(tokens[0]) > Integer.parseInt(this.myPeerID)) {
                    toAccept.add(tokens[0]);
                    peerInfo.put(tokens[0], new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
                    peers.add(tokens[0]);
                } else if(Integer.parseInt(tokens[0]) == Integer.parseInt(this.myPeerID)) {
                    haveFile = Boolean.parseBoolean(tokens[3]);
                } else { //should never happen
                    System.out.println("Either parsed the wrong peerID when starting or the peerID for this process is not in the Config");
                    return; //how to fail gracefully?
                }
            }
            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
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
                try {
                    processRequest(message);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("ERROR processing request for piece (interrupted)");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("ERROR processing request for piece (IOException)");
                }
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
        Message response = new Message(this.myPeerID, responseLength, responseType);
        outboxes.get(message.getFrom()).put(response); //get the outbox for the peerID associated with the received message and put in a response.
        // put throws InterruptedException. add(response) should also work, but I guess this is preferred.
    }

    private void processBitField(Message message) {
        BitSet peerBitfield = BitSet.valueOf(message.getPayload());
        peerPieces.putIfAbsent(message.getFrom(), peerBitfield);
    }

    private void processRequest(Message message) throws IOException, InterruptedException {
        /* first  see what piece is requested */
        byte[] indexHolder = message.getPayload(); //grab the request payload containing the requested piece index
        int pieceIndex = Util.bytesToInt(indexHolder); //convert the first 4 bytes to an int
        if (bitfield.get(pieceIndex) && unchoked.contains(message.getFrom())) { //if we have the piece and the requestor is unchoked
            FileInputStream fileInputStream = new FileInputStream(new File(Constants.FILE_NAME));
            int start = pieceIndex * Constants.PIECE_SIZE; // each read reads from index: start to index: Constants.PIECE_SIZE - 1
            byte[] content = new byte[Constants.PIECE_SIZE];
            fileInputStream.read(content, start, Constants.PIECE_SIZE); // Constants.PIECE_SIZE specifies the length of the read
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(indexHolder);
            out.write(content);
            byte[] payload = out.toByteArray();
            Message response = new Message(this.myPeerID, Constants.PIECE_SIZE + 5, Constants.PIECE, payload); // message len is the size of the piece + 1 byte msg type + 4 bytes piece index field
            outboxes.get(message.getFrom()).put(response);
        } else {
            return;
        }
    }

    private void processPiece(Message message) {

    }

    private void timeout() {

        requested.clear();

    }
    
    private void run() {
        processConfig();
        new InConnectHandler().acceptConnections();
        new OutConnectHandler().requestConnections();
        Iterator peerIterator = peers.iterator();
        while(peerIterator.hasNext()) {

        }
    }


    public static void main(String[] args) {


        String peerID = args[0];
        peerProcess me = new peerProcess(peerID);
        me.run();


    }





    private class InConnectHandler {

        private OutputStream out;
        private InputStream in;


        private boolean checkHandshake() throws IOException {
            byte[] header = new byte[18];
            byte[] zeroes = new byte[10];
            byte[] ID = new byte[4];
            in.read(header, 0, 18);
            in.read(zeroes, 0, 10);
            in.read(ID, 0, 4);
            String headerString = new String(header);
            String peer = new String(ID);
            System.out.println("In checkHandshake(). Received " + headerString + " from peer " + peer);
            if (headerString.equals(Constants.HANDSHAKE) && toAccept.contains(peer)) {
                return true; //will check whether this is a valid peer in peerProcess
            } else {
                return false;
            }
        }


        private void reciprocateHandshake() throws IOException { //myID is the peerID of the peer calling this function (peerProcess) ("Hi, I'm...")
            System.out.println("Entered reciprocate handshake");
            byte[] zeroes = new byte[10];
            Arrays.fill(zeroes, (byte) 0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(Constants.HANDSHAKE.getBytes());
            out.write(zeroes);
            out.write(myPeerID.getBytes());
            this.out.write(out.toByteArray());
        }

        private void sendBitfield() {


        }


        private void acceptConnections() { //maybe add a timer so that can proceed even if not all expected connections are attempted
            long start = System.currentTimeMillis();
            Vector<Socket> requests = new Vector<>(numConnecting);

            try {
                ServerSocket welcomeSocket = new ServerSocket(port);
                for (int i = 0; i < numConnecting ; i++) {
                    if ((System.currentTimeMillis() - start) < Constants.CONNECT_TIMEOUT) {
                        requests.add(welcomeSocket.accept());
                    } else {
                        break;
                    }
                }
                for(Socket socket: requests) {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    if (checkHandshake()) {
                        reciprocateHandshake();
                    } else {
                        continue;
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception in peerProcess's InConnectHandler's acceptConnections() method");
            }
        }

    }


    private class OutConnectHandler {

        private OutputStream out;
        private InputStream in;

        private boolean checkHandshake(String ConnectToPeerID) throws IOException {
            byte[] header = new byte[18];
            byte[] zeroes = new byte[10];
            byte[] ID = new byte[4];
            in.read(header, 0, 18);
            in.read(zeroes, 0, 10);
            in.read(ID, 0, 4);
            String headerString = new String(header);
            String peer = new String(ID);
            System.out.println("In checkHandshake(). Received " + headerString + " from peer " + peer);
            if (headerString.equals(Constants.HANDSHAKE) && peer.equals(ConnectToPeerID)) {
                return true; //will check whether this is a valid peer in peerProcess
            } else {
                return false;
            }
        }

        private void initiateHandshake(String myID) throws IOException { //myID is the peerID of the peer calling this function (peerProcess)
            byte[] header = ByteBuffer.allocate(18).put(Constants.HANDSHAKE.getBytes()).array();
            byte[] zeroes = new byte[10];
            byte[] peerID = ByteBuffer.allocate(4).put(myID.getBytes()).array();
            Arrays.fill(zeroes, (byte) 0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(header);
            out.write(zeroes);
            out.write(peerID);
//        this.send(out.toByteArray());
            this.out.write(out.toByteArray());
        }


        public void requestConnections() {


        }


    }

}
