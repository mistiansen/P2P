
import java.util.concurrent.BlockingQueue;

public class IncomingHandler implements Runnable {

    private Connection connection;
    BlockingQueue<byte[]> inbox;

    public IncomingHandler(BlockingQueue<byte[]> inbox, Connection connection) {
        this.inbox = inbox;
        this.connection = connection;
    }

    private void receive() {
        byte[] in = connection.receive();
        try {
            this.inbox.put(in);
        } catch(InterruptedException e) {
            e.printStackTrace();
            System.out.println("Interrupted exception in IncomingHandler receive");
        }
    }

    @Override
    public void run() {
        while(true) { //use a timeout mechanism instead?
            receive();
        }
    }
}
