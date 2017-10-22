import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
	
	private String peerID;
	private BufferedWriter logFile;
	
	public Logger(String peerID) throws IOException{
		this.peerID = peerID;
		String fileName = "/project/log_peer_" + peerID + ".log";
		logFile = new BufferedWriter(new FileWriter(fileName, true));
	}
	
	// when the peer initiated the connection to another peer
	public void logMadeTCPConnection(String connectedPeerID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" makes a connection to Peer " + connectedPeerID + "\n");
	}
	
	// when the peer accepted the connection from another peer
	public void logAcceptedTCPConnection(String connectedPeerID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" is connected from Peer " + connectedPeerID + "\n");
	}
	
	// when the peer changes its preferred neighbors
	public void logPreferredNeighbors(String...neighborIDs) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" has the preferred neighbors ");
		for(int i=0; i<neighborIDs.length-1; i++){
			logFile.append(neighborIDs[i] + ",");
		}
		logFile.append(neighborIDs[neighborIDs.length-1] + "\n");
	}
	
	// when the peer changes its optimistically unchoked neighbor
	public void logOptimisticallyUnchokedNeighbor(String neighborID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" has the optimistically unchoked neighbor " + neighborID + "\n");
	}
	
	// when the peer is unchoked by a neighbor (which means when the peer recieves an unchoking message from a neighbor)
	public void logUnchokedByNeighbor(String neighborID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" is unchoked by " + neighborID + "\n");
	}
	
	// when the peer is choked by a neighbor (which means when the peer recieves a choking message from a neighbor)
	public void logChokedByNeighbor(String neighborID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" is choked by " + neighborID + "\n");
	}
	
	// when the peer receives a ‘have’ message
	public void logRecievedHave(String otherPeerID, String pieceIndex) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" received the ‘have’ message from " + otherPeerID + 
				 "for the piece " + pieceIndex + "\n");
	}

	// when the peer receives an ‘interested’ message
	public void logRecievedInterested(String otherPeerID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" received the ‘interested’ message from " + otherPeerID + "\n");
	}
	
	// when the peer receives an ‘not interested’ message
	public void logRecievedNotInterested(String otherPeerID) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" received the ‘not interested’ message from " + otherPeerID + "\n");
	}
	
	// when the peer finishes downloading a piece
	public void logDoneDownloadingPiece(String otherPeerID, String pieceIndex, int totalPieces) throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" has downloaded the piece " + pieceIndex + " from " + 
				otherPeerID + ". Now the number of pieces it has is " + 
				totalPieces + "\n");
	}
	
	// when the peer downloads the complete file
	public void logFileDownloadComplete() throws IOException{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logFile.append(timeStamp + "Peer " + peerID + 
				" has downloaded the complete file\n");
	}
	
	// actually write to the file
	public void done() throws IOException{
		logFile.close();
	}
	
	

}
