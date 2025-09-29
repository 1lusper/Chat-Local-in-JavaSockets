import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    // Servidor local (mesmo PC)
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    // Lista segura de escritores de todos os clientes
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) { new ChatServer().start(); }

    public void start() {
        System.out.println("Servidor em " + SERVER_IP + ":" + PORT);
        try (ServerSocket ss = new ServerSocket(PORT, 50, InetAddress.getByName(SERVER_IP))) {
            System.out.println("Aguardando conexoes...");
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handleClient(s)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        System.out.println("Conexao: " + socket.getRemoteSocketAddress());
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            clients.add(out);
            out.println("Bem-vindo! Envie seu nickname:");
            String name = in.readLine();
            if (name == null || name.trim().isEmpty()) name = "Anon";

            broadcast(">>> " + name + " entrou no chat.", out);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) break;
                broadcast(name + ": " + line, out);
            }

            broadcast("<<< " + name + " saiu do chat.", out);
        } catch (IOException e) {
            System.err.println("Erro com cliente: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void broadcast(String msg, PrintWriter from) {
        System.out.println("Broadcast: " + msg);
        for (PrintWriter pw : clients) if (pw != from) pw.println(msg);
    }
}
