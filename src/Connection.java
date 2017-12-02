
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;


public class Connection {

    private String peerID; //peerID of the peer you are connected to (not your (peerProcess') peerID)
    private Socket socket;
    private OutputStream out;
    private InputStream in;

    public Connection(String peerID, Socket socket) {
        this.peerID = peerID;
        this.socket = socket;
        try {
            this.out = this.socket.getOutputStream();
            this.in = this.socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to establish IO stream in Connection");
        }
    }

    public Connection(Socket socket) {
        this.socket = socket;
        try {
            this.out = this.socket.getOutputStream();
            this.in = this.socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to establish IO stream in Connection");
        }
    }

    public String getPeerID() {
        return peerID;
    }

    public void setPeerID(String peerID) {
        this.peerID = peerID;
    }

    public void send(byte[] out) {
        try {
            System.out.println("Attempting send in Connection to " + peerID);
//            System.out.println("Attempting send in Connection to " + peerID + ". Bytes: " + new String(out));
            this.out.write(out);
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Unable to send in connection");
        }
    }


    public String checkHandshake() throws IOException {
        byte[] header = new byte[18];
        byte[] zeroes = new byte[10];
        byte[] ID = new byte[4];
        in.read(header, 0, 18);
        in.read(zeroes, 0, 10);
        in.read(ID, 0, 4);
        String headerString = new String(header);
        String peer = new String(ID);
        System.out.println("In checkHandshake(). Received " + headerString + " from peer " + peer);
        if (headerString.equals(Constants.HANDSHAKE)) {
            return peer;
        } else {
            return "";
        }
    }

    public void reciprocateHandshake(String myID) throws IOException { //myID is the peerID of the peer calling this function (peerProcess) ("Hi, I'm...")
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte) 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Constants.HANDSHAKE.getBytes());
        out.write(zeroes);
        out.write(myID.getBytes());
        this.send(out.toByteArray());
    }

    public void initiateHandshake(String myID) throws IOException { //myID is the peerID of the peer calling this function (peerProcess)
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

    public void sendBitfield(BitSet bitfield) throws IOException {
        byte type = (byte) Constants.BITFIELD;
        byte[] bits = bitfield.toByteArray();
        int msgLength = bits.length + 1; // add 1 because of the message type byte.
        byte[] length = ByteBuffer.allocate(4).putInt(msgLength).array();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outStream.write(length);
        outStream.write(type);
        outStream.write(bits);
//        System.out.println("Attempting to send bitfield");
        System.out.println("Sending " + bitfield);
        this.out.write(outStream.toByteArray());

    }


    /* Our message length field does NOT include the message type field */
    public Message receive() throws IOException {
//        InputStream is = new ByteArrayInputStream(msg);
        byte[] length = new byte[4]; //grab the first 4 bytes, which hold the message length
        in.read(length, 0, 4); //read the first 4 bytes into byte array
        int payloadLength = Util.bytesToInt(length) - 1; //convert the first 4 bytes to an int; subtract 1 for the message type
        System.out.println("Got payload length variable of " + payloadLength + " in connection receive ");
        int messageType = in.read(); //read the next byte, which holds the message type
        if (messageType == 0 || messageType == 1 || messageType == 2 || messageType == 3) { //choke, unchoke, interested, not_interested don't have payloads
            return new Message(this.peerID, payloadLength, messageType);
        } else {
            //byte[] payload = ByteBuffer.allocate(msgLength).array(); //does this work?
            byte[] payload = new byte[payloadLength]; //allocate a byte array for the message payload. OR is it msgLength - 1 because of msgType field?
            in.read(payload, 0, payloadLength); //Read remaining bytes into payload. (or payload = is.readAllBytes()? Or does that only work with Java 9?)
            return new Message(this.peerID, payloadLength, messageType, payload); //create and return a new message
        }
    }

    public void close() throws IOException {
        this.socket.close();
    }


}
