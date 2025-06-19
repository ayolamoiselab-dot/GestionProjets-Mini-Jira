package com.aemtechnology.gestionprojet.forms;

import com.aemtechnology.gestionprojet.view.HomeScreen;
import org.json.JSONArray;
import org.json.JSONObject;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AddCollaboratorForm extends JDialog {

    private final HomeScreen homeScreen;
    private final String projectId;
    private final String projectName;
    private JTextField searchField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    // Map pour associer chaque élément affiché à son UID
    private Map<String, String> displayToUidMap;

    public AddCollaboratorForm(HomeScreen homeScreen, JSONObject project) {
        super(homeScreen, "Ajouter un Collaborateur", true);
        this.homeScreen = homeScreen;
        this.projectId = project.getString("projectId");
        this.projectName = project.getString("projectName");
        this.displayToUidMap = new HashMap<>(); // Initialisation de la map
        initializeUI();
    }

    private void initializeUI() {
        setSize(400, 300);
        setLocationRelativeTo(homeScreen);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(50, 50, 60));

        // Panneau de recherche
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel searchLabel = new JLabel("Rechercher un utilisateur :");
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel, BorderLayout.NORTH);

        searchField = new JTextField();
        searchField.setBackground(new Color(60, 60, 70));
        searchField.setForeground(Color.WHITE);
        searchField.setBorder(new ModernBorder());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                searchUsers(searchField.getText());
            }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Liste des utilisateurs
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(new Color(60, 60, 70));
        userList.setForeground(Color.WHITE);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(new ModernBorder());

        // Bouton d'invitation
        JButton inviteButton = new JButton("Inviter");
        inviteButton.setBackground(new Color(70, 130, 180));
        inviteButton.setForeground(Color.WHITE);
        inviteButton.addActionListener(e -> sendInvitation());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(inviteButton);

        add(searchPanel, BorderLayout.NORTH);
        add(userScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void searchUsers(String query) {
        try {
            System.out.println("Recherche de l'utilisateur avec la requête : " + query);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/users/search?query=" + query))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Code de réponse HTTP : " + response.statusCode());
            System.out.println("Réponse du serveur : " + response.body());

            userListModel.clear();
            displayToUidMap.clear(); // Réinitialiser la map

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray users = jsonResponse.getJSONArray("users");
                System.out.println("Nombre d'utilisateurs trouvés : " + users.length());
                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    String display = user.getString("fullName") + " (" + user.getString("email") + ")";
                    String uid = user.getString("uid"); // Récupérer le UID
                    userListModel.addElement(display);
                    displayToUidMap.put(display, uid); // Associer l'affichage au UID
                }
            } else {
                System.out.println("Erreur HTTP : " + response.statusCode());
                Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur HTTP : " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur lors de la recherche d'utilisateurs : " + e.getMessage());
        }
    }

    private void sendInvitation() {
        int selectedIndex = userList.getSelectedIndex();
        System.out.println("sendInvitation() - Index sélectionné : " + selectedIndex);
        if (selectedIndex == -1) {
            System.out.println("Aucun utilisateur sélectionné.");
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "Veuillez sélectionner un utilisateur.");
            return;
        }

        String selectedUser = userListModel.getElementAt(selectedIndex);
        System.out.println("Utilisateur sélectionné : " + selectedUser);
        String receiverUid = displayToUidMap.get(selectedUser); // Récupérer le UID directement
        if (receiverUid == null) {
            System.out.println("UID non trouvé pour l'utilisateur sélectionné.");
            Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur : UID de l'utilisateur non trouvé.");
            return;
        }
        System.out.println("UID récupéré : " + receiverUid);

        // Désactiver le bouton pendant l'envoi pour éviter plusieurs clics
        JButton inviteButton = (JButton) ((JPanel) this.getContentPane().getComponent(2)).getComponent(0);
        inviteButton.setEnabled(false);

        // Exécuter la requête dans un thread séparé
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                JSONObject invitationData = new JSONObject();
                invitationData.put("senderUid", homeScreen.getUid());
                invitationData.put("receiverUid", receiverUid);
                invitationData.put("projectId", projectId);
                System.out.println("Données de l'invitation : " + invitationData.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/invitations"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(invitationData.toString(), StandardCharsets.UTF_8))
                        .build();
                System.out.println("Requête POST envoyée à /api/invitations");

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Réponse reçue - Code HTTP : " + response.statusCode() + ", Corps : " + response.body());

                // Mettre à jour l'interface graphique dans l'EDT
                SwingUtilities.invokeLater(() -> {
                    if (response.statusCode() == 200) {
                        System.out.println("Invitation envoyée avec succès.");
                        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "Invitation envoyée avec succès !");
                        dispose();
                    } else {
                        System.out.println("Erreur lors de l'envoi de l'invitation - Code HTTP : " + response.statusCode());
                        Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur lors de l'envoi de l'invitation.");
                    }
                    inviteButton.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    System.err.println("Erreur dans sendInvitation() : " + e.getMessage());
                    Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur serveur: " + e.getMessage());
                    inviteButton.setEnabled(true);
                });
            }
        }).start();
    }

    class ModernBorder extends javax.swing.border.AbstractBorder {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(100, 100, 120));
            g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 15, 10, 15);
        }
    }
}