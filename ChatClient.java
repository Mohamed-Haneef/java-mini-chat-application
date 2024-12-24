package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient extends Application {
    private PrintWriter out;
    private BufferedReader in;
    private TextArea messageArea;
    private TextField inputField;
    private ListView<String> groupListView;
    private Socket serverConnection;
    private String userName;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Client");

        userName = promptForName();

        messageArea = new TextArea();
        messageArea.setEditable(false);
        inputField = new TextField();
        Button sendButton = new Button("Send");

        groupListView = new ListView<>();
        groupListView.setPrefWidth(200);

        HBox textbox = new HBox(inputField, sendButton);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        VBox messageBox = new VBox(10, messageArea, textbox);
        VBox.setVgrow(messageArea, Priority.ALWAYS);
        VBox userListBox = new VBox(10, new Label("Users"), groupListView);

        HBox hbox = new HBox(10, messageBox, userListBox);
        HBox.setHgrow(messageBox, Priority.ALWAYS);

        Scene scene = new Scene(hbox, 1200, 680);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        connectToServer();

        Thread readMessagesThread = new Thread(this::readMessages);
        readMessagesThread.setDaemon(true);
        readMessagesThread.start();
    }

    private String promptForName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enter Your Name");
        dialog.setHeaderText("Welcome to the chat!");
        dialog.setContentText("Please enter your name:");

        Optional<String> result = dialog.showAndWait();
        return result.orElse("User_" + UUID.randomUUID().toString().substring(0, 5));
    }

    private void connectToServer() {
        try {
            serverConnection = new Socket("127.0.0.1", 8444);
            in = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
            out = new PrintWriter(serverConnection.getOutputStream(), true);
            System.out.println("Connected to the server");
            out.println(userName);

        } catch (IOException e) {
            showError("Error connecting to the server: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.clear();
        }
    }

    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                if (finalMessage.startsWith("Active Users: ")) {
                    String[] users = finalMessage.substring("Active Users: ".length()).split(", ");
                    Platform.runLater(() -> {
                        groupListView.getItems().setAll(users);
                    });
                } else {
                    Platform.runLater(() -> messageArea.appendText(finalMessage + "\n"));
                }
            }
        } catch (IOException e) {
            showError("Error reading from server: " + e.getMessage());
        }
    }

    private void showError(String errorMessage) {
        Platform.runLater(() -> messageArea.appendText("ERROR: " + errorMessage + "\n"));
    }
}
