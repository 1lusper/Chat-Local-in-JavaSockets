import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    private JTextArea chatArea;
    private JTextField inputField;
    private PrintWriter out;

    public ChatClientGUI() {
        setTitle("Chat Cliente");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addActionListener(e -> send());
        add(inputField, BorderLayout.SOUTH);

        connect();
    }

    private void connect() {
        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) chatArea.append(line + "\n");
                } catch (IOException e) {
                    chatArea.append("Conexao perdida: " + e.getMessage() + "\n");
                }
            }).start();

            String prompt = in.readLine();
            String nick = JOptionPane.showInputDialog(this, prompt, "Nickname", JOptionPane.QUESTION_MESSAGE);
            if (nick == null || nick.trim().isEmpty()) nick = "Anon";
            out.println(nick);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void send() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            out.println(msg);
            if (msg.equalsIgnoreCase("/quit") || msg.equalsIgnoreCase("/exit")) System.exit(0);
        }
        inputField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true));
    }
}
