public class Message {

    private String from; //peerID of sender
    private int length;
    private int type;
    private byte[] payload;

    public Message(String from, int length, int type, byte[] payload) {
        this.from = from; // peerID of sender
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    public Message(String from, int length, int type) { //not all messages have a payload
        this.from = from;
        this.length = length;
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}
