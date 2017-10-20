
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class OutgoingHandler implements Runnable {

    private Connection connection;
    private BlockingQueue<Message> outbox;

    public OutgoingHandler(BlockingQueue<Message> outbox, Connection connection) {
        this.outbox = outbox;
        this.connection = connection;
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
        out.write(ByteBuffer.allocate(4).putInt(message.getLength()).array());
        out.write((byte) message.getType());
        out.write(message.getPayload());
        return out.toByteArray();
    }

    @Override
    public void run() {
        while(true) { // maybe have some timeout variable instead?
            send();
        }
    }
}
