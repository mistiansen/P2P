import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class StartPeers {

    public Vector<RemotePeerInfo> peerInfoVector;

    public void getConfiguration(String configFile) {
        String st;
        peerInfoVector = new Vector<RemotePeerInfo>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(configFile));
            while ((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                int port = Integer.parseInt(tokens[2]);
                this.peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], port));
            }
            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public void startRemote() {
        try {
//            StartRemotePeers myStart = new StartRemotePeers(); // artifacts from when this was main()
//            myStart.getConfiguration();
            getConfiguration("PeerInfo.cfg");
            // get current path
            String path = System.getProperty("user.dir"); //this is the directory where java program is being run from
            System.out.println("Got path: " + path);
            // start clients at remote hosts
            for (int i = 0; i < peerInfoVector.size(); i++) {
                RemotePeerInfo pInfo = (RemotePeerInfo) peerInfoVector.elementAt(i);
                System.out.println("Start remote peer " + pInfo.getPeerId() + " at " + pInfo.getPeerHost());
                // *********************** IMPORTANT *************************** //
                // If your program is JAVA, use this line.
//                Runtime.getRuntime().exec("ssh " + pInfo.getPeerHost() + " cd " + path + "; java peerProcess " + pInfo.getPeerId());
                String command = "ssh " + pInfo.getPeerHost() + " cd " + path + "; java peerProcess " + pInfo.getPeerId();
//                String command = " cd " + path + "; java peerProcess " + pInfo.getPeerId();
                System.out.println("Attempting command: " + command);
                Runtime.getRuntime().exec(command);
            }
            System.out.println("Starting all remote peers has done.");
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void startLocal() {
        getConfiguration("PeerInfo.cfg");
        for (int i = 0; i < peerInfoVector.size(); i++) {
            RemotePeerInfo pInfo = (RemotePeerInfo) peerInfoVector.elementAt(i);
            System.out.println("Attempting to start peer: " + pInfo.getPeerId());
            new Thread(new peerProcess(pInfo.getPeerId())).start();
        }
    }

    public void run() {
        startLocal();
    }

    public static void main(String[] args) {
//        new Thread(new StartPeers()).start();
        new StartPeers().startLocal();
    }


}
