
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/*
 * still need to add to peerProcess:
 * logFileDownloadComplete
 * done
 */

class Logger {
	
//	private static Logger singleton = null;
//
//    private Logger(String peerID) throws IOException {
//        this.peerID = peerID;
//        String fileName = "project/log_peer_" + peerID + ".log";
//        logFile = new BufferedWriter(new FileWriter(fileName));
//    }
//
//    public static Logger getInstance() {
//        if(singleton == null) {
//            throw new AssertionError("You have to call init first");
//        }
//
//        return singleton;
//    }
//
//    // must call init before getInstance()
//    public synchronized static Logger init(String peerID) throws IOException {
//        if (singleton != null)
//        {
//            // in my opinion this is optional, but for the purists it ensures
//            // that you only ever get the same instance when you call getInstance
//            throw new AssertionError("You already initialized me");
//        }
//
//        singleton = new Logger(peerID);
//        return singleton;
//    }

    private String peerID;
    private BufferedWriter logFile;

    public Logger(String peerID) {
        this.peerID = peerID;
        String fileName = "project/log_peer_" + peerID + ".log";
        try {
            logFile = new BufferedWriter(new FileWriter(fileName));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    // when the peer initiated the connection to another peer
    public void logMadeTCPConnection(String connectedPeerID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " makes a connection to Peer " + connectedPeerID + "\n");
        logFile.flush();
    }

    // when the peer accepted the connection from another peer
    public void logAcceptedTCPConnection(String connectedPeerID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " is connected from Peer " + connectedPeerID + "\n");
        logFile.flush();
    }

    // when the peer changes its preferred neighbors
    public void logPreferredNeighbors(String...neighborIDs) throws IOException{
    	if(neighborIDs.length == 0)
    		return;
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " has the preferred neighbors ");
        for(int i=0; i<neighborIDs.length-1; i++){
            logFile.append(neighborIDs[i] + ",");
        }
        logFile.append(neighborIDs[neighborIDs.length-1] + "\n");
        logFile.flush();
    }

    // when the peer changes its optimistically unchoked neighbor
    public void logOptimisticallyUnchokedNeighbor(String neighborID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " has the optimistically unchoked neighbor " + neighborID + "\n");
        logFile.flush();
    }

    // when the peer is unchoked by a neighbor (which means when the peer recieves an unchoking message from a neighbor)
    public void logUnchokedByNeighbor(String neighborID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " is unchoked by " + neighborID + "\n");
        logFile.flush();
    }

    // when the peer is choked by a neighbor (which means when the peer recieves a choking message from a neighbor)
    public void logChokedByNeighbor(String neighborID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " is choked by " + neighborID + "\n");
        logFile.flush();
    }

    // when the peer receives a ‘have’ message
    public void logRecievedHave(String otherPeerID, String pieceIndex) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " received the ‘have’ message from " + otherPeerID +
                " for the piece " + pieceIndex + "\n");
        logFile.flush();
    }

    // when the peer receives an ‘interested’ message
    public void logRecievedInterested(String otherPeerID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " received the ‘interested’ message from " + otherPeerID + "\n");
        logFile.flush();
    }

    // when the peer receives an ‘not interested’ message
    public void logRecievedNotInterested(String otherPeerID) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " received the ‘not interested’ message from " + otherPeerID + "\n");
        logFile.flush();
    }

    // when the peer finishes downloading a piece
    public void logDoneDownloadingPiece(String otherPeerID, String pieceIndex, int totalPieces) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " has downloaded the piece " + pieceIndex + " from " +
                otherPeerID + ". Now the number of pieces it has is " +
                totalPieces + "\n");
        logFile.flush();
    }

    // when the peer downloads the complete file
    public void logFileDownloadComplete() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        logFile.append(timeStamp + ": Peer " + peerID +
                " has downloaded the complete file\n");
        logFile.flush();
    }
    
    public void logRecievedRequest(String otherPeerID, int messageType) throws IOException{
    	 String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
         logFile.append(timeStamp + ": Peer " + peerID +
                 " recieved a request of type " + messageType + " from " + otherPeerID + "[TESTING]\n");
         logFile.flush();
    }

    // actually write to the file
    public void done() throws IOException{
        logFile.close();
    }
    
    /*
    public static void main(String args[]) throws IOException{
    	// test logger
    	Logger logger = new Logger("10");
    	logger.logMadeTCPConnection("12");
    	logger.logAcceptedTCPConnection("13");
    	logger.logPreferredNeighbors("1", "4", "24");
    	logger.logOptimisticallyUnchokedNeighbor("3");
    	logger.logUnchokedByNeighbor("7");
    	logger.logChokedByNeighbor("14");
    	logger.logRecievedHave("7", "0");
    	logger.logRecievedInterested("8");
    	logger.logRecievedNotInterested("19");
    	logger.logDoneDownloadingPiece("19", "3",8);
    	logger.logFileDownloadComplete();
    }
    */
    
}
