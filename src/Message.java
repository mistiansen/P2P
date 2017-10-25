public class Message {

    private String from; //peerID of sender
    private int payloadLength;
    private int type;
    private byte[] payload;

    public Message(String from, int payloadLength, int type, byte[] payload) {
        this.from = from; // peerID of sender
        this.payloadLength = payloadLength;
        this.type = type;
        this.payload = payload;
    }

    public Message(String from, int payloadLength, int type) { //not all messages have a payload
        this.from = from;
        this.payloadLength = payloadLength;
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
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
