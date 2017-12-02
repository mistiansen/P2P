

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class peerProcess implements Runnable {

    private String myPeerID;
    private int myPort;
    private int numConnecting;
    private int numConnectTo;
    private int numPieces;

    private BlockingQueue<Message> inbox = new LinkedBlockingQueue<>(); //one inbox for this process. Look into timed BlockingQueue "offer" method for timeout?
    private HashMap<String, BlockingQueue> outboxes = new HashMap<>(); //maps a peerID to its outbox. Look into timed BlockingQueue "offer" method for timeout?

    private HashSet<IncomingHandler> inHandlers = new HashSet<>(); //one IncomingHandler for each connection to another peer
    private HashSet<OutgoingHandler> outHandlers = new HashSet<>(); //one OutgoinggHandler for each connection to another peer

    private HashSet<String> toAccept; // peers to accept incoming connections from
    private HashSet<RemotePeerInfo> toInitiate; //peers to initiate connections to
    private HashSet<String> peers; //hashSet of PeerID strings
    private HashMap<String, RemotePeerInfo> peerInfo;
    private HashSet<String> choked = new HashSet<>();
    private String optUnchoked = "";
    private HashSet<String> unchoked = new HashSet<>();
    private HashSet<String> interested = new HashSet<>();
    private HashSet<String> chokingMe = new HashSet<>();
    //    private ConcurrentHashMap<String, BitSet> peerPieces; //maps from peerID to the bitfield for that peer (which pieces that peer has)
    private HashMap<String, BitSet> peerPieces = new HashMap<>(); //don't actually see a need for concurrency here, because processing inbox serially in 1 thread.
    /* Do we need to know bitfields for each peer? I guess so to be able to make requests. Updated on receipt of "have" message. */
//    private HashMap<String, Integer> count = new HashMap<>();
    private ConcurrentHashMap<String, Integer> count = new ConcurrentHashMap<>();

    private AtomicIntegerArray pieces; //create a new class with this underneath? Then use it like a bitset? No no no...don't need concurrency here. Just update as get pieces.
    // so when a piece is received this bitfield is adjusted. If get multiple pieces? I guess doesn't matter. Or can safely overwrite? Not an issue I think
    // remember, this is for just 1 peer. so we choose who gets requested and whether to send multiple requests. Have a requested BitSet?
    private boolean haveFile;
    private boolean peersHaveFile;
    private BitSet bitfield = new BitSet();
    private BitSet requested = new BitSet(); // So maybe only send 1 request for a piece per round. Then when receive or at timeout, unset the bit.
    private BitSet need = new BitSet();
    private String filename;
    private byte[] contents = new byte[Constants.FILE_SIZE];
//    private ByteArrayOutputStream contentStream = new ByteArrayOutputStream();


    private Logger logger;
    private int totalPieces = 0;

    public peerProcess(String myPeerID) {
        this.myPeerID = myPeerID;
        filename = "peers/" + myPeerID + "/" + Constants.FILE_NAME;
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
                } else if (remotePeerID > Integer.parseInt(this.myPeerID)) { //if the peerID in the config is greater than my peerID, expect incoming connection
                    toAccept.add(tokens[0]);
                    peerInfo.put(tokens[0], peer);
                    peers.add(tokens[0]);
                    numConnecting++;
                } else if (remotePeerID == Integer.parseInt(this.myPeerID)) {
                    haveFile = Util.strToBool(tokens[3]);
                    myPort = Integer.parseInt(tokens[2]);
                    System.out.println("my peerID is: " + this.myPeerID + ". My port is : " + myPort);
                    numPieces = (int) Math.ceil(Constants.FILE_SIZE / (Constants.PIECE_SIZE * 1.0));
                    if (haveFile) {
                        System.out.println("Peer " + remotePeerID + " has file");
                        this.bitfield.set(0, numPieces); //sets bits from 0 to numPieces exclusive (so a total of numPieces bits)
                        contents = Files.readAllBytes(Paths.get(filename));
                        System.out.println("READ FILE IT IS " + contents.length + " bytes long");
                    } else {
                        System.out.println("Peer " + remotePeerID + " does not have file");
//                        this.bitfield.clear(0, numPieces); //don't actually need to do this because the default is 0/false
                        this.need.set(0, numPieces);
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
    public void processMessage(Message message) throws IOException, InterruptedException {

        switch (message.getType()) {
            case -1: //unfinished
                break;
            case Constants.CHOKE:
                System.out.println("Received a CHOKE message in peerProcess " + myPeerID + " FROM " + message.getFrom() + "Going to process it.");
                processChoke(message);
                break;
            case Constants.UNCHOKE:
                System.out.println("Received an UNCHOKE in peerProcess " + myPeerID + " FROM " + message.getFrom() + " Going to process it.");
                processUnchoke(message);
                break;
            case Constants.INTERESTED:
                System.out.println("Received an INTERESTED message type in peerProcess " + myPeerID + " FROM " + message.getFrom() + " Going to process it.");
                processInterested(message);
                break;
            case Constants.NOT_INTERESTED:
                System.out.println("Received a NOT INTERESTED message type in peerProcess " + myPeerID + " FROM " + message.getFrom() + ". Going to process it.");
                processNotInterested(message);
                break;
            case Constants.HAVE:
                try {
                    processHave(message);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception in processMessage's processHave section");
                    e.printStackTrace();
                }
                break;
            case Constants.BITFIELD:
                System.out.println("Received a bitfield message type in " + myPeerID + " from " + message.getFrom() + " Going to process it.");
                processBitField(message);
                break;
            case Constants.REQUEST:
                System.out.println("Received a REQUEST MESSAGE IN peer " + myPeerID + ". Going to process it.");
                processRequest(message);
                break;
            case Constants.PIECE:
                processPiece(message);
                break;
        }
    }

    private void processChoke(Message message) throws IOException {
        chokingMe.add(message.getFrom());
        logger.logChokedByNeighbor(message.getFrom());
    }

    // TODO: 11/30/17 need to check bitfield differently
    private void processUnchoke(Message message) throws IOException {
        logger.logUnchokedByNeighbor(message.getFrom());
        String unchokedMe = message.getFrom();
        chokingMe.remove(unchokedMe);
        BitSet toRequest = (BitSet) this.need.clone();
//        toRequest.andNot(this.requested); //bits set in the bitfield that are not set in requested (bits needed but not requested)
        toRequest.and(peerPieces.get(unchokedMe)); //only want to request if they have it (I need and they have it)
        toRequest.andNot(requested);
        if (!toRequest.isEmpty()) {
            int requestIndex = toRequest.nextSetBit(0); //get the next needed but not yet requested bit (piece that don't have)
            System.out.println(myPeerID + " was unchoked and is requesting piece " + requestIndex + " from peer (processUnchoke)" + unchokedMe);
            requestPiece(unchokedMe, requestIndex);
        }
    }

    private void processInterested(Message message) throws IOException { //Does an "interested" only apply to a single piece index? Or every piece? Looks like every piece.
        interested.add(message.getFrom());
        logger.logRecievedInterested(message.getFrom());
    }

    private void processNotInterested(Message message) throws IOException {
        interested.remove(message.getFrom());
        logger.logRecievedNotInterested(message.getFrom());
    }

    private void processHave(Message message) throws InterruptedException, IOException {
        int pieceIndex = Util.bytesToInt(message.getPayload());
        peerPieces.get(message.getFrom()).set(pieceIndex);
        logger.logRecievedHave(message.getFrom(), Integer.toString(pieceIndex));
        int responseType;
        int responseLength = 1; // 1 byte for message type. No payload for 'interested' and not_interested' messages.
        if (!bitfield.get(pieceIndex)) {
            System.out.println("Got HAVE from " + message.getFrom() + " for piece " + pieceIndex + ". " + myPeerID + "'s bitfield is " + bitfield + ". I do need " + pieceIndex + ". Sending INTERESTED ");
            responseType = Constants.INTERESTED;
            Message response = new Message(this.myPeerID, responseLength, responseType);
            outboxes.get(message.getFrom()).put(response); //get the outbox for the peerID associated with the received message and put in a response.
        }
        // put throws InterruptedException. add(response) should also work, but I guess this is preferred.
        isEveryoneElseCompleted();
    }

    private void processBitField(Message message) throws InterruptedException {
        String fromPeer = message.getFrom();
        BitSet peerBitfield = BitSet.valueOf(message.getPayload());
        peerPieces.putIfAbsent(fromPeer, peerBitfield);
        System.out.println("Peer" + myPeerID + " Received bitfield " + peerBitfield + " from peer " + message.getFrom());
        System.out.println("Peer " + message.getFrom() + " pieces in " +  myPeerID + " is now " + peerPieces.get(message.getFrom()));
        if (need.intersects(peerBitfield)) { //they HAVE SOMETHING we want
            Message interested = new Message(myPeerID, 0, Constants.INTERESTED); //PAYLOAD length was 1, changed to 0. Think should be 0. the 1 byte is added in send.
            outboxes.get(fromPeer).put(interested);
        }
    }

    private void processRequest(Message message) throws IOException, InterruptedException {
        /* first  see what piece is requested */
        byte[] indexHolder = message.getPayload(); //grab the request payload containing the requested piece index
        int pieceIndex = Util.bytesToInt(indexHolder); //convert the first 4 bytes to an int
        System.out.println(myPeerID + " RECEIVED REQUEST FOR PIECE " + pieceIndex + " from peer " + message.getFrom());
        if (bitfield.get(pieceIndex) && (unchoked.contains(message.getFrom()) || optUnchoked.equals(message.getFrom()))) { //if we have the piece and the requestor is unchoked
            System.out.println(myPeerID + " granting peer " + message.getFrom() + "'s request for piece " + pieceIndex);
            byte[] byteFile = Files.readAllBytes(Paths.get(filename)); //or do new FileInputStream(filename)?
            int start = pieceIndex * Constants.PIECE_SIZE; // each read reads from index: start to index: Constants.PIECE_SIZE - 1
            System.out.println("Trying to read file starting from " + start + " to " + (start + Constants.PIECE_SIZE));
            System.out.println("IN processRequest. My bitfield is " + bitfield+myPeerID);
            byte[] content = Arrays.copyOfRange(byteFile, start, start+Constants.PIECE_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(indexHolder);
            out.write(content);
            byte[] payload = out.toByteArray();
            Message response = new Message(this.myPeerID, Constants.PIECE_SIZE + 4, Constants.PIECE, payload); // message len is the size of the piece + 4 bytes piece index field
            outboxes.get(message.getFrom()).put(response);
//            logger.logRecievedRequest(message.getFrom(), message.getType());
        } else {
            System.out.println("Peer " + myPeerID + " just processed a request from " + message.getFrom() + " but they aren't unchoked");
            return;
        }
    }

    // TODO: 12/1/17 Need to create different file/directory for each peer
    /* Do we just store the pieces in a data structure then write to file when all present? */
    private void processPiece(Message message) throws IOException, InterruptedException {
        int pieceLength = message.getPayloadLength() - 4; //the length of the piece should be the messageLength - type portion - index portion
        InputStream is = new ByteArrayInputStream(message.getPayload());
        byte[] indexField = new byte[4]; //grab the first 4 bytes, which hold the message length
        byte[] piece = new byte[pieceLength];
        try {
        	byte[] byteFile = Files.readAllBytes(Paths.get(filename));
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            is.read(indexField, 0, 4); //read the first 4 bytes into byte array
            is.read(piece, 0, pieceLength);
            int pieceIndex = Util.bytesToInt(indexField);
            int start=pieceIndex*Constants.PIECE_SIZE;
            for (int i=0; i<Constants.PIECE_SIZE && (start+i)<Constants.FILE_SIZE; i++) {
            	byteFile[start+i]=piece[i];
            }
            fileOutputStream.write(byteFile);

            bitfield.set(pieceIndex); //think pieceIndex is index of first bit of a piece
            need.clear(pieceIndex);
            System.out.println(myPeerID + " just got a piece from " + message.getFrom() + " of length " + piece.length);
            logger.logDoneDownloadingPiece(message.getFrom(), Integer.toString(pieceIndex), ++totalPieces);
            //count.put(message.getFrom(), count.get(message.getFrom()) + 1);
            byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            for (String peer: outboxes.keySet()) {
                outboxes.get(peer).put(new Message(myPeerID, 4, Constants.HAVE, payload));
                BitSet needed = (BitSet) need.clone();
                needed.and(peerPieces.get(peer)); //needed and peer has it
                if (needed.isEmpty()) {
                    outboxes.get(peer).put(new Message(myPeerID, 0, Constants.NOT_INTERESTED));
                }
            }
            amICompleted();
            if (!haveFile){
              BitSet toRequest = (BitSet) this.need.clone();
              toRequest.and(peerPieces.get(message.getFrom())); //only want to request if they have it (I need and they have it)
              toRequest.andNot(requested);
              if (!toRequest.isEmpty()) {
                  int requestIndex = toRequest.nextSetBit(0); //get the next needed but not yet requested bit (piece that don't have)
                  requestPiece(message.getFrom(), requestIndex);
              }
            } else {
              logger.logFileDownloadComplete();
              System.out.println(myPeerID+" Completed File!");
//              if (peersHaveFile){
//            	  System.out.println("Process done exiting!");
//            	  System.exit(0);
//              }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Trying to process a piece but file not found.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOException in peerProcess processPiece() method");
            e.printStackTrace();
        }
    }

    /* This is sent in response to an unchoke */
    /* craft a message and put it in the outbox of the peer sending the unchoke */
    // TODO: 11/29/17 Need to check peerPieces data structure to see if that peer actually has the piece
    private void requestPiece(String peer, int pieceIndex) {
        System.out.println("NOW in requestPiece of peer " + myPeerID + ". Attempting to request piece " + pieceIndex + " from peer " + peer);
        try {
            byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            System.out.println("This is the payload in requestPiece in " + myPeerID + "..." + Arrays.toString(payload) + " should be " + pieceIndex);
            Message request = new Message(this.myPeerID, 4, Constants.REQUEST, payload); //payload length for requests is 1 byte type + 4 bytes piece index. NO...not including type.
            outboxes.get(peer).put(request); //throws InterruptedException
            requested.set(pieceIndex);
        } catch (InterruptedException e) {
            System.out.println("InterruptedException in peerProcess' requestPiece method");
            e.printStackTrace();
        }
    }

    private boolean amICompleted() {
        if (bitfield.cardinality() >= numPieces) {
            haveFile = true;
            try {
                FileOutputStream fileOut = new FileOutputStream(filename);
                fileOut.write(contents);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        } else {
            haveFile = false;
            return false;
        }
    }

    private boolean isEveryoneElseCompleted() {
        for (String peer : peers) {
            BitSet peerBitset = peerPieces.get(peer);
            if (peerBitset.cardinality() < numPieces) {
                peersHaveFile = false;
                return false;
            }
        }
        System.out.println(myPeerID+" noticed rest have finished!");
        peersHaveFile = true;
        return true;
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
                } else {
                    System.out.println("Got some sort of timeout in acceptConnections");
                    break;
                }
            }
            for (Connection request : incoming) {
                String requestor = request.checkHandshake();
                if (toAccept.contains(requestor)) { //if one fails does it break things? I don't think so. Well...it would break that peer if that peer tried to connect again (?).
                    request.setPeerID(requestor);
                    request.reciprocateHandshake(myPeerID);
                    request.sendBitfield(bitfield);
                    logger.logAcceptedTCPConnection(requestor);
                    inHandlers.add(new IncomingHandler(inbox, request)); //create new incoming handler for this connection. Give it my inbox and this connection. Add to HashSet of incomingHandlers.
                    BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();
                    outboxes.put(requestor, outbox);
                    outHandlers.add(new OutgoingHandler(request, outbox)); //each OutgoingHandler has its own outbox created in the constructor.
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
        for (RemotePeerInfo peer : sortedPeers) {
            System.out.println(peer.getPeerId());
        }
        for (RemotePeerInfo peer : sortedPeers) {
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
                    BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();
                    outboxes.put(peer.getPeerId(), outbox);
                    outHandlers.add(new OutgoingHandler(connection, outbox)); //each OutgoingHandler has its own outbox created in the constructor.
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


    /* could potentially move all of this functionality into IncomingHandler, and pass in self so each Handler has access to data structures
* Would need to make the data structures concurrent, but then would be multithreaded processing of messages */
    public void dispatch() {


        try {
            processMessage(inbox.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Interrupted exception in peer process dispatch method");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void timeout() {

      this.requested.clear();
//      BitSet toRequest = (BitSet) this.need.clone();
//      for(String peer : peers) {
//    	  if (!chokingMe.contains(peer)) {
//		      toRequest.and(peerPieces.get(peer)); //only want to request if they have it (I need and they have it)
////		      toRequest.andNot(requested);
//		      if (!toRequest.isEmpty()) {
//		          int requestIndex = toRequest.nextSetBit(0); //get the next needed but not yet requested bit (piece that don't have)
//		          System.out.println(myPeerID + " was unchoked and is requesting piece " + requestIndex + " from peer (processUnchoke)" + peer);
//		          requestPiece(peer, requestIndex);
//		      }
//    	  }
//      }
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
        if (!haveFile) {
        	byte[] createFile = new byte[Constants.FILE_SIZE];
        	try {
	        	FileOutputStream fileOutputStream = new FileOutputStream(filename);
	            fileOutputStream.write(createFile);
	            fileOutputStream.close();
        	} catch (Exception E) {
        		System.out.println("Failure to initialize file");
        	}
        }

        for (IncomingHandler inHandler : inHandlers) {
            new Thread(inHandler).start();
        }
        for (OutgoingHandler outHandler : outHandlers) {
            new Thread(outHandler).start();
        }

        new Thread(new UnchokeTimer()).start();
        new Thread(new OptUnchokeTimer()).start();

        while (!peersHaveFile || !haveFile) {
            dispatch();
        }

        System.out.println("All done. Bye!");
        System.exit(0);
    }


    public static void main(String[] args) {
        String peerID = args[0];
        peerProcess me = new peerProcess(peerID);
        me.run();
    }


    private class UnchokeTimer implements Runnable {

        public UnchokeTimer() {
            for (String peer: peers) {
                count.put(peer, 0);
            }
        }

        private void unchokeTimer() throws InterruptedException {
            while (!haveFile) {
                HashSet<String> newPrefs = new HashSet<>();
                int i = 0;
                for (String peer : peers) {
                    if (i < Constants.NUM_PREF_NEIGHBORS) {
                        newPrefs.add(peer);
                        i++;
                    } else {
                        int maxdiff = -1;
                        String replace = "";
                        for (String newpeer : newPrefs) {
                            int diff = count.get(peer) - count.get(newpeer);
                            if (maxdiff < diff) {
                                maxdiff = diff;
                                replace = newpeer;
                            }
                        }
                        if (maxdiff > -1) {
                            newPrefs.add(peer);
                            newPrefs.remove(replace);
                        }
                    }
                    count.put(peer, 0);
                }
                for (String newpeer : newPrefs) {
                    if (!unchoked.contains(newpeer)) {
                        Message response = new Message(myPeerID, 0, Constants.UNCHOKE);
                        outboxes.get(newpeer).put(response);
                    }
                }
                for (String oldpeer : unchoked) {
                    if (!newPrefs.contains(oldpeer)) {
                        Message response = new Message(myPeerID, 0, Constants.CHOKE);
                        outboxes.get(oldpeer).put(response);
                    }
                }
                unchoked.clear();
                unchoked = newPrefs;
                try {
                    logger.logPreferredNeighbors(Arrays.copyOf(unchoked.toArray(), unchoked.toArray().length, String[].class));
                } catch (IOException e) {
                    System.out.println(e.toString());
                }
                timeout();


                TimeUnit.SECONDS.sleep(Constants.UNCHOKE_INTERVAL);
            }
            TimeUnit.SECONDS.sleep(1);
            while (!peersHaveFile) {

                //Select k neighbors from interested peers
                HashSet<String> newPrefs = new HashSet<>();
                int intSize = interested.size();
                int selected = 0;
                for (String peer : interested) {
                    if (intSize > 0 && selected < Constants.NUM_PREF_NEIGHBORS) {
                        newPrefs.add(peer);
                    }
                    intSize--;
                    selected++;
                }
                for (String newpeer : newPrefs) {
                    if (!unchoked.contains(newpeer)) {
                        Message response = new Message(myPeerID, 0, Constants.UNCHOKE);
                        outboxes.get(newpeer).put(response);
                    }
                }
                for (String oldpeer : unchoked) {
                    if (!newPrefs.contains(oldpeer)) {
                        Message response = new Message(myPeerID, 0, Constants.CHOKE);
                        outboxes.get(oldpeer).put(response);
                    }
                }
                unchoked.clear();
                unchoked = newPrefs;

                try {
                    logger.logPreferredNeighbors(Arrays.copyOf(unchoked.toArray(), unchoked.toArray().length, String[].class)); // TODO: 12/1/17 CAUSING ARRAY OUT BOUNDS?
                } catch (IOException e) {
                    System.out.println(e.toString());
                }

                TimeUnit.SECONDS.sleep(Constants.UNCHOKE_INTERVAL);
            }
        }


        public void run() {
            try {
                unchokeTimer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class OptUnchokeTimer implements Runnable {


        private void optUnchokeTimer() throws InterruptedException {
        	TimeUnit.SECONDS.sleep(1);
            do {
                Random rnd = new Random();
//                TimeUnit.SECONDS.sleep(Constants.OPT_UNCHOKE_INTERVAL);
                //Create a list of interested peers that are not preferred neighbors and randomly pick one
                HashSet<String> optList = new HashSet<>();
                for (String p : interested) {
                    if (!unchoked.contains(p)) {
                        optList.add(p);
                    }
                }
                int randSize = optList.size();
                if (randSize > 0) {
                    int rand = rnd.nextInt(randSize);
                    int i = 0;
                    for (String p : optList) {
                        if (i == rand) {
                            //If you didnt pick the same optUnchoked peer then send and unchoke message
                            if (!optUnchoked.equals(p)) {
                                Message response1 = new Message(myPeerID, 0, Constants.UNCHOKE);
                                outboxes.get(p).put(response1);
                            }
                            //If the optUnchoked isn't a preferred peer then it is now choked
                            if (!unchoked.contains(optUnchoked) && optUnchoked!="" && !optUnchoked.equals(p)) {
                                Message response2 = new Message(myPeerID, 0, Constants.CHOKE);
                                outboxes.get(optUnchoked).put(response2);
                            }
                            optUnchoked = p;
                            try {
                                logger.logOptimisticallyUnchokedNeighbor(optUnchoked);
                            } catch (IOException e) {
                                System.out.println(e.toString());
                            }
                        }
                        i++;
                    }
                }
                TimeUnit.SECONDS.sleep(Constants.OPT_UNCHOKE_INTERVAL);

            } while (!peersHaveFile);
        }

        public void run() {
            try {
                optUnchokeTimer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


}
