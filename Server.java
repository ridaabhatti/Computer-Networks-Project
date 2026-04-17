import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Server.java
 * Listens for clients, starts one thread per connection (ClientHandler),
 * and writes every log line to a text file plus the console.
 */
public class Server {

    /** Port must match Client.java (default 6789). */
    private static final int PORT = 6789;

    /** All connections and requests are appended here. */
    private static final String LOG_FILE = "server_log.txt";

    /** One pattern for timestamps in the log file and console. */
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Open once; we append lines as events happen. */
    private final BufferedWriter fileLog;

    /**
     * Opens (or creates) the log file in append mode so old runs are kept.
     */
    public Server() throws IOException {
        // true = append, do not wipe the file each time the server starts
        this.fileLog = new BufferedWriter(new FileWriter(LOG_FILE, true));
    }

    /**
     * Starts the TCP server and accepts clients forever.
     */
    public void start() throws IOException {
        // Prints to console and logs the same line to the file
        log("Server starting on port " + PORT);

        // Open a server socket and keep listening forever
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Block until a client connects
                Socket clientSocket = serverSocket.accept();

                // Handle this client on its own thread so many clients can talk at once
                ClientHandler handler = new ClientHandler(clientSocket, this);
                Thread t = new Thread(handler);
                t.start();
            }
        }
    }

    /**
     * Called when a client finishes JOIN — keeps a record of who connected and when.
     */
    public void registerClient(String name, LocalDateTime connectTime, String address) {
        // Save who connected, when, and from where
        String line = "REGISTER name=" + name
                + " connectTime=" + connectTime.format(TIME_FMT)
                + " address=" + address;
        log(line);
    }

    /**
     * Called when a client sends QUIT — logs how long they stayed.
     */
    public void deregisterClient(String name, LocalDateTime disconnectTime, Duration session) {
        // Save who disconnected and how long they were connected
        String line = "DEREGISTER name=" + name
                + " disconnectTime=" + disconnectTime.format(TIME_FMT)
                + " sessionSeconds=" + session.getSeconds();
        log(line);
    }

    /**
     * Writes one line to the console and the same line to server_log.txt.
     * Synchronized so many client threads do not mix their lines in the file.
     */
    public synchronized void log(String message) {
        String stamp = LocalDateTime.now().format(TIME_FMT);
        String full = "[" + stamp + "] " + message;

        System.out.println(full);

        try {
            fileLog.write(full);
            fileLog.newLine();
            fileLog.flush();
        } catch (IOException e) {
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }

    /** Program entry point: create the server and run until stopped. */
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
