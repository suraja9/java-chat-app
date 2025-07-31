package server;

import util.AESUtil;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SecretKeySpec key;
    private List<ClientHandler> clientList;
    private String username;
    private boolean isRunning;
    private long lastActivity;
    private boolean userListSent = false; // NEW: Track if user list was sent

    private static final int SOCKET_TIMEOUT = 60000; // INCREASED: 60 seconds instead of 30

    public ClientHandler(Socket socket, SecretKeySpec key, List<ClientHandler> clientList) throws IOException {
        this.socket = socket;
        this.key = key;
        this.clientList = clientList;
        this.isRunning = true;
        this.lastActivity = System.currentTimeMillis();

        // Set socket timeout for read operations
        socket.setSoTimeout(SOCKET_TIMEOUT);

        // ADDED: Keep socket alive to prevent disconnections
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);

        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // First message is the encrypted username
            String encryptedUsername = in.readLine();
            if (encryptedUsername == null) {
                ChatServer.log("Client disconnected before sending username");
                return;
            }

            username = AESUtil.decrypt(encryptedUsername, key);
            ChatServer.log("User '" + username + "' joined the chat");

            // FIXED: Send current user list to new client BEFORE broadcasting join message
            ChatServer.sendUserListToClient(this);
            userListSent = true;

            // Broadcast join message to all clients (including this one)
            String joinMessage = "🟢 " + username + " has joined the chat";
            ChatServer.broadcastToAll(joinMessage, this);
            lastActivity = System.currentTimeMillis();

            String line;
            while (isRunning && (line = in.readLine()) != null) {
                try {
                    String decrypted = AESUtil.decrypt(line, key);
                    ChatServer.log("Message from " + username + ": " + decrypted);

                    // Broadcast message to all clients (including sender)
                    ChatServer.broadcastToAll(decrypted, this);
                    lastActivity = System.currentTimeMillis();

                } catch (Exception e) {
                    ChatServer.log("Failed to decrypt message from " + username + ": " + e.getMessage());
                    // IMPROVED: Only send error message to the sender, not broadcast
                    try {
                        sendMessage("[Server] Failed to decrypt your message");
                    } catch (Exception sendError) {
                        ChatServer.log("Failed to send error message to " + username);
                        break; // Connection is likely broken
                    }
                }
            }

        } catch (SocketTimeoutException e) {
            ChatServer.log("Client " + username + " timed out - connection idle too long");
        } catch (SocketException e) {
            if (isRunning) {
                ChatServer.log("Client " + username + " disconnected: " + e.getMessage());
            }
        } catch (IOException e) {
            if (isRunning) {
                ChatServer.log("IO error with client " + username + ": " + e.getMessage());
            }
        } catch (Exception e) {
            ChatServer.log("Unexpected error with client " + username + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public void sendMessage(String message) throws Exception {
        if (!isRunning || socket.isClosed()) {
            throw new IOException("Client connection is closed");
        }

        try {
            String encrypted = AESUtil.encrypt(message, key);
            out.println(encrypted);

            // Check if the message was sent successfully
            if (out.checkError()) {
                throw new IOException("Failed to send message - PrintWriter error");
            }

        } catch (Exception e) {
            ChatServer.log("Failed to send message to " + username + ": " + e.getMessage());
            throw e;
        }
    }

    private void cleanup() {
        isRunning = false;

        // Remove from client list
        ChatServer.removeClient(this);

        // Broadcast leave message if we have a username
        if (username != null) {
            String leaveMessage = "🔴 " + username + " has left the chat";
            ChatServer.broadcastToAll(leaveMessage, this);
            ChatServer.log("User '" + username + "' left the chat");
        }

        // Close resources
        close();
    }

    public void close() {
        isRunning = false;

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            ChatServer.log("Error closing input stream for " + username + ": " + e.getMessage());
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            ChatServer.log("Error closing output stream for " + username + ": " + e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            ChatServer.log("Error closing socket for " + username + ": " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return isRunning && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public String getClientInfo() {
        if (socket != null) {
            return username + " (" + socket.getRemoteSocketAddress() + ")";
        }
        return username + " (disconnected)";
    }
}