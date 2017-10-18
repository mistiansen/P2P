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

    public static void setMessageSize(int messageSize) {
        MESSAGE_SIZE = messageSize;
    }


}
