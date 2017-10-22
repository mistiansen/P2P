import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;

public class InConnectHandler implements Runnable {


    private Socket socket;
    private HashSet<String> expectedPeers;
    private OutputStream out;
    private InputStream in;

    public InConnectHandler(HashSet<String> peers, Socket socket) {
        this.expectedPeers = peers;
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
        if (headerString.equals(Constants.HANDSHAKE) && expectedPeers.contains(peer)) {
            return true; //will check whether this is a valid peer in peerProcess
        } else {
            return false;
        }
    }


    public void reciprocateHandshake(String myID) throws IOException { //myID is the peerID of the peer calling this function (peerProcess) ("Hi, I'm...")
//        if (checkHandshake()) {
        System.out.println("Entered reciprocate handshake");
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte) 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Constants.HANDSHAKE.getBytes());
        out.write(zeroes);
        out.write(myID.getBytes());

        this.out.write(out.toByteArray());
//            return true; //NOT FINISHED
//        } else {
//            return false;
//        }
    }


    @Override
    public void run() {

    }
}
