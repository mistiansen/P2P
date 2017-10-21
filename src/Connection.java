import jdk.vm.ci.meta.Constant;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Connection {

    private String peerID;
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

    public void send(byte[] out) {
        try {
            this.out.write(out);
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Unable to send in connection");
        }
    }

//    public byte[] receive() { //need to double check on what multiple reads do
//        //probably want to readAllBytes then use a ByteInputStreamReader to parse the length field and message type
////        byte[] message = new byte[Constants.MESSAGE_SIZE]; //better idea might be to read first 4 bytes to get size, then allocate based on that
//        byte[] lengthField = new byte[4];
////        byte [] lengthField = ByteBuffer.allocate(4);
//        try {
//            this.in.read(lengthField, 0, 4);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.out.println("Unable to read length in Connection.receive");
//        }
//        int msgLength = Util.bytesToInt(lengthField);
//        byte[] message = new byte[msgLength + 1]; //need to consider the
//        try {
//            message = this.in.readAllBytes(); //will this also read in the message length field again?
//        } catch(IOException e) {
//            e.printStackTrace();
//        }
//        return message;
//    }


    public boolean reciprocateHandshake(String myID, String handshakePeer) throws IOException {
        byte[] header = new byte[18];
        byte[] zeroes = new byte[10];
        byte[] ID = new byte[4];
        in.read(header, 0, 18);
        in.read(zeroes, 0, 10);
        in.read(ID, 0, 4);
        String headerString = new String(header);
        String peer = Integer.toString(Util.bytesToInt(ID));
        if (headerString.equals(Constants.HANDSHAKE) && peer.equals(handshakePeer)) {
            zeroes = new byte[10];
            Arrays.fill(zeroes, (byte) 0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(header);
            out.write(zeroes);
            out.write(myID.getBytes());
            this.send(out.toByteArray());
            return true; //NOT FINISHED
        } else {
            return false;
        }
    }

    public boolean initiateHandshake(String myID, String handshakePeer) throws IOException {
        byte[] header = ByteBuffer.allocate(18).put(Constants.HANDSHAKE.getBytes()).array();
        byte[] zeroes = new byte[10];
        byte[] peerID = ByteBuffer.allocate(4).put(myID.getBytes()).array();
        Arrays.fill(zeroes, (byte) 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header);
        out.write(zeroes);
        out.write(peerID);
        this.send(out.toByteArray());
        return true; //not finished
    }

    public Message receive() throws IOException {
//        InputStream is = new ByteArrayInputStream(msg);
        byte[] length = new byte[4]; //grab the first 4 bytes, which hold the message length
        in.read(length, 0, 4); //read the first 4 bytes into byte array
        int msgLength = Util.bytesToInt(length); //convert the first 4 bytes to an int
        int messageType = in.read(); //read the next byte, which holds the message type
        if (messageType == 0 || messageType == 1 || messageType == 2 || messageType == 3) { //choke, unchoke, interested, not_interested don't have payloads
            return new Message(this.peerID, msgLength, messageType);
        } else {
            //byte[] payload = ByteBuffer.allocate(msgLength).array(); //does this work?
            byte[] payload = new byte[msgLength]; //allocate a byte array for the message payload
            in.read(payload, 0, msgLength); //Read remaining bytes into payload. (or payload = is.readAllBytes()? Or does that only work with Java 9?)
            return new Message(this.peerID, msgLength, messageType, payload); //create and return a new message
        }
    }


}
