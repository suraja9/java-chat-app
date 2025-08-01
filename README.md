# 🔐 Java Encrypted Chat App

> A secure real-time chat application using Java, JavaFX, and AES encryption with a client-server architecture.

---

## 🚀 Features

- **End-to-End AES Encryption**: All messages exchanged between client and server are encrypted using symmetric AES encryption.
- **Client-Server Architecture**: Centralized server managing multiple client connections concurrently.
- **Multi-Client Support**: Handles multiple clients with thread-based client handling on the server.
- **JavaFX UI**: Clean and minimal JavaFX GUI for the client chat window.
- **Console-Based Server**: Lightweight and easy-to-run server interface.
- **Thread-Safe Communication**: Each client connection runs on a separate thread for simultaneous message handling.
- **Simple Protocol**: Built without third-party networking libraries – uses `java.net.Socket` and `ServerSocket`.

---

## 📁 Project Structure

```
Java Encrypted Chat App/
├── client/
│   ├── ChatClient.java        # JavaFX GUI client app
│   └── ClientConnection.java  # Handles encrypted client-side communication
├── server/
│   ├── ChatServer.java        # Main server logic
│   └── ClientHandler.java     # Handles individual client sessions on the server
├── Main.java                  # Entry point (launches GUI)
```

---

## ⚙️ How It Works

1. The **server** starts and listens on a specified port.
2. Each **client** connects using the GUI, and messages are sent over encrypted sockets.
3. The **AES encryption key** is hardcoded/shared between client and server (can be improved with key exchange).
4. Messages are decrypted on the receiving side and displayed in the chat interface.

---

## 🛠 Tech Stack

- Java 8+
- JavaFX (GUI)
- AES Encryption (`javax.crypto`)
- Sockets (`java.net.Socket`, `ServerSocket`)
- Threads (`java.lang.Thread`)

---

## 🚀 Getting Started

### 🖥️ Run the Server

```
# Compile
javac server/*.java

# Run
java server.ChatServer
```

### 💬 Run the Client

```
# Compile
javac client/*.java Main.java

# Run
java Main
```

> Make sure to run the server before starting any client instance.

---

## 🔒 Security Note

- Currently uses **static AES key**. For production-grade encryption, consider using:
  - Secure key exchange (e.g., Diffie-Hellman)
  - Public-key cryptography (e.g., RSA)
  - TLS/SSL sockets

---

## 📦 Improvements You Can Make

- Dynamic key exchange
- Message timestamps and read receipts
- User authentication/login system
- Group chat or room-based communication
- File transfer support
- Hosting the server on a remote VPS
