// Represents one parsed protocol line received from a client.
public class Request {

    private final String type;
    private final String payload;
    private final String sender;

    private Request(String type, String payload, String sender) {
        this.type = type;
        this.payload = payload;
        this.sender = sender;
    }

    public static Request parse(String rawMessage, String sender) {
        String type = Protocol.getType(rawMessage);
        String payload = Protocol.getPayload(rawMessage);
        return new Request(type, payload, sender);
    }

    public boolean isRequest() {
        return Protocol.REQUEST.equals(type);
    }

    public boolean isQuit() {
        return Protocol.QUIT.equals(type);
    }

    public boolean isJoin() {
        return Protocol.JOIN.equals(type);
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "Request{type=" + type
                + ", payload='" + payload + "'"
                + ", sender='" + sender + "'}";
    }
}
