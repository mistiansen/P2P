import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;

public class OutConnectHandler implements Runnable {

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private String peerID;

    public OutConnectHandler(String peerID, Socket socket) {
        this.peerID = peerID;
        this.socket = socket;
        try {
            this.out = this.socket.getOutputStream();
            this.in = this.socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to establish IO stream in ConnectionHandler");
        }
    }

    public boolean checkHandshake() throws IOException {
        byte[] header = new byte[18];
        byte[] zeroes = new byte[10];
        byte[] ID = new byte[4];
        in.read(header, 0, 18);
        in.read(zeroes, 0, 10);
        in.read(ID, 0, 4);
        String headerString = new String(header);
        String peer = new String(ID);
        System.out.println("In checkHandshake(). Received " + headerString + " from peer " + peer);
        if (headerString.equals(Constants.HANDSHAKE) && peer.equals(this.peerID)) {
            return true; //will check whether this is a valid peer in peerProcess
        } else {
            return false;
        }
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
//        if (checkHandshake()) {
//            return true;
//        } else {
//            return false;
//        }
    }


    @Override
    public void run() {

    }
}
