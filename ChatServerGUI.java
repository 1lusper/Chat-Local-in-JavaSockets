import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServerGUI extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    private final JTextArea logArea = new JTextArea();
    private final DefaultListModel<String> clientsModel = new DefaultListModel<>();
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");

    private ServerCore server;

    public ChatServerGUI() {
        super("Chat Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 420);
        setLocationByPlatform(true);

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);

        JList<String> clientsList = new JList<>(clientsModel);
        JScrollPane clientsScroll = new JScrollPane(clientsList);
        clientsScroll.setPreferredSize(new Dimension(180, 0));
        clientsList.setBorder(BorderFactory.createTitledBorder("Conectados"));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("IP: " + SERVER_IP + "   Porta: " + PORT));
        top.add(btnStart);
        top.add(btnStop);
        btnStop.setEnabled(false);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScroll, clientsScroll);
        split.setResizeWeight(0.75);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
    }

    private void startServer() {
        if (server != null && server.isRunning()) {
            appendLog("Servidor já está rodando.");
            return;
        }
        server = new ServerCore(this, SERVER_IP, PORT);
        server.start();
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    void addClient(String name) {
        SwingUtilities.invokeLater(() -> clientsModel.addElement(name));
    }

    void removeClient(String name) {
        SwingUtilities.invokeLater(() -> clientsModel.removeElement(name));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServerGUI().setVisible(true));
    }

    // ================= Núcleo do Servidor =================
    private static class ServerCore {
        private final ChatServerGUI ui;
        private final String ip;
        private final int port;

        private volatile boolean running = false;
        private ServerSocket serverSocket;
        private final List<ClientHandler> handlers = new CopyOnWriteArrayList<>();

        ServerCore(ChatServerGUI ui, String ip, int port) {
            this.ui = ui;
            this.ip = ip;
            this.port = port;
        }

        boolean isRunning() { return running; }

        void start() {
            Thread t = new Thread(this::runLoop, "ServerCore");
            t.setDaemon(true);
            t.start();
        }

        void stop() {
            running = false;
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
            for (ClientHandler h : handlers) h.close();
            handlers.clear();
            ui.appendLog("Servidor parado.");
        }

        private void runLoop() {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(InetAddress.getByName(ip), port));
                this.serverSocket = ss;
                running = true;
                ui.appendLog("Servidor ouvindo em " + ip + ":" + port + " ...");
                while (running) {
                    Socket s = ss.accept();
                    ClientHandler h = new ClientHandler(s, this, ui);
                    handlers.add(h);
                    new Thread(h, "ClientHandler-" + s.getPort()).start();
                }
            } catch (IOException e) {
                if (running) ui.appendLog("Erro no servidor: " + e.getMessage());
            } finally {
                running = false;
            }
        }

        void broadcast(String msg, ClientHandler from) {
            ui.appendLog("Broadcast: " + msg);
            for (ClientHandler h : handlers) if (h != from) h.send(msg);
        }

        void remove(ClientHandler h) {
            handlers.remove(h);
        }
    }

    // ================= Handler de Cliente =================
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ServerCore server;
        private final ChatServerGUI ui;

        private PrintWriter out;
        private String name = "Anon";

        ClientHandler(Socket socket, ServerCore server, ChatServerGUI ui) {
            this.socket = socket;
            this.server = server;
            this.ui = ui;
        }

        @Override
        public void run() {
            ui.appendLog("Conexao de " + socket.getRemoteSocketAddress());
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter pw = new PrintWriter(socket.getOutputStream(), true)) {

                this.out = pw;
                pw.println("Bem-vindo! Envie seu nickname:");
                String nick = in.readLine();
                if (nick != null && !nick.trim().isEmpty()) name = nick.trim();

                server.broadcast(">>> " + name + " entrou no chat.", this);
                ui.addClient(name);

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) break;
                    server.broadcast(name + ": " + line, this);
                }

            } catch (IOException e) {
                ui.appendLog("Erro com " + name + ": " + e.getMessage());
            } finally {
                close();
                server.remove(this);
                server.broadcast("<<< " + name + " saiu do chat.", this);
                ui.removeClient(name);
                ui.appendLog("Conexao encerrada de " + socket.getRemoteSocketAddress());
            }
        }

        void send(String msg) {
            if (out != null) {
                out.println(msg);
                out.flush();
            }
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
