package com.aemtechnology.gestionprojet.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONObject;

public class VerificationPendingView extends JFrame {

    private final String uid;
    private final String fullName;
    private JLabel statusLabel;
    private Timer timer;

    public VerificationPendingView(String uid, String fullName) {
        this.uid = uid;
        this.fullName = fullName != null ? fullName : "User";
        setTitle("Verification Pending");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setupUI();
        startVerificationCheck();
    }

    private void setupUI() {
        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(30, 50, 100),
                        getWidth(), getHeight(), new Color(80, 150, 200)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setOpaque(false);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Icon
        JLabel iconLabel = new JLabel("⏳");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(iconLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Status message
        statusLabel = new JLabel("En attente de vérification d'email...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Instruction message
        JLabel instructionLabel = new JLabel("<html><center>Veuillez vérifier votre boîte mail et cliquer sur le lien de vérification.<br>Si vous ne trouvez pas l'email, pensez à regarder dans vos spams.</center></html>");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructionLabel.setForeground(Color.WHITE);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(instructionLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Progress bar
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setForeground(new Color(70, 130, 180));
        progressBar.setBackground(new Color(255, 255, 255, 50));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setMaximumSize(new Dimension(200, 20));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(progressBar);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void startVerificationCheck() {
        timer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Vérifier le statut de vérification via le backend
                    HttpClient client = HttpClient.newHttpClient();
                    String checkUrl = "https://teamworkatmini-jira.onrender.com/api/check-verification-status?uid=" + uid;
                    HttpRequest checkRequest = HttpRequest.newBuilder()
                            .uri(URI.create(checkUrl))
                            .header("Content-Type", "application/json")
                            .GET()
                            .build();
                    HttpResponse<String> checkResponse = client.send(checkRequest, HttpResponse.BodyHandlers.ofString());

                    if (checkResponse.statusCode() != 200) {
                        statusLabel.setText("Erreur lors de la vérification...");
                        statusLabel.setForeground(new Color(255, 100, 100));
                        return;
                    }

                    JSONObject checkJson = new JSONObject(checkResponse.body());
                    String status = checkJson.getString("status");

                    if ("verified".equals(status)) {
                        // L'email est vérifié, arrêter le timer et rediriger
                        timer.stop();
                        statusLabel.setText("Vérification réussie !");
                        statusLabel.setForeground(new Color(100, 200, 100));

                        // Rediriger vers HomeScreen après un court délai
                        Timer redirectTimer = new Timer(2000, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                dispose();
                                HomeScreen homeScreen = new HomeScreen(uid, fullName, null);
                                homeScreen.setVisible(true);
                            }
                        });
                        redirectTimer.setRepeats(false);
                        redirectTimer.start();
                    } else {
                        // L'email n'est pas encore vérifié, continuer à vérifier
                        statusLabel.setText("En attente de vérification d'email...");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusLabel.setText("Erreur de connexion au serveur...");
                    statusLabel.setForeground(new Color(255, 100, 100));
                }
            }
        });
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VerificationPendingView view = new VerificationPendingView("JohnDoe", "Ayola Moise");
            view.setVisible(true);
        });
    }
}