// Defines the communication protocol used between the server and clients.
public class Protocol {

    // Message type constants
    public static final String JOIN = "JOIN";         // client -> server
    public static final String ACK = "ACK";           // server -> client
    public static final String REQUEST = "REQUEST";   // client -> server
    public static final String RESPONSE = "RESPONSE"; // server -> client
    public static final String ERROR = "ERROR";       // server -> client
    public static final String QUIT = "QUIT";         // client -> server

    // Separator between type and payload, e.g. "REQUEST|2+2"
    private static final String SEPARATOR = "|";

    public static String joinMessage(String name) {
        return build(JOIN, name);
    }

    public static String ackMessage(String text) {
        return build(ACK, text);
    }

    public static String requestMessage(String expression) {
        return build(REQUEST, expression);
    }

    public static String responseMessage(String result) {
        return build(RESPONSE, result);
    }

    public static String errorMessage(String reason) {
        return build(ERROR, reason);
    }

    public static String quitMessage(String name) {
        return build(QUIT, name);
    }

    // Extracts the type portion from "TYPE|payload".
    public static String getType(String rawMessage) {
        if (rawMessage == null) {
            return "";
        }
        int idx = rawMessage.indexOf(SEPARATOR);
        if (idx == -1) {
            return rawMessage.trim();
        }
        return rawMessage.substring(0, idx).trim();
    }

    // Extracts the payload portion from "TYPE|payload".
    public static String getPayload(String rawMessage) {
        if (rawMessage == null) {
            return "";
        }
        int idx = rawMessage.indexOf(SEPARATOR);
        if (idx == -1 || idx == rawMessage.length() - 1) {
            return "";
        }
        return rawMessage.substring(idx + 1).trim();
    }

    // Shared formatter for protocol lines.
    private static String build(String type, String payload) {
        return type + SEPARATOR + payload;
    }
}