import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Connection {

    private int peerID;
    private Socket socket;
    private OutputStream out;
    private InputStream in;

    public Connection(int peerID, Socket socket) {
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

    public byte[] receive() { //need to double check on what multiple reads do
        //probably want to readAllBytes then use a ByteInputStreamReader to parse the length field and message type
//        byte[] message = new byte[Constants.MESSAGE_SIZE]; //better idea might be to read first 4 bytes to get size, then allocate based on that
        byte[] lengthField = new byte[4];
//        byte [] lengthField = ByteBuffer.allocate(4);
        try {
            this.in.read(lengthField, 0, 4);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to read length in Connection.receive");
        }
        int msgLength = Util.bytesToInt(lengthField);
        byte[] message = new byte[msgLength + 1]; //need to consider the
        try {
            message = this.in.readAllBytes(); //will this also read in the message length field again?
        } catch(IOException e) {
            e.printStackTrace();
        }
        return message;
    }


}
