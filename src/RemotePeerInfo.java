
public class RemotePeerInfo implements Comparable<RemotePeerInfo> {
	private String peerId;
	private String peerHost;
	private int peerPort;
	
	public RemotePeerInfo(String peerId, String peerHost, int peerPort) {
		this.peerId = peerId;
		this.peerHost = peerHost;
		this.peerPort = peerPort;
	}


	public String getPeerId() {
		return peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}

	public String getPeerHost() {
		return peerHost;
	}

	public void setPeerHost(String peerHost) {
		this.peerHost = peerHost;
	}

	public int getPeerPort() {
		return peerPort;
	}

	public void setPeerPort(int peerPort) {
		this.peerPort = peerPort;
	}

    public int compareTo(RemotePeerInfo peer) {
//        return this.getPeerId().compareTo(peer.getPeerId());
        return peer.getPeerId().compareTo(this.getPeerId()); //want to return descending order
    }
}
