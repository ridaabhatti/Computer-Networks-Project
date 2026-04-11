import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Client.java
 * A Math Server client that:
 *   1. Connects to the server and sends a JOIN with its name.
 *   2. Waits for the server's ACK confirming a successful connection.
 *   3. Automatically sends at least 3 math expressions at random intervals.
 *   4. Sends a QUIT message to gracefully close the connection.
 *
 * Usage:
 *   java Client <name> [host] [port]
 *
 *   name  – the display name sent to the server (required)
 *   host  – server hostname or IP (default: 127.0.0.1)
 *   port  – server port           (default: 6789)
 *
 * Example:
 *   java Client Alice
 *   java Client Bob 192.168.1.10 6789
 */
public class Client {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int    DEFAULT_PORT = 6789;

    /**
     * Pool of math expressions the client will choose from randomly.
     * Covers all supported operators: +  -  *  /  %  ^  and parentheses.
     */
    private static final String[] EXPRESSION_POOL = {
        "3 + 4",
        "10 - 3 * 2",
        "100 / 4",
        "17 % 5",
        "2 ^ 8",
        "(1 + 2) * (3 + 4)",
        "2 ^ 3 ^ 2",
        "-5 + 20",
        "3.5 * 2",
        "(10 % 3) ^ 2",
        "50 / (2 + 3)",
        "7 * 7 - 1",
        "((4 + 6) * 2) / 4",
        "9 ^ 0.5",
        "100 % 7 + 2 ^ 3"
    };

    /** Minimum delay between automatic requests (milliseconds). */
    private static final int MIN_DELAY_MS = 1000;

    /** Maximum delay between automatic requests (milliseconds). */
    private static final int MAX_DELAY_MS = 3000;

    /** How many math requests to send before disconnecting. */
    private static final int NUM_REQUESTS = 5;

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        // --- Parse command-line arguments ------------------------------------
        if (args.length < 1) {
            System.err.println("Usage: java Client <name> [host] [port]");
            System.exit(1);
        }
        String clientName = args[0];
        String host       = (args.length > 1) ? args[1] : DEFAULT_HOST;
        int    port       = DEFAULT_PORT;
        if (args.length > 2) {
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port '" + args[2] +
                                   "'. Using default: " + DEFAULT_PORT);
            }
        }

        // --- Connect and run -------------------------------------------------
        System.out.println("Client [" + clientName + "] starting...");
        System.out.println("Connecting to " + host + ":" + port);

        try (Socket socket = new Socket(host, port)) {

            BufferedReader in  = new BufferedReader(
                                     new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(
                                     new OutputStreamWriter(socket.getOutputStream()), true);

            // Step 1: Send JOIN
            String joinMsg = Protocol.joinMessage(clientName);
            out.println(joinMsg);
            System.out.println("[SENT]     " + joinMsg);

            // Step 2: Wait for ACK
            String ackMsg = in.readLine();
            if (ackMsg == null) {
                System.err.println("Server closed the connection before sending ACK.");
                return;
            }
            System.out.println("[RECEIVED] " + ackMsg);

            String ackType = Protocol.getType(ackMsg);
            if (!Protocol.ACK.equals(ackType)) {
                System.err.println("Expected ACK but got: " + ackMsg);
                return;
            }
            System.out.println("Successfully connected to the Math Server!\n");

            // Step 3: Send math requests at random intervals
            Random rng = new Random();

            for (int i = 1; i <= NUM_REQUESTS; i++) {
                // Pick a random expression from the pool
                String expression = EXPRESSION_POOL[rng.nextInt(EXPRESSION_POOL.length)];
                String requestMsg = Protocol.requestMessage(expression);

                out.println(requestMsg);
                System.out.println("[SENT]     (" + i + "/" + NUM_REQUESTS + ") " + requestMsg);

                // Read the server's RESPONSE or ERROR
                String serverReply = in.readLine();
                if (serverReply == null) {
                    System.err.println("Server closed connection unexpectedly.");
                    break;
                }
                System.out.println("[RECEIVED] " + serverReply);

                // Wait a random amount of time before the next request
                // (skip the delay after the last request)
                if (i < NUM_REQUESTS) {
                    int delay = MIN_DELAY_MS + rng.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
                    System.out.println("           (waiting " + delay + " ms)\n");
                    Thread.sleep(delay);
                }
            }

            // Step 4: Send QUIT
            System.out.println();
            String quitMsg = Protocol.quitMessage(clientName);
            out.println(quitMsg);
            System.out.println("[SENT]     " + quitMsg);

            // Read the server's farewell ACK
            String farewell = in.readLine();
            if (farewell != null) {
                System.out.println("[RECEIVED] " + farewell);
            }

            System.out.println("\nClient [" + clientName + "] has disconnected. Goodbye!");

        } catch (ConnectException e) {
            System.err.println("Could not connect to " + host + ":" + port +
                               ". Is the server running?");
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Client interrupted.");
        }
    }
}

