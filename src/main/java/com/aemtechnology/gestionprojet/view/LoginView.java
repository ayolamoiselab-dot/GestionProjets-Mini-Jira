package com.aemtechnology.gestionprojet.view;

import com.aemtechnology.gestionprojet.menu.FormManager;
import fi.iki.elonen.NanoHTTPD;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

public class LoginView extends JPanel {

    private boolean darkMode = false;
    private boolean isSignUpMode = false;
    private JTextField emailField, nameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton continueButton;
    private JLabel toggleModeLabel;
    private JPanel formPanel;
    private JPanel noAccountPanel;
    private JLabel welcomeLabel;
    private JLabel subtitleLabel;
    private JButton googleButton, facebookButton, githubButton;
    private Window parentWindow; // Nouvelle variable pour stocker la fenêtre parente

    private static final Dotenv dotenv = Dotenv.load();
    private static final String GOOGLE_CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");
    private static final String GITHUB_CLIENT_ID = dotenv.get("GITHUB_CLIENT_ID");

    public LoginView() {
        this(true); // Par défaut, mode clair
    }

    public LoginView(boolean darkMode) {
        this.darkMode = darkMode;

        // Appliquer le LookAndFeel en fonction de darkMode
        if (darkMode) {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.themes.FlatMacDarkLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            FlatLightLaf.setup();
        }

        // Capturer la fenêtre parente dès que possible
        SwingUtilities.invokeLater(() -> {
            parentWindow = SwingUtilities.getWindowAncestor(this);
            if (parentWindow == null) {
                System.err.println("Avertissement : Fenêtre parente non trouvée lors de l'initialisation de LoginView.");
            } else {
                System.out.println("Fenêtre parente capturée lors de l'initialisation : " + parentWindow.getClass().getSimpleName());
            }
        });

        setLayout(new MigLayout("fill,insets 20", "[center]", "[center]"));

        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 35 45 30 45", "fill,250:280"));
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;"
                + "[light]background:darken(@background,3%);"
                + "[dark]background:lighten(@background,3%)");

        welcomeLabel = new JLabel("Ravie de vous Revoir!");
        welcomeLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +10");
        panel.add(welcomeLabel);

