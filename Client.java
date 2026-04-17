import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client.java
 * An interactive Math Server client that:
 *   1. Connects to the server and sends a JOIN with the client's name.
 *   2. Waits for the server's ACK confirming a successful connection.
 *   3. Prompts the user to type math expressions one at a time.
 *      Each expression is sent as a REQUEST and the server's result is printed.
 *   4. When the user types "quit", sends a QUIT message and terminates cleanly.
 *
 * Usage:
 *   java Client <n> [host] [port]
 *
 *   name  – display name sent to the server (required)
 *   host  – server hostname or IP        (default: 127.0.0.1)
 *   port  – server port                  (default: 6789)
 *
 * Examples:
 *   java Client Alice
 *   java Client Bob 192.168.1.10 6789
 *
 * Supported expression operators: +  -  *  /  %  ^  and ( )
 * Type "quit" (case-insensitive) to disconnect.
 */
public class Client {

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int    DEFAULT_PORT = 6789;

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        // --- Parse command-line arguments ------------------------------------
        if (args.length < 1) {
            System.err.println("Usage: java Client <n> [host] [port]");
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
        System.out.println("Connecting to " + host + ":" + port + "\n");

        try (Socket socket  = new Socket(host, port);
             Scanner stdin  = new Scanner(System.in)) {

            // Set up text I/O over the socket
            BufferedReader in  = new BufferedReader(
                                     new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(
                                     new OutputStreamWriter(socket.getOutputStream()), true);

            // Step 1: Send JOIN
            String joinMsg = Protocol.joinMessage(clientName);
            out.println(joinMsg);
            System.out.println("[SENT]     " + joinMsg);

            // Step 2: Wait for ACK from the server
            String ackMsg = in.readLine();
            if (ackMsg == null) {
                System.err.println("Server closed the connection before sending ACK.");
                return;
            }
            System.out.println("[RECEIVED] " + ackMsg);

            // Verify the server accepted the JOIN
            if (!Protocol.ACK.equals(Protocol.getType(ackMsg))) {
                System.err.println("Expected ACK but got: " + ackMsg);
                return;
            }

            System.out.println("\nSuccessfully connected! Type a math expression and press Enter.");
            System.out.println("Supported operators: +  -  *  /  %  ^  and ( )");
            System.out.println("Type \"quit\" to disconnect.\n");

            // Step 3: Interactive request loop
            while (true) {
                System.out.print("[" + clientName + "] > ");

                // Read one line of user input
                if (!stdin.hasNextLine()) {
                    // EOF (e.g. Ctrl+D on Unix / Ctrl+Z on Windows) — treat as quit
                    break;
                }
                String input = stdin.nextLine().trim();

                // Ignore blank lines
                if (input.isEmpty()) continue;

                // "quit" (any capitalisation) → send QUIT and exit the loop
                if (input.equalsIgnoreCase("quit")) break;

                // Send the expression as a REQUEST
                String requestMsg = Protocol.requestMessage(input);
                out.println(requestMsg);
                System.out.println("[SENT]     " + requestMsg);

                // Read and display the server's RESPONSE or ERROR
                String serverReply = in.readLine();
                if (serverReply == null) {
                    System.err.println("Server closed the connection unexpectedly.");
                    break;
                }
                System.out.println("[RECEIVED] " + serverReply + "\n");
            }

            // Step 4: Send QUIT and read farewell ACK
            System.out.println();
            String quitMsg = Protocol.quitMessage(clientName);
            out.println(quitMsg);
            System.out.println("[SENT]     " + quitMsg);

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
        }
    }
}
