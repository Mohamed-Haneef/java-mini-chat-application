import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 8444;
    private static final Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static ArrayList<String> ActiveUsers = new ArrayList<>();
    private static ArrayList<String> PendingUsers = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Chat Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                PendingUsers.add(clientSocket.getInetAddress().getHostAddress());
                System.out.println("Pending Users: " + PendingUsers);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler clientHandler : clientHandlers) {
            // if (clientHandler != sender) {
            clientHandler.sendMessage(message);
            // }
        }
    }

    public static void updateUserList() {
        String usersList = String.join(", ", ActiveUsers);
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage("Active Users: " + usersList);
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Have to enter name
                // out.println("Enter your name:");
                clientName = in.readLine();
                if (clientName == null || clientName.isEmpty()) {
                    clientName = "Anonymous";
                }

                ActiveUsers.add(clientName);
                System.out.println(clientName + " joined the chat.");
                broadcastMessage(clientName + " has joined the chat!", this);
                updateUserList();

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(clientName + ": " + message);
                    broadcastMessage(clientName + ": " + message, this);
                }
            } catch (IOException e) {
                System.out.println("Error handling client " + clientName + ": " + e.getMessage());
            } finally {
                try {
                    clientHandlers.remove(this);
                    ActiveUsers.remove(clientName);
                    updateUserList();
                    socket.close();
                    broadcastMessage(clientName + " has left the chat.", this);
                    System.out.println(clientName + " disconnected.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
