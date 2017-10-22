
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;

public abstract class ConnectionHandler {

    private Socket socket;
    private HashSet<String> expectedPeers;
    private OutputStream out;
    private InputStream in;

    public ConnectionHandler(HashSet<String> peers, Socket socket) {
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


}
