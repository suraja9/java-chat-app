package server;

import util.AESUtil;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 1234;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static SecretKeySpec key;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool;
    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // Initialize thread pool for handling clients
        threadPool = Executors.newCachedThreadPool();

        try {
            System.out.print("Enter shared password: ");
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String password = consoleReader.readLine();
            key = AESUtil.getKeyFromPassword(password);

            // Add shutdown hook for graceful server shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(ChatServer::shutdown));

            serverSocket = new ServerSocket(PORT);
            log("Server started on port " + PORT + ". Waiting for clients...");

            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    log("New client connection from: " + socket.getRemoteSocketAddress());

                    // Handle client in thread pool
                    ClientHandler handler = new ClientHandler(socket, key, clients);
                    clients.add(handler);
                    threadPool.submit(handler);

                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        log("Error accepting client connection: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        log("Client removed. Active clients: " + clients.size());
    }

    // Fixed: Now includes sender in broadcast so they can see their own messages
    public static void broadcastToAll(String message, ClientHandler sender) {
        List<ClientHandler> disconnectedClients = new ArrayList<>();

        synchronized (clients) {
            for (ClientHandler client : clients) {
                // CHANGED: Removed the condition that excluded sender
                // Now sender also receives their own messages
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    log("Failed to send message to client: " + e.getMessage());
                    disconnectedClients.add(client);
                }
            }
        }

        // Remove disconnected clients
        for (ClientHandler client : disconnectedClients) {
            clients.remove(client);
        }
    }

    // NEW: Send current user list to a specific client
    public static void sendUserListToClient(ClientHandler newClient) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != newClient && client.getUsername() != null) {
                    try {
                        String userJoinMessage = "🟢 " + client.getUsername() + " has joined the chat";
                        newClient.sendMessage(userJoinMessage);
                    } catch (Exception e) {
                        log("Failed to send user list to new client: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void shutdown() {
        log("Shutting down server...");

        try {
            // Close all client connections
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        log("Error closing client: " + e.getMessage());
                    }
                }
                clients.clear();
            }

            // Shutdown thread pool
            if (threadPool != null) {
                threadPool.shutdown();
            }

            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

        } catch (Exception e) {
            log("Error during shutdown: " + e.getMessage());
        }

        log("Server shutdown complete.");
    }

    public static void log(String message) {
        String timestamp = LocalDateTime.now().format(LOG_FORMAT);
        System.out.println("[" + timestamp + "] " + message);
    }

    public static int getActiveClientCount() {
        return clients.size();
    }
}
