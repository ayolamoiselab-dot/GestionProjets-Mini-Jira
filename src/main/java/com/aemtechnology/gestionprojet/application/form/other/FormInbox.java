package com.aemtechnology.gestionprojet.application.form.other;

import com.aemtechnology.gestionprojet.application.form.MainForm;
import com.formdev.flatlaf.FlatClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import raven.popup.component.SimplePopupBorder;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FormInbox extends JPanel {

    private final String fullName;
    private final String email;
    private JTabbedPane tabbedPane;
    private JPanel notificationsPanel;
    private JPanel pendingInvitationsPanel;

    public FormInbox(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
        init();
    }

    private void init() {
        setOpaque(false);
        setLayout(new BorderLayout());

        // Initialisation des panneaux avec vérification
        notificationsPanel = new JPanel();
        notificationsPanel.setOpaque(false);
        notificationsPanel.setLayout(new BoxLayout(notificationsPanel, BoxLayout.Y_AXIS));

        pendingInvitationsPanel = new JPanel();
        pendingInvitationsPanel.setOpaque(false);
        pendingInvitationsPanel.setLayout(new BoxLayout(pendingInvitationsPanel, BoxLayout.Y_AXIS));

        // Création du JTabbedPane
        tabbedPane = new JTabbedPane();
        JScrollPane notificationsScroll = new JScrollPane(notificationsPanel);
        notificationsScroll.setOpaque(false);
        notificationsScroll.getViewport().setOpaque(false);
        notificationsScroll.setBorder(null);

        JScrollPane invitationsScroll = new JScrollPane(pendingInvitationsPanel);
        invitationsScroll.setOpaque(false);
        invitationsScroll.getViewport().setOpaque(false);
        invitationsScroll.setBorder(null);

        tabbedPane.addTab("Notifications", notificationsScroll);
        tabbedPane.addTab("Demandes en attente", invitationsScroll);
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void updateNotifications(JSONArray notifications) {
        if (notificationsPanel == null) {
            System.err.println("notificationsPanel est null, réinitialisation...");
            notificationsPanel = new JPanel();
            notificationsPanel.setOpaque(false);
            notificationsPanel.setLayout(new BoxLayout(notificationsPanel, BoxLayout.Y_AXIS));
            tabbedPane.setComponentAt(0, new JScrollPane(notificationsPanel));
        }
        notificationsPanel.removeAll();
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject notification = notifications.getJSONObject(i);
            String message = notification.getString("message");
            String type = notification.getString("type");
            String invitationId = notification.optString("invitationId", null);
            String notificationId = notification.getString("notificationId");
            String status = notification.optString("status", "unread");

            JPanel notificationCard = createStyledNotificationCard(message, type, invitationId, notificationId, status);
            notificationsPanel.add(notificationCard);
            notificationsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        notificationsPanel.revalidate();
        notificationsPanel.repaint();

        // Mettre à jour le badge dans MainForm
        int unreadCount = 0;
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject notification = notifications.getJSONObject(i);
            if ("unread".equals(notification.optString("status", "unread"))) {
                unreadCount++;
            }
        }
        MainForm mainForm = (MainForm) SwingUtilities.getAncestorOfClass(MainForm.class, this);
        if (mainForm != null) {
            mainForm.setNotificationBadge(unreadCount);
        }
    }

    private JPanel createStyledNotificationCard(String message, String type, String invitationId, String notificationId, String status) {
        JPanel card = new JPanel();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5)); // Boutons sur la même ligne
        card.setOpaque(true);
        card.setBackground(new Color(50, 50, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel messageLabel = new JLabel("" + message + "");
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        card.add(messageLabel);

        // Statut lu/non lu
        JLabel statusLabel = new JLabel(status.equals("unread") ? "Non lu" : "Lu");
        statusLabel.setForeground(status.equals("unread") ? Color.RED : Color.GREEN);
        card.add(statusLabel);

        // Boutons pour les invitations en attente
        if (type.equals("invitation") && invitationId != null && status.equals("unread")) {
            String invitationStatus = checkInvitationStatus(invitationId);
            if ("pending".equals(invitationStatus)) {
                JButton acceptButton = new JButton("Accepter");
                acceptButton.setBackground(new Color(70, 160, 70));
                acceptButton.setForeground(Color.WHITE);
                acceptButton.addActionListener(e -> respondToInvitation(invitationId, "accept", notificationId));

                JButton declineButton = new JButton("Refuser");
                declineButton.setBackground(new Color(200, 50, 50));
                declineButton.setForeground(Color.WHITE);
                declineButton.addActionListener(e -> respondToInvitation(invitationId, "decline", notificationId));

                card.add(acceptButton);
                card.add(declineButton);
            }
        }

        // Ajuster dynamiquement la taille au contenu
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        // Listener pour marquer comme lu
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showNotificationDetails(notificationId, message, type, invitationId);
            }
        });

        return card;
    }

    public void updatePendingInvitations(JSONArray invitations) {
        if (pendingInvitationsPanel == null) {
            System.err.println("pendingInvitationsPanel est null, réinitialisation...");
            pendingInvitationsPanel = new JPanel();
            pendingInvitationsPanel.setOpaque(false);
            pendingInvitationsPanel.setLayout(new BoxLayout(pendingInvitationsPanel, BoxLayout.Y_AXIS));
            tabbedPane.setComponentAt(1, new JScrollPane(pendingInvitationsPanel));
        }
        pendingInvitationsPanel.removeAll();
        for (int i = 0; i < invitations.length(); i++) {
            JSONObject invitation = invitations.getJSONObject(i);
            String projectName = invitation.getString("projectName");
            String receiverName = invitation.getString("receiverName");
            String status = invitation.optString("status", "pending");

            JPanel invitationCard = createInvitationCard(projectName, receiverName, status);
            pendingInvitationsPanel.add(invitationCard);
            pendingInvitationsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        pendingInvitationsPanel.revalidate();
        pendingInvitationsPanel.repaint();
    }

    private JPanel createInvitationCard(String projectName, String receiverName, String status) {
        JPanel card = new JPanel();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        card.setOpaque(true);
        card.setBackground(new Color(50, 50, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel invitationLabel = new JLabel("Invitation à " + receiverName + " pour le projet : " + projectName);
        invitationLabel.setForeground(Color.WHITE);
        invitationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        card.add(invitationLabel);

        JLabel statusLabel = new JLabel("Statut: " + status);
        statusLabel.setForeground(getStatusColor(status));
        card.add(statusLabel);

        // Ajuster dynamiquement la taille au contenu
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        return card;
    }

    private void showNotificationDetails(String notificationId, String message, String type, String invitationId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Détails de la Notification", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(40, 40, 40));
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(true);
        contentPanel.setBackground(new Color(40, 40, 40));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel detailLabel = new JLabel("<b>Message:</b> " + message + "");
        detailLabel.setForeground(Color.WHITE);
        contentPanel.add(detailLabel, BorderLayout.CENTER);

        if (type.equals("invitation") && invitationId != null) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/invitations/" + invitationId))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body()).getJSONObject("invitation");
                    String senderName = json.getString("senderName");
                    String projectName = json.getString("projectName");
                    JLabel extraDetails = new JLabel("<b>Expéditeur:</b> " + senderName + "<br><b>Projet:</b> " + projectName + "");
                    extraDetails.setForeground(Color.WHITE);
                    contentPanel.add(extraDetails, BorderLayout.SOUTH);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des détails de l'invitation : " + e.getMessage());
            }
        }

        JButton closeButton = new JButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
        if (!"read".equals(getNotificationStatus(notificationId))) {
            markNotificationAsRead(notificationId);
        }
    }

    private String getNotificationStatus(String notificationId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications/" + notificationId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                return json.optString("status", "unread");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du statut de la notification : " + e.getMessage());
        }
        return "unread";
    }

    private void markNotificationAsRead(String notificationId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications/" + notificationId + "/mark-read"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"status\": \"read\"}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "Notification marquée comme lue.");
                MainForm mainForm = (MainForm) SwingUtilities.getAncestorOfClass(MainForm.class, this);
                if (mainForm != null) {
                    mainForm.fetchNotificationsAndInvitations();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du marquage comme lu : " + e.getMessage());
        }
    }

    private void respondToInvitation(String invitationId, String response, String notificationId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/invitations/" + invitationId + "/respond?response=" + response))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                Notifications.getInstance().show(
                        Notifications.Type.SUCCESS,
                        Notifications.Location.TOP_CENTER,
                        7000,
                        "Réponse envoyée avec succès !"
                );
                markNotificationAsRead(notificationId); // Marquer automatiquement comme lu
                MainForm mainForm = (MainForm) SwingUtilities.getAncestorOfClass(MainForm.class, this);
                if (mainForm != null) {
                    mainForm.fetchNotificationsAndInvitations();
                }
            } else {
                Notifications.getInstance().show(
                        Notifications.Type.ERROR,
                        Notifications.Location.TOP_CENTER,
                        7000,
                        "Erreur lors de la réponse à l'invitation."
                );
            }
        } catch (Exception e) {
            Notifications.getInstance().show(
                    Notifications.Type.ERROR,
                    Notifications.Location.TOP_CENTER,
                    7000,
                    "Erreur serveur: " + e.getMessage()
            );
        }
    }

    private String checkInvitationStatus(String invitationId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/invitations/" + invitationId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONObject invitation = jsonResponse.getJSONObject("invitation");
                return invitation.optString("status", "pending");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du statut de l'invitation: " + e.getMessage());
        }
        return "pending";
    }

    private Color getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "accepted":
                return new Color(70, 160, 70); // Vert
            case "declined":
                return new Color(200, 50, 50); // Rouge
            case "pending":
            default:
                return new Color(200, 200, 200); // Gris
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Inbox");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lb;
    // End of variables declaration//GEN-END:variables
}
