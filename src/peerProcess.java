

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class peerProcess implements Runnable {

    private String myPeerID;
    private int myPort;
    private int numConnecting;
    private int numConnectTo;

    private BlockingQueue<Message> inbox = new LinkedBlockingQueue<>(); //one inbox for this process. Look into timed BlockingQueue "offer" method for timeout?
    private HashMap<String, BlockingQueue> outboxes = new HashMap<>(); //maps a peerID to its outbox. Look into timed BlockingQueue "offer" method for timeout?

    private HashSet<IncomingHandler> inHandlers = new HashSet<>(); //one IncomingHandler for each connection to another peer
    private HashSet<OutgoingHandler> outHandlers = new HashSet<>(); //one OutgoinggHandler for each connection to another peer

    private HashSet<String> toAccept; // peers to accept incoming connections from
    private HashSet<RemotePeerInfo> toInitiate; //peers to initiate connections to
    private HashSet<String> peers; //hashSet of PeerID strings
    private HashMap<String, RemotePeerInfo> peerInfo;
    private HashSet<String> choked = new HashSet<>();
    private HashSet<String> unchoked = new HashSet<>();
    private HashSet<String> interested = new HashSet<>();
    private HashSet<String> chokingMe = new HashSet<>();
//    private ConcurrentHashMap<String, BitSet> peerPieces; //maps from peerID to the bitfield for that peer (which pieces that peer has)
    private HashMap<String, BitSet> peerPieces = new HashMap<>(); //don't actually see a need for concurrency here, because processing inbox serially in 1 thread.
    /* Do we need to know bitfields for each peer? I guess so to be able to make requests. Updated on receipt of "have" message. */

    private AtomicIntegerArray pieces; //create a new class with this underneath? Then use it like a bitset? No no no...don't need concurrency here. Just update as get pieces.
    // so when a piece is received this bitfield is adjusted. If get multiple pieces? I guess doesn't matter. Or can safely overwrite? Not an issue I think
    // remember, this is for just 1 peer. so we choose who gets requested and whether to send multiple requests. Have a requested BitSet?
    private boolean haveFile;
    private BitSet bitfield = new BitSet();
    private BitSet requested = new BitSet(); // So maybe only send 1 request for a piece per round. Then when receive or at timeout, unset the bit.

    private Logger logger;
    private int totalPieces = 0;
    
    public peerProcess(String myPeerID) {
        this.myPeerID = myPeerID;
    }

    public void processPeerConfig(String configFile) { //have to do this here so know what connections to initiate/wait for
        String st;
        peerInfo = new HashMap<>();
        peers = new HashSet<>();
        toInitiate = new HashSet<>(); // peers we will initiate connection to
        toAccept = new HashSet<>(); // peers we will accepts connections from
//        try {
//        	Logger.init(myPeerID);
//			logger = Logger.getInstance();
//		} catch (IOException e) {
//			 System.out.println(e.toString());
//	    }
        logger = new Logger(myPeerID);
        
        try {
            BufferedReader in = new BufferedReader(new FileReader(configFile));
            while ((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                int remotePeerID = Integer.parseInt(tokens[0]);
                int port = Integer.parseInt(tokens[2]);
                RemotePeerInfo peer = new RemotePeerInfo(tokens[0], tokens[1], port);
                if (remotePeerID < Integer.parseInt(this.myPeerID)) { //if the peerID in the config is less than my peerID, connect to it
                    toInitiate.add(peer);
                    peerInfo.put(tokens[0], peer);
                    peers.add(tokens[0]);
                    numConnectTo++;
                } else if(remotePeerID > Integer.parseInt(this.myPeerID)) { //if the peerID in the config is greater than my peerID, expect incoming connection
                    toAccept.add(tokens[0]);
                    peerInfo.put(tokens[0], peer);
                    peers.add(tokens[0]);
                    numConnecting++;
                } else if(remotePeerID == Integer.parseInt(this.myPeerID)) {
                    haveFile = Util.strToBool(tokens[3]);
                    myPort = Integer.parseInt(tokens[2]);
                    System.out.println("my peerID is: " + this.myPeerID + ". My port is : " + myPort);
                    int numPieces = (int)Math.ceil(Constants.FILE_SIZE / (Constants.PIECE_SIZE * 1.0));
                    if(haveFile) {
                        System.out.println("Peer " + remotePeerID + " has file");
                        this.bitfield.set(0, numPieces); //sets bits from 0 to numPieces exclusive (so a total of numPieces bits)
                    } else {
                        System.out.println("Peer " + remotePeerID + " does not have file");
                        this.bitfield.clear(0, numPieces); //don't actually need to do this because the default is 0/false
                    } //but then what does a receiver of this bitfield get? Nothing? Yeah...because don't actually need to send a bitfield if don't have the file
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
            processMessage(inbox.take());
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
        try {
			logger.logChokedByNeighbor(message.getFrom());
		} catch (IOException e) { 
			 System.out.println(e.toString());
		}
    }

    private void processUnchoke(Message message) {
        chokingMe.remove(message.getFrom());
        requestPiece(message.getFrom());
        try {
			logger.logUnchokedByNeighbor(message.getFrom());
		} catch (IOException e) { 
			 System.out.println(e.toString());
		}
    }

    private void processInterested(Message message) { //Does an "interested" only apply to a single piece index? Or every piece? Looks like every piece.
        interested.add(message.getFrom());
        try {
			logger.logRecievedInterested(message.getFrom());
		} catch (IOException e) { 
			 System.out.println(e.toString());
		}
    }

    private void processNotInterested(Message message) {
        interested.remove(message.getFrom());
        try {
			logger.logRecievedNotInterested(message.getFrom());
		} catch (IOException e) { 
			 System.out.println(e.toString());
		}
    }

    private void processHave(Message message) throws InterruptedException {
        int pieceIndex = Util.bytesToInt(message.getPayload());
        peerPieces.get(message.getFrom()).set(pieceIndex);
        try {
			logger.logRecievedHave(message.getFrom(), Integer.toString(pieceIndex));
		} catch (IOException e) { 
			 System.out.println(e.toString());
		}
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
            Message response = new Message(this.myPeerID, Constants.PIECE_SIZE + 4, Constants.PIECE, payload); // message len is the size of the piece + 4 bytes piece index field
            outboxes.get(message.getFrom()).put(response);
        } else {
            return;
        }
    }

    /* Do we just store the pieces in a data structure then write to file when all present? */
    private void processPiece(Message message) {
        int pieceLength = message.getPayloadLength() - 4; //the length of the piece should be the messageLength - type portion - index portion
        InputStream is = new ByteArrayInputStream(message.getPayload());
        byte[] indexField = new byte[4]; //grab the first 4 bytes, which hold the message length
        byte[] piece = new byte[pieceLength];

        try {
            is.read(indexField, 0, 4); //read the first 4 bytes into byte array
            is.read(piece, 0, pieceLength);
            int pieceIndex = Util.bytesToInt(indexField);
            FileOutputStream fileOutputStream = new FileOutputStream(Constants.FILE_NAME);
            fileOutputStream.write(piece, pieceIndex, pieceLength); //guess can write before have all pieces with this offset method
            bitfield.set(pieceIndex); //think pieceIndex is index of first bit of a piece
            try {
    			logger.logDoneDownloadingPiece(message.getFrom(), Integer.toString(pieceIndex), ++totalPieces);
    		} catch (IOException e) { 
    			 System.out.println(e.toString());
    		}
        } catch (FileNotFoundException e) {
            System.out.println("Trying to process a piece but file not found.");
        } catch (IOException e) {
            System.out.println("IOException in peerProcess processPiece() method");
        }
    }

    /* This is sent in response to an unchoke */
    /* craft a message and put it in the outbox of the peer sending the unchoke */
    // TODO: 11/29/17 Need to check peerPieces data structure to see if that peer actually has the piece 
    private void requestPiece(String peer) {
        BitSet toRequest = (BitSet) this.bitfield.clone();
        toRequest.andNot(this.requested); //bits set in the bitfield that are not set in requested (bits needed but not requested)
        int requestIndex = toRequest.nextSetBit(0); //get the next needed but not yet requested bit (piece that don't have)
        try {
            ByteArrayOutputStream payloadOut = new ByteArrayOutputStream();
            payloadOut.write(ByteBuffer.allocate(4).putInt(requestIndex).array()); //have to add 1 for the type field
            byte[] payload = payloadOut.toByteArray();
            Message request = new Message(this.myPeerID, 5, Constants.REQUEST, payload); //payload length for requests is 1 byte type + 4 bytes piece index
            outboxes.get(peer).put(request); //throws InterruptedException
        } catch (IOException e) {
            System.out.println("IOException in peerProcess' requestPiece method");
        } catch (InterruptedException e) {
            System.out.println("InterruptedException in peerProcess' requestPiece method");
        }


//        Message request = new Message(this.myPeerID, )

    }

    private void timeout() {

        this.requested.clear();

    }


        private void acceptConnections() { //maybe add a timer so that can proceed even if not all expected connections are attempted
            long start = System.currentTimeMillis();
            Vector<Connection> incoming = new Vector<>(numConnecting);
            System.out.println("In acceptConnections for peer: " + myPeerID + ". Expecting: " + numConnecting + " connection requests.");
            try {
                ServerSocket welcomeSocket = new ServerSocket(myPort);
                for (int i = 0; i < numConnecting; i++) {
                    if ((System.currentTimeMillis() - start) < Constants.CONNECT_TIMEOUT) {
                        Socket connect = welcomeSocket.accept();
                        System.out.println("Just accepted a connection in peerProcess " + myPeerID);
                        incoming.add(new Connection(connect));
                        System.out.println("End for loop in acceptConnections");
                    } else {
                        System.out.println("Got some sort of timeout in acceptConnections");
                        break;
                    }
                }
                System.out.println("Made it out of socket accepting for loop in acceptConnections for peer " + myPeerID);
                for (Connection request: incoming) {
                    String requestor = request.checkHandshake();
                    if (toAccept.contains(requestor)) { //if one fails does it break things? I don't think so. Well...it would break that peer if that peer tried to connect again (?).
                        request.setPeerID(requestor);
                        request.reciprocateHandshake(myPeerID);
                        request.sendBitfield(bitfield);
                        logger.logAcceptedTCPConnection(requestor);
                        inHandlers.add(new IncomingHandler(inbox, request)); //create new incoming handler for this connection. Give it my inbox and this connection. Add to HashSet of incomingHandlers.
                        outHandlers.add(new OutgoingHandler(request)); //each OutgoingHandler has its own outbox created in the constructor.
                    } else {
                        continue;
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception in peerProcess's InConnectHandler's acceptConnections() method. Use different port?");
                e.printStackTrace();
            }
        }


        public void requestConnections() {
            List<RemotePeerInfo> sortedPeers = new ArrayList<RemotePeerInfo>(toInitiate);
            Collections.sort(sortedPeers);
            for (RemotePeerInfo peer: sortedPeers) {
                System.out.println(peer.getPeerId());
            }
            for (RemotePeerInfo peer: sortedPeers) {
                try {
                    InetAddress address = InetAddress.getByName(peer.getPeerHost());
                    System.out.println("In requestConnections for peer " + myPeerID + ". Attempting connection to peer " + peer.getPeerId() + " host: " + peer.getPeerHost() + " address: " + address);
                    Connection connection = new Connection(peer.getPeerId(), new Socket(address, peer.getPeerPort()));
                    connection.initiateHandshake(myPeerID);
                    if (connection.checkHandshake().equals(peer.getPeerId())) {
                        System.out.println("Got a valid handshake in requestConnections() from peer " + peer.getPeerId());
                        logger.logMadeTCPConnection(peer.getPeerId());
                        connection.sendBitfield(bitfield);
                        inHandlers.add(new IncomingHandler(inbox, connection)); //create new incoming handler for this connection. Give it my inbox and this connection. Add to HashSet of incomingHandlers.
                        outHandlers.add(new OutgoingHandler(connection)); //each OutgoingHandler has its own outbox created in the constructor.
                    } else {
                        System.out.println("Got an improper handshake in requestConnections from peer: " + peer.getPeerId());
                        continue;
                    }
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host exception in OutConnectHandler's requestConnections() method");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("IOException in OutConnectHandler's requestConnections() method");
                    e.printStackTrace();
                }
            }
        }


    public void run() {
        try {
            ConfigParser.parseConfig(("config.cfg"));
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find the config file specified. Using defaults.");
        }
        processPeerConfig("PeerInfo.cfg");
        acceptConnections();
        requestConnections();
        Iterator peerIterator = peers.iterator();
        while (peerIterator.hasNext()) {
            System.out.println("In peerProcess for peer " + myPeerID + " here are peers: " + peerIterator.next());
        }
        // TODO: 11/29/17 need to actually start the inhandlers and outhandlers
//        Iterator inHandlerIterator = inHandlers.iterator();
//        while(inHandlerIterator.hasNext()) {
//            inHandlerIterator.next().
//        }
    }


    public static void main(String[] args) {
        String peerID = args[0];
        peerProcess me = new peerProcess(peerID);
        me.run();
    }

}
