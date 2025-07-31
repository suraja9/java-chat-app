package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class ChatClient extends Application {

    private TextArea messageArea;
    private TextField inputField;
    private ListView<String> userList;
    private Label statusLabel;
    private ClientConnection connection;
    private String username;
    private Set<String> onlineUsers;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void start(Stage primaryStage) {
        onlineUsers = new HashSet<>();

        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #2b2b2b;");

        // Create top bar with status
        createTopBar(root);

        // Create center chat area
        createChatArea(root);

        // Create right sidebar with user list
        createUserListSidebar(root);

        // Create bottom input area
        createInputArea(root);

        Scene scene = new Scene(root, 800, 600);

        // Try to load CSS file if it exists
        try {
            if (getClass().getResource("/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            }
        } catch (Exception e) {
            System.out.println("CSS file not found, using default styling");
        }

        primaryStage.setTitle("🔒 Secure Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Prompt for username and password before connecting
        promptUsernameAndPassword();
    }

    private void createTopBar(BorderPane root) {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 10;");

        Label titleLabel = new Label("🔒 Secure Chat");
        titleLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("🔴 Disconnected");
        statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 12px;");

        topBar.getChildren().addAll(titleLabel, spacer, statusLabel);
        root.setTop(topBar);
    }

    private void createChatArea(BorderPane root) {
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setStyle(
                "-fx-control-inner-background: #1a1a1a; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-family: 'Consolas', 'Monaco', monospace; " +
                        "-fx-font-size: 13px; " +
                        "-fx-border-color: #444444; " +
                        "-fx-border-width: 1px;"
        );

        ScrollPane scrollPane = new ScrollPane(messageArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #1a1a1a; -fx-border-color: #444444;");

        root.setCenter(scrollPane);
    }

    private void createUserListSidebar(BorderPane root) {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(0, 0, 0, 10));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #1e1e1e;");

        Label userListLabel = new Label("👥 Online Users");
        userListLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 14px; -fx-font-weight: bold;");

        userList = new ListView<>();
        userList.setPrefHeight(300);
        userList.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-border-color: #444444; " +
                        "-fx-border-width: 1px;"
        );

        VBox.setVgrow(userList, Priority.ALWAYS);
        sidebar.getChildren().addAll(userListLabel, userList);
        root.setRight(sidebar);
    }

    private void createInputArea(BorderPane root) {
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10, 0, 0, 0));
        inputArea.setAlignment(Pos.CENTER);

        inputField = new TextField();
        inputField.setPromptText("Type your message...");
        inputField.setStyle(
                "-fx-background-color: #2a2a2a; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: #888888; " +
                        "-fx-border-color: #444444; " +
                        "-fx-border-width: 1px; " +
                        "-fx-font-size: 13px;"
        );
        inputField.setOnAction(e -> sendMessage());

        Button sendButton = new Button("Send");
        sendButton.setStyle(
                "-fx-background-color: #00ff88; " +
                        "-fx-text-fill: #000000; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );
        sendButton.setOnAction(e -> sendMessage());

        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputArea.getChildren().addAll(inputField, sendButton);
        root.setBottom(inputArea);
    }

    private void promptUsernameAndPassword() {
        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setTitle("Login");
        userDialog.setHeaderText("Enter your username");
        userDialog.setContentText("Username:");
        userDialog.getDialogPane().setStyle("-fx-background-color: #2b2b2b;");

        userDialog.showAndWait().ifPresent(enteredUsername -> {
            this.username = enteredUsername;

            TextInputDialog passDialog = new TextInputDialog();
            passDialog.setTitle("Authentication");
            passDialog.setHeaderText("Enter the shared password");
            passDialog.setContentText("Password:");
            passDialog.getDialogPane().setStyle("-fx-background-color: #2b2b2b;");

            passDialog.showAndWait().ifPresent(password -> {
                try {
                    connection = new ClientConnection("localhost", 1234, password, this.username, this::handleMessage);
                    statusLabel.setText("🟢 Connected as " + this.username);
                    statusLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 12px;");
                    appendMessage("[Connected as " + this.username + "]");
                    addUser(this.username);
                } catch (Exception e) {
                    statusLabel.setText("🔴 Connection Failed");
                    statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 12px;");
                    appendMessage("[Failed to connect: " + e.getMessage() + "]");
                }
            });
        });
    }

    private void handleMessage(String message) {
        Platform.runLater(() -> {
            appendMessage(message);
            updateUserList(message);
            autoScroll();
        });
    }

    private void updateUserList(String message) {
        if (message.contains(" has joined the chat")) {
            String user = message.replace("🟢 ", "").replace(" has joined the chat", "");
            addUser(user);
        } else if (message.contains(" has left the chat")) {
            String user = message.replace("🔴 ", "").replace(" has left the chat", "");
            removeUser(user);
        }
    }

    private void addUser(String user) {
        if (!onlineUsers.contains(user)) {
            onlineUsers.add(user);
            Platform.runLater(() -> {
                userList.getItems().clear();
                userList.getItems().addAll(onlineUsers);
                userList.getItems().sort(String::compareToIgnoreCase);
            });
        }
    }

    private void removeUser(String user) {
        if (onlineUsers.contains(user)) {
            onlineUsers.remove(user);
            Platform.runLater(() -> {
                userList.getItems().clear();
                userList.getItems().addAll(onlineUsers);
                userList.getItems().sort(String::compareToIgnoreCase);
            });
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && connection != null) {
            String timestamp = LocalTime.now().format(TIME_FORMAT);
            String fullMessage = "[" + timestamp + "] " + this.username + ": " + text;
            connection.sendMessage(fullMessage);
            inputField.clear();
        }
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            messageArea.appendText(message + "\n");
            autoScroll();
        });
    }

    private void autoScroll() {
        Platform.runLater(() -> {
            messageArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
