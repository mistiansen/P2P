

import java.util.concurrent.BlockingQueue;

public class OutgoingHandler implements Runnable {

    private Connection connection;
    private BlockingQueue<byte[]> outbox;

    public OutgoingHandler(BlockingQueue<byte[]> outbox, Connection connection) {
        this.outbox = outbox;
        this.connection = connection;
    }

    private void send() {
        try {
            connection.send(outbox.take()); //also available: poll (which does the same thing but can specify wait time)
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) { // maybe have some timeout variable instead?
            send();
        }
    }
}
