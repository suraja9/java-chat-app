package client;

import util.AESUtil;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ClientConnection {
    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private SecretKeySpec key;
    private String username;
    private Consumer<String> messageHandler;
    private AtomicBoolean isConnected;
    private AtomicBoolean shouldReconnect;
    private Thread listenerThread;

    public ClientConnection(String serverAddress, int serverPort, String password, String username, Consumer<String> onMessageReceived) throws Exception {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.username = username;
        this.messageHandler = onMessageReceived;
        this.key = AESUtil.getKeyFromPassword(password);
        this.isConnected = new AtomicBoolean(false);
        this.shouldReconnect = new AtomicBoolean(true);
        connect();
    }

    private void connect() throws IOException {
        try {
            socket = new Socket(serverAddress, serverPort);

            // IMPROVED: Better socket configuration to prevent disconnections
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(60000); // 60 second timeout

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isConnected.set(true);

            // Send encrypted username first
            try {
                String encryptedUsername = AESUtil.encrypt(username, key);
                out.println(encryptedUsername);
            } catch (Exception e) {
                throw new IOException("Failed to encrypt username", e);
            }

            // Start listening for incoming messages
            startMessageListener();

        } catch (IOException e) {
            isConnected.set(false);
            throw e;
        }
    }

    private void startMessageListener() {
        listenerThread = new Thread(() -> {
            String line;
            try {
                while (isConnected.get() && (line = in.readLine()) != null) {
                    try {
                        String decrypted = AESUtil.decrypt(line, key);
                        messageHandler.accept(decrypted);
                    } catch (Exception e) {
                        messageHandler.accept("[Decryption failed]");
                    }
                }
            } catch (SocketException e) {
                if (isConnected.get()) {
                    messageHandler.accept("[Connection lost - attempting to reconnect...]");
                    attemptReconnection();
                }
            } catch (IOException e) {
                if (isConnected.get()) {
                    messageHandler.accept("[Connection error: " + e.getMessage() + "]");
                    attemptReconnection();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void attemptReconnection() {
        isConnected.set(false);

        if (!shouldReconnect.get()) {
            return;
        }

        new Thread(() -> {
            int attempts = 0;
            int maxAttempts = 5;
            int baseDelay = 2000; // 2 seconds

            while (attempts < maxAttempts && shouldReconnect.get()) {
                attempts++;
                try {
                    messageHandler.accept("[Reconnection attempt " + attempts + "/" + maxAttempts + "...]");
                    Thread.sleep(baseDelay * attempts); // Exponential backoff

                    // Close old connection
                    closeConnection();

                    // Attempt new connection
                    connect();
                    messageHandler.accept("[Reconnected successfully!]");
                    return;

                } catch (Exception e) {
                    messageHandler.accept("[Reconnection attempt " + attempts + " failed: " + e.getMessage() + "]");
                }
            }

            messageHandler.accept("[Failed to reconnect after " + maxAttempts + " attempts]");
            isConnected.set(false);
        }).start();
    }

    public void sendMessage(String message) {
        if (!isConnected.get()) {
            messageHandler.accept("[Cannot send message - not connected]");
            return;
        }

        try {
            String encrypted = AESUtil.encrypt(message, key);
            out.println(encrypted);

            // Check if the message was sent successfully
            if (out.checkError()) {
                messageHandler.accept("[Message send failed - connection error]");
                attemptReconnection();
            }
        } catch (Exception e) {
            messageHandler.accept("[Failed to send message: " + e.getMessage() + "]");
        }
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
    }

    public void close() throws IOException {
        shouldReconnect.set(false);
        isConnected.set(false);

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        closeConnection();
    }

    public boolean isConnected() {
        return isConnected.get();
    }
}