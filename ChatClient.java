import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) { new ChatClient().start(); }

    public void start() {
        try (Socket socket = new Socket()) {
            System.out.println("Conectando a " + SERVER_IP + ":" + PORT + " ...");
            socket.connect(new InetSocketAddress(SERVER_IP, PORT), 5000);
            System.out.println("Conectado. Use /quit para sair.");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // leitor de mensagens do servidor
            Thread reader = new Thread(() -> {
                try {
                    String s;
                    while ((s = in.readLine()) != null) System.out.println(s);
                } catch (IOException e) {
                    System.err.println("Conexao perdida: " + e.getMessage());
                }
            });
            reader.setDaemon(true);
            reader.start();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String prompt = in.readLine(); if (prompt != null) System.out.println(prompt);
            String nickname = console.readLine(); if (nickname == null || nickname.trim().isEmpty()) nickname = "Anon";
            out.println(nickname);

            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
                if (userInput.equalsIgnoreCase("/quit") || userInput.equalsIgnoreCase("/exit")) break;
            }
            System.out.println("Desconectado.");
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
