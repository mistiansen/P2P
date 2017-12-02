
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OutgoingHandler implements Runnable {

    private Connection connection;
    private BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();


    public OutgoingHandler(Connection connection) {
        this.connection = connection;
    }

    public OutgoingHandler(Connection connection, BlockingQueue<Message> outbox) {
        this.connection = connection;
        this.outbox = outbox;
    }

    private void send() {
        try {
            connection.send(createOutgoing(outbox.take())); //also available: poll (which does the same thing but can specify wait time)
        } catch(InterruptedException e) {
            e.printStackTrace();
        } catch(IOException e) {
            System.out.println("IOException in OutgoingHandler send() caused by createOutgoing()");
        }
    }

    private byte[] createOutgoing(Message message) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.out.println("Here's the payload length in OutHandler's createOutgoing " + message.getPayloadLength());
        int messageLength = message.getPayloadLength() + 1;
        out.write(ByteBuffer.allocate(4).putInt(messageLength).array()); //have to add 1 for the type field
        out.write((byte) message.getType());
        if (message.getPayload()!=null) {
            out.write(message.getPayload());
        }
        return out.toByteArray();
    }

    @Override
    public void run() {
//        System.out.println("Started outgoing handler to handle connection with " + connection.getPeerID());
        while(true) { // maybe have some timeout variable instead?
            send();
        }
    }
}
