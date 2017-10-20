
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

public class IncomingHandler implements Runnable {

    private String peerID; // peerID associated with this handler. Necessary so that can know who messages are from.
    private Connection connection;
    BlockingQueue<Message> inbox;

    public IncomingHandler(BlockingQueue<Message> inbox, Connection connection) {
        this.inbox = inbox;
        this.connection = connection;
    }

    private void receive() {
        byte[] in = connection.receive();
        try {
            Message message = parseIncoming(in);
            this.inbox.put(message);
        } catch(InterruptedException e) {
            System.out.println("Interrupted exception in IncomingHandler receive");
        } catch (IOException e) {
            System.out.println("IOException in IncomingHandler receive caused by parseMessage");
        }
    }

    private Message parseIncoming(byte[] msg) throws IOException {

        InputStream is = new ByteArrayInputStream(msg);
        byte[] length = new byte[4]; //grab the first 4 bytes, which hold the message length
        is.read(length, 0, 4); //read the first 4 bytes into byte array
        int msgLength = Util.bytesToInt(length); //convert the first 4 bytes to an int
        int messageType = is.read(); //read the next byte, which holds the message type
        if (messageType == 0 || messageType == 1 || messageType == 2 || messageType == 3) { //choke, unchoke, interested, not_interested don't have payloads
            return new Message(this.peerID, msgLength, messageType);
        } else {
            //byte[] payload = ByteBuffer.allocate(msgLength).array(); //does this work?
            byte[] payload = new byte[msgLength]; //allocate a byte array for the message payload
            is.read(payload, 0, msgLength); //Read remaining bytes into payload. (or payload = is.readAllBytes()? Or does that only work with Java 9?)
            return new Message(this.peerID, msgLength, messageType, payload); //create and return a new message
        }
    }

    @Override
    public void run() {
        while(true) { //use a timeout mechanism instead?
            receive();
        }
    }
}
