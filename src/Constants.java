public final class Constants {

    private Constants() { }

    public static final int CHOKE = 0;
    public static final int UNCHOKE = 1;
    public static final int INTERESTED = 2;
    public static final int NOT_INTERESTED = 3;
    public static final int HAVE = 4;
    public static final int BITFIELD = 5;
    public static final int REQUEST = 6;
    public static final int PIECE = 7;
    public static int MESSAGE_SIZE = 32773; //default piece size in bytes
//    public static final int HEADER_SIZE = 5; //number of bytes for the message length and message type fields (all but payload)
    public static final String HANDSHAKE = "P2PFILESHARINGPROJ";
    public static final int CONNECT_TIMEOUT = 10000; // wait for 10 secs (10,000 ms)

    public static int NUM_PREF_NEIGHBORS;
    public static int UNCHOKE_INTERVAL;
    public static int OPT_UNCHOKE_INTERVAL;
    public static String FILE_NAME;
    public static int FILE_SIZE;
    public static int PIECE_SIZE;

    public static void setMessageSize(int messageSize) {
        MESSAGE_SIZE = messageSize;
    }


    public static void setNumPrefNeighbors(int numPrefNeighbors) {
        NUM_PREF_NEIGHBORS = numPrefNeighbors;
    }

    public static void setUnchokeInterval(int unchokeInterval) {
        UNCHOKE_INTERVAL = unchokeInterval;
    }

    public static void setOptUnchokeInterval(int optUnchokeInterval) {
        OPT_UNCHOKE_INTERVAL = optUnchokeInterval;
    }

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public static void setFileSize(int fileSize) {
        FILE_SIZE = fileSize;
    }

    public static void setPieceSize(int pieceSize) {
        PIECE_SIZE = pieceSize;
    }
}
