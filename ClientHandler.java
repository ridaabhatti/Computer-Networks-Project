import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;

/**
 * ClientHandler.java
 * Runs on its own thread to manage a single client connection from start to finish.
 *
 * Responsibilities:
 *   1. Perform the JOIN handshake and register the client with the Server.
 *   2. Read incoming REQUEST messages, evaluate them via Calculator, and reply.
 *   3. Handle the QUIT message and cleanly close the connection.
 *   4. Log all significant events through Server's logging method.
 */
public class ClientHandler implements Runnable {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** The socket for this client's connection. */
    private final Socket socket;

    /** Reference to the Server so we can call its logging / tracking methods. */
    private final Server server;

    /** Reader for incoming text from the client. */
    private BufferedReader  in;

    /** Writer for outgoing text to the client. */
    private PrintWriter     out;

    /** The client's display name, set after a successful JOIN. */
    private String clientName = "UNKNOWN";

    /** Timestamp of when this client connected. */
    private final LocalDateTime connectTime;

    // Date-time formatter used in log messages
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param socket the accepted client socket
     * @param server reference to the Server instance (for logging)
     */
    public ClientHandler(Socket socket, Server server) {
        this.socket      = socket;
        this.server      = server;
        this.connectTime = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Runnable entry point
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        try {
            // Set up buffered text I/O over the socket streams
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Step 1: Wait for the JOIN message
            if (!handleJoin()) return; // malformed or missing JOIN → abort

            // Step 2: Service loop – handle REQUEST and QUIT messages
            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                Request req = Request.parse(rawMessage, clientName);
                server.log("[" + clientName + "] sent: " + rawMessage);

                if (req.isRequest()) {
                    handleMathRequest(req);
                } else if (req.isQuit()) {
                    handleQuit();
                    break; // exit the service loop
                } else {
                    // Unexpected message type – inform the client
                    out.println(Protocol.errorMessage(
                            "Unknown message type: " + req.getType()));
                }
            }

        } catch (IOException e) {
            server.log("[" + clientName + "] connection lost: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    // -------------------------------------------------------------------------
    // Protocol handlers
    // -------------------------------------------------------------------------

    /**
     * Read the first message from the client and validate it is a JOIN.
     * Sends an ACK on success, or an ERROR and closes on failure.
     *
     * @return true if the JOIN was valid and the client is registered
     */
    private boolean handleJoin() throws IOException {
        String rawMessage = in.readLine();
        if (rawMessage == null) {
            server.log("Client at " + socket.getInetAddress() +
                       " disconnected before sending JOIN.");
            return false;
        }

        String type    = Protocol.getType(rawMessage);
        String payload = Protocol.getPayload(rawMessage);

        if (!Protocol.JOIN.equals(type) || payload.isEmpty()) {
            out.println(Protocol.errorMessage("First message must be JOIN|<name>"));
            server.log("Rejected client at " + socket.getInetAddress() +
                       " – invalid JOIN: " + rawMessage);
            return false;
        }

        clientName = payload;

        // Register with the server (adds to user-tracking map)
        server.registerClient(clientName, connectTime, socket.getInetAddress().toString());

        // Acknowledge the client
        out.println(Protocol.ackMessage("Welcome " + clientName +
                    "! You are now connected to the Math Server."));

        server.log("[" + clientName + "] joined from " +
                   socket.getInetAddress() + " at " + connectTime.format(FMT));
        return true;
    }

    /**
     * Evaluate a math expression and send the result (or an error) back.
     *
     * @param req the parsed REQUEST message
     */
    private void handleMathRequest(Request req) {
        String expression = req.getPayload();
        server.log("[" + clientName + "] requested: " + expression);

        try {
            double result = Calculator.evaluate(expression);
            String response = Protocol.responseMessage(expression + " = " + result);
            out.println(response);
            server.log("[" + clientName + "] result sent: " + expression + " = " + result);

        } catch (IllegalArgumentException e) {
            out.println(Protocol.errorMessage("Could not evaluate \"" +
                        expression + "\": " + e.getMessage()));
            server.log("[" + clientName + "] bad expression: " + e.getMessage());
        }
    }

    /**
     * Process a QUIT message: acknowledge, log disconnect with session duration,
     * and clean up the server's tracking entry.
     */
    private void handleQuit() {
        LocalDateTime disconnectTime = LocalDateTime.now();

        // Calculate how long the client was connected
        Duration session = Duration.between(connectTime, disconnectTime);
        String duration  = formatDuration(session);

        out.println(Protocol.ackMessage(
                "Goodbye " + clientName + "! Session duration: " + duration));

        server.log("[" + clientName + "] disconnected at " +
                   disconnectTime.format(FMT) + " | Session duration: " + duration);

        // Remove from the server's active-client map
        server.deregisterClient(clientName, disconnectTime, session);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Close the socket quietly after the handler exits. */
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            server.log("Error closing socket for [" + clientName + "]: " + e.getMessage());
        }
    }

    /**
     * Format a Duration as "Xh Ym Zs" for human-readable log output.
     */
    private static String formatDuration(Duration d) {
        long hours   = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0)   return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}

