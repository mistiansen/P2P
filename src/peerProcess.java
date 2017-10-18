import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

public class peerProcess {

    private int peerID;

    private BlockingQueue<byte[]> inbox; //one inbox for this process
    private HashMap<String, BlockingQueue> outboxes; //maps a peerID to its outbox

    private HashSet<IncomingHandler> inHandlers;
    private HashSet<OutgoingHandler> outHandlers;

    private HashSet<String> peers;
    private HashSet<String> choked;
    private HashSet<String> unchoked;
    private HashSet<String> interested;


    public peerProcess(int peerID) {
        this.peerID = peerID;
    }

    public void dispatch() {
        byte[] message = new byte[Constants.MESSAGE_SIZE];
        try {
            message = inbox.take();
        } catch(InterruptedException e) {
            e.printStackTrace();
            System.out.println("Interruped exception in peer process dispatch method");
        }
        processMessage(message);

    }

    public void processMessage(byte[] message) {
        InputStream is = new ByteArrayInputStream(message);
        int messageType = -1;
        try {
            messageType = is.read();
        } catch(IOException e) {
            e.printStackTrace();
        }
        switch (messageType) {
            case -1: //unfinished

        }




    }


    public static void main(String[] args) {

    }

}