        subtitleLabel = new JLabel("Veuillez vous connecter à votre compte");
        subtitleLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]foreground:lighten(@foreground,30%);"
                + "[dark]foreground:darken(@foreground,30%)");
        panel.add(subtitleLabel);

        formPanel = new JPanel(new MigLayout("wrap, fillx, insets 0", "[fill]", ""));
        formPanel.setOpaque(false);
        panel.add(formPanel, "gapy 8, growx");

        updateForm();

        JPanel socialPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]", ""));
        socialPanel.setOpaque(false);
        googleButton = createSocialButton("/images/google.png", "google");
        githubButton = createSocialButton("/images/github.png", "github");
        socialPanel.add(googleButton);
        socialPanel.add(githubButton);
        panel.add(socialPanel, "gapy 10");

        noAccountPanel = new JPanel(new MigLayout("insets 0"));
        noAccountPanel.setOpaque(false);
        updateNoAccountText();
        panel.add(noAccountPanel, "gapy 10");

        add(panel);

        JPanel topBar = new JPanel(new MigLayout("insets 10", "push[]", ""));
        topBar.setOpaque(false);
        toggleModeLabel = new JLabel(darkMode ? "\u2600" : "\u263D");
        toggleModeLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        toggleModeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleModeLabel.setForeground(Color.BLACK);
        toggleModeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleDarkMode();
            }
        });
        topBar.add(toggleModeLabel);
        add(topBar, "dock north");
    }

    private void updateForm() {
        formPanel.removeAll();
        if (isSignUpMode) {
            JLabel nameLabel = new JLabel("Nom & Prenoms");
            formPanel.add(nameLabel, "gapy 8");
            nameField = new JTextField();
            nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Entrez votre nom & prenoms");
            formPanel.add(nameField);

            JLabel emailLabel = new JLabel("Email");
            formPanel.add(emailLabel, "gapy 8");
            emailField = new JTextField();
            emailField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Entrez votre email");
            formPanel.add(emailField);

            JLabel passwordLabel = new JLabel("Mot de passe");
            formPanel.add(passwordLabel, "gapy 8");
            passwordField = new JPasswordField();
            passwordField.putClientProperty(FlatClientProperties.STYLE, ""
                    + "showRevealButton:true");
            passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Entrez votre mot de passe");
            formPanel.add(passwordField);

            JLabel confirmPasswordLabel = new JLabel("Confirmer le mot de passe");
            formPanel.add(confirmPasswordLabel, "gapy 8");
            confirmPasswordField = new JPasswordField();
            confirmPasswordField.putClientProperty(FlatClientProperties.STYLE, ""
                    + "showRevealButton:true");
            confirmPasswordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Confirmez votre mot de passe");
            formPanel.add(confirmPasswordField);
        } else {
            JLabel emailLabel = new JLabel("Email");
            formPanel.add(emailLabel, "gapy 8");
            emailField = new JTextField();
            emailField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Entrez votre email");
            formPanel.add(emailField);

            JLabel passwordLabel = new JLabel("Mot de passe");
            formPanel.add(passwordLabel, "gapy 8");
            passwordField = new JPasswordField();
            passwordField.putClientProperty(FlatClientProperties.STYLE, ""
                    + "showRevealButton:true");
            passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Entrez votre mot de passe");
            formPanel.add(passwordField);
        }

        continueButton = new JButton(isSignUpMode ? "Sign Up" : "Continuer");
        continueButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:darken(@background,10%);"
                + "[dark]background:lighten(@background,10%);"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");
        continueButton.addActionListener(e -> {
            if (isSignUpMode) {
                if (!new String(passwordField.getPassword()).equals(new String(confirmPasswordField.getPassword()))) {
                    JOptionPane.showMessageDialog(this, "Passwords do not match!");
                    return;
                }

                String fullName = nameField.getText();
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());

                JSONObject json = new JSONObject();
                json.put("fullName", fullName);
                json.put("email", email);
                json.put("password", password);

                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/signup"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String uid = extractUidFromResponse(response.body());
                        if (uid != null) {
                            SwingUtilities.getWindowAncestor(this).dispose();
                            VerificationPendingView pendingView = new VerificationPendingView(uid, fullName);
                            pendingView.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(this, "Error: Could not retrieve user ID.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Error during signup: " + response.body());
                    }
                } catch (Exception signupEx) {
                    JOptionPane.showMessageDialog(this, "Error connecting to the server: " + signupEx.getMessage());
                }
            } else {
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());

                if (email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.");
                    return;
                }

                try {
                    HttpClient client = HttpClient.newHttpClient();
                    String signInUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDib42OIcXpJDePgJea920plc2hrKX0L1Y";
                    JSONObject signInPayload = new JSONObject();
                    signInPayload.put("email", email);
                    signInPayload.put("password", password);
                    signInPayload.put("returnSecureToken", true);

                    HttpRequest signInRequest = HttpRequest.newBuilder()
                            .uri(URI.create(signInUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(signInPayload.toString(), StandardCharsets.UTF_8))
                            .build();
                    HttpResponse<String> signInResponse = client.send(signInRequest, HttpResponse.BodyHandlers.ofString());

                    if (signInResponse.statusCode() != 200) {
                        JSONObject errorJson = new JSONObject(signInResponse.body());
                        String errorMessage = errorJson.optString("error.message", "Erreur inconnue");
                        JOptionPane.showMessageDialog(this, "Échec de la connexion : " + errorMessage);
                        return;
                    }

                    JSONObject signInJson = new JSONObject(signInResponse.body());
                    String uid = signInJson.getString("localId");
                    String idToken = signInJson.getString("idToken");

                    String checkUrl = "https://teamworkatmini-jira.onrender.com/api/check-verification-status?uid=" + uid;
                    HttpRequest checkRequest = HttpRequest.newBuilder()
                            .uri(URI.create(checkUrl))
                            .header("Content-Type", "application/json")
                            .GET()
                            .build();
                    HttpResponse<String> checkResponse = client.send(checkRequest, HttpResponse.BodyHandlers.ofString());

                    if (checkResponse.statusCode() != 200) {
                        JSONObject errorJson = new JSONObject(checkResponse.body());
                        String errorMessage = errorJson.optString("error", "Erreur inconnue");
                        JOptionPane.showMessageDialog(this, "Erreur lors de la vérification du statut : " + errorMessage);
                        return;
                    }

                    JSONObject checkJson = new JSONObject(checkResponse.body());
                    String status = checkJson.getString("status");

                    String fullName;
                    String userUrl = "https://teamworkatmini-jira.onrender.com/user/" + uid;
                    HttpRequest userRequest = HttpRequest.newBuilder()
                            .uri(URI.create(userUrl))
                            .header("Content-Type", "application/json")
                            .GET()
                            .build();
                    HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());

                    if (userResponse.statusCode() == 200) {
                        System.out.println("Données utilisateur récupérées : " + userResponse.body());
                        try {
                            JSONObject userJson = new JSONObject(userResponse.body());
                            fullName = userJson.getString("fullName");
                            System.out.println("Nom complet récupéré : " + fullName);
                        } catch (Exception jsonEx) {
                            System.out.println("Erreur lors de la lecture du nom complet : " + jsonEx.getMessage());
                            fullName = "Utilisateur";
                        }
                    } else {
                        System.out.println("Échec de la récupération des données utilisateur. Statut : " + userResponse.statusCode() + ", Réponse : " + userResponse.body());
                        try {
                            String userInfoUrl = "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=AIzaSyDib42OIcXpJDePgJea920plc2hrKX0L1Y";
                            JSONObject lookupPayload = new JSONObject();
                            lookupPayload.put("idToken", idToken);
                            HttpRequest lookupRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(userInfoUrl))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(lookupPayload.toString(), StandardCharsets.UTF_8))
                                    .build();
                            HttpResponse<String> lookupResponse = client.send(lookupRequest, HttpResponse.BodyHandlers.ofString());
                            if (lookupResponse.statusCode() == 200) {
                                JSONObject lookupJson = new JSONObject(lookupResponse.body());
                                fullName = lookupJson.getJSONArray("users").getJSONObject(0).optString("displayName", "Utilisateur");
                                System.out.println("Nom complet récupéré depuis Firebase : " + fullName);
                            } else {
                                // System.out.println("É \#chec de la récupération des données depuis Firebase. Statut : " + lookupResponse.statusCode() + ", Réponse : " + lookupResponse.body());
                                fullName = "Utilisateur";
                            }
                        } catch (Exception lookupEx) {
                            System.out.println("Erreur lors de la récupération du nom complet depuis Firebase : " + lookupEx.getMessage());
                            fullName = "Utilisateur";
                        }
                    }

                    final String finalStatus = status;
                    final String finalUid = uid;
                    final String finalFullName = fullName;
                    SwingUtilities.getWindowAncestor(this).dispose();
                    SwingUtilities.invokeLater(() -> {
                        if ("verified".equals(finalStatus)) {
                            HomeScreen homeScreen = new HomeScreen(finalUid, finalFullName, email);
                            homeScreen.setVisible(true);
                        } else {
                            VerificationPendingView pendingView = new VerificationPendingView(finalUid, finalFullName);
                            pendingView.setVisible(true);
                        }
                    });
                } catch (Exception loginEx) {
                    JOptionPane.showMessageDialog(this, "Erreur de connexion au serveur : " + loginEx.getMessage());
                }
            }
        });
        formPanel.add(continueButton, "gapy 10");
        revalidate();
        repaint();
    }

    private void updateNoAccountText() {
        noAccountPanel.removeAll();
        if (isSignUpMode) {
            JLabel backLabel = new JLabel("Vous avez déjà un compte? ");
            backLabel.putClientProperty(FlatClientProperties.STYLE, ""
                    + "[light]foreground:lighten(@foreground,30%);"
                    + "[dark]foreground:darken(@foreground,30%)");
            JLabel signInLabel = new JLabel("Se connecter");
            signInLabel.putClientProperty(FlatClientProperties.STYLE, ""
                    + "font:bold;"
                    + "[light]foreground:#FF7643;"
                    + "[dark]foreground:#FF7643");
            signInLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            signInLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    isSignUpMode = false;
                    updateForm();
                    welcomeLabel.setText("Ravie de vous Revoir!");
                    subtitleLabel.setText("Veuillez vous connecter pour acceder à votre compte!");
                    updateNoAccountText();
                }
            });
            noAccountPanel.add(backLabel);
            noAccountPanel.add(signInLabel);
        } else {
            JLabel noAccountLabel = new JLabel("Pas encore de compte? ");
            noAccountLabel.putClientProperty(FlatClientProperties.STYLE, ""
                    + "[light]foreground:lighten(@foreground,30%);"
                    + "[dark]foreground:darken(@foreground,30%)");
            JLabel signUpLabel = new JLabel("Créer un compte");
            signUpLabel.putClientProperty(FlatClientProperties.STYLE, ""
                    + "font:bold;"
                    + "[light]foreground:#FF7643;"
                    + "[dark]foreground:#FF7643");
            signUpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            signUpLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    isSignUpMode = true;
                    updateForm();
                    welcomeLabel.setText("Créer un compte");
                    subtitleLabel.setText("Créer un compte avec un email et un mot de passe");
                    updateNoAccountText();
                }
            });
            noAccountPanel.add(noAccountLabel);
            noAccountPanel.add(signUpLabel);
        }
        noAccountPanel.revalidate();
        noAccountPanel.repaint();
    }

    private JButton createSocialButton(String imagePath, String provider) {
        JButton btn = new JButton();
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream(imagePath));
            if (img != null) {
                Image scaledImg = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(scaledImg));
            } else {
                btn.setText(provider.substring(0, 1).toUpperCase());
            }
        } catch (IOException e) {
            btn.setText("?");
        }

        btn.setPreferredSize(new Dimension(56, 56));
        btn.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:#F5F6F9;"
                + "[dark]background:#F5F6F9;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");
        btn.addActionListener(e -> handleSocialLoginWithBrowser(provider));
        return btn;
    }

    private void handleSocialLoginWithBrowser(String provider) {
        try {
            if (!"google".equals(provider) && !"github".equals(provider)) {
                JOptionPane.showMessageDialog(this, "Provider non supporté: " + provider);
                return;
            }

            String authUrl = buildAuthUrl(provider);
            System.out.println("Opening browser with URL: " + authUrl);
            Desktop.getDesktop().browse(new URI(authUrl));
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture du navigateur: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur: Impossible d'ouvrir le navigateur: " + e.getMessage());
        }
    }

    private String buildAuthUrl(String provider) {
        String clientId = provider.equals("google") ? GOOGLE_CLIENT_ID : GITHUB_CLIENT_ID;
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new RuntimeException("Client ID manquant pour " + provider + ". Vérifiez vos variables d'environnement.");
        }

        String redirectUri = URLEncoder.encode("https://teamworkatmini-jira.onrender.com/api/auth/callback", StandardCharsets.UTF_8);
        String state = provider;

        if ("google".equals(provider)) {
            return String.format(
                    "https://accounts.google.com/o/oauth2/v2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=email%%20profile&state=%s",
                    clientId, redirectUri, state
            );
        } else {
            return String.format(
                    "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=user:email&state=%s",
                    clientId, redirectUri, state
            );
        }
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        if (darkMode) {
            toggleModeLabel.setText("\u2600");
        } else {
            toggleModeLabel.setText("\u263D");
        }
        SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
        revalidate();
        repaint();
    }

    class RoundedBorder extends AbstractBorder {

        private int radius;
        private Color borderColor;

        public RoundedBorder(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape border = new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius);
            g2.setColor(borderColor);
            g2.draw(border);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 12, 8, 12);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = 12;
            insets.top = insets.bottom = 8;
            return insets;
        }
    }

    private String extractUidFromResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("uid")) {
                return json.getString("uid");
            }
        } catch (Exception e) {
            System.out.println("Error parsing UID from response: " + e.getMessage());
        }
        return null;
    }

    public void exchangeCodeForToken(String provider, String code) {
        // Utiliser la fenêtre parente capturée lors de l'initialisation
        Window windowToClose = parentWindow;
        if (windowToClose == null) {
            System.err.println("Erreur : Fenêtre parente non disponible dans parentWindow.");
            // Dernière tentative : essayer de récupérer la fenêtre actuelle
            windowToClose = SwingUtilities.getWindowAncestor(this);
            if (windowToClose == null) {
                System.err.println("Erreur : Fenêtre parente introuvable même après une nouvelle tentative.");
            } else {
                System.out.println("Fenêtre parente trouvée après une nouvelle tentative : " + windowToClose.getClass().getSimpleName());
            }
        } else {
            System.out.println("Fenêtre parente trouvée via parentWindow : " + windowToClose.getClass().getSimpleName());
        }

        // Créer une variable final pour la lambda
        final Window finalWindowToClose = windowToClose;

        try {
            HttpClient client = HttpClient.newHttpClient();
            JSONObject payload = new JSONObject();
            payload.put("provider", provider);
            payload.put("code", code);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/auth/" + provider))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String uid = json.getString("uid");
                String fullName = json.optString("fullName", "Utilisateur");

                // Étape : Fermer la fenêtre et afficher le LoadingDialog sur l'EDT
                SwingUtilities.invokeLater(() -> {
                    // Fermer la fenêtre immédiatement
                    if (finalWindowToClose != null) {
                        System.out.println("Fermeture de la fenêtre de LoginView...");
                        finalWindowToClose.dispose();
                        System.out.println("Fenêtre de LoginView fermée.");
                    } else {
                        System.err.println("La fenêtre est null, impossible de la fermer.");
                    }

                    // Afficher le LoadingDialog
                    LoadingDialog loadingDialog = new LoadingDialog(null);
                    loadingDialog.setLocationRelativeTo(null); // Centrer sur l'écran
                    loadingDialog.setVisible(true);
                    System.out.println("LoadingDialog affiché.");

                    // Planifier l'ouverture de HomeScreen après un court délai
                    Timer timer = new Timer(2000, e -> {
                        SwingUtilities.invokeLater(() -> {
                            // Fermer le LoadingDialog
                            loadingDialog.dispose();
                            System.out.println("LoadingDialog fermé.");

                            // Ouvrir HomeScreen
                            HomeScreen homeScreen = new HomeScreen(uid, fullName, null);
                            homeScreen.setVisible(true);
                            System.out.println("HomeScreen affiché.");
                        });
                    });
                    timer.setRepeats(false); // Ne se répète pas
                    timer.start();
                });
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la connexion avec " + provider + ": " + response.body());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion: " + e.getMessage());
            // Si une erreur survient, on essaie quand même de fermer la fenêtre
            if (finalWindowToClose != null) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Fermeture de la fenêtre après une erreur...");
                    finalWindowToClose.dispose();
                });
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.themes.FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sign In");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new LoginView());
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
