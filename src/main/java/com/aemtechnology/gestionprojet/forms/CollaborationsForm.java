package com.aemtechnology.gestionprojet.forms;

import com.aemtechnology.gestionprojet.view.HomeScreen;
import com.formdev.flatlaf.FlatClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.miginfocom.swing.MigLayout;

public class CollaborationsForm extends JPanel {

    private final HomeScreen homeScreen;
    private JPanel collaborationsPanel;

    public CollaborationsForm(HomeScreen homeScreen) {
        this.homeScreen = homeScreen;
        init();
        refreshCollaborations();
    }

    private void init() {
        setOpaque(false);
        setLayout(new BorderLayout());

        boolean isLightMode = UIManager.getLookAndFeel().getName().contains("Light");
        JLabel titleLabel = new JLabel("Mes Collaborations");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(isLightMode ? Color.BLACK : Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titleLabel, BorderLayout.NORTH);

        collaborationsPanel = new JPanel();
        collaborationsPanel.setOpaque(false);
        collaborationsPanel.setLayout(new MigLayout("wrap 3, fillx, top", "[grow]", "[]"));
        JScrollPane scrollPane = new JScrollPane(collaborationsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshCollaborations() {
        collaborationsPanel.removeAll();
        try {
            HttpClient client = HttpClient.newHttpClient();
            System.out.println("Fetching collaborations for UID: " + homeScreen.getUid());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/collaborations/" + homeScreen.getUid()))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Réponse de /api/collaborations/" + homeScreen.getUid() + ": " + response.body());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray collaborations = jsonResponse.getJSONArray("collaborations");
                System.out.println("Nombre de collaborations trouvées : " + collaborations.length());
                for (int i = 0; i < collaborations.length(); i++) {
                    JSONObject project = collaborations.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    String projectName = project.getString("projectName");
                    String type = project.getString("type");
                    long startDateMillis = project.optLong("startDate", 0);
                    long endDateMillis = project.optLong("endDate", 0);
                    int durationValue = project.optInt("durationValue", 0);
                    String durationUnit = project.optString("durationUnit", "");

                    // Créer un MouseListener personnalisé pour ce projet
                    MouseAdapter clickListener = new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            showAssignedTasks(project);
                        }
                    };

                    // Passer le MouseListener personnalisé à createProjectCard
                    JPanel card = homeScreen.createProjectCard(projectId, projectName, type, startDateMillis, endDateMillis, durationValue, durationUnit, clickListener);
                    collaborationsPanel.add(card, "grow, w 300!, h 200!");
                }
            } else {
                System.err.println("Erreur lors de la récupération des collaborations - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des collaborations: " + e.getMessage());
        }
        collaborationsPanel.revalidate();
        collaborationsPanel.repaint();
    }

    private void showAssignedTasks(JSONObject project) {
        System.out.println("Ouverture de AssignedTasksForm pour le projet : " + project.getString("projectName"));
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tâches Assignées", true);
        dialog.setLayout(new MigLayout("fill, wrap, gap 10", "[grow]", "[][][grow]"));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        AssignedTasksForm assignedTasksForm = new AssignedTasksForm(
                (Frame) SwingUtilities.getWindowAncestor(this),
                project,
                homeScreen.getUid(),
                homeScreen // Passer homeScreen
        );
        dialog.add(assignedTasksForm, "grow");

        dialog.setVisible(true);
    }

    private JPanel createTaskCard(JSONObject task) {
        JPanel card = new JPanel(new MigLayout("fill, gap 5", "[grow][]", "[]"));
        card.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "border:2,2,2,2,$Component.borderColor,,5");

        JLabel nameLabel = new JLabel(task.getString("name"));
        nameLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "foreground:$Label.foreground;");
        card.add(nameLabel, "growx");

        String status = task.getString("status");
        JLabel statusLabel = new JLabel(status);
        statusLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:" + getStatusColor(status) + ";");
        card.add(statusLabel);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showTaskDetails(task);
            }
        });

        return card;
    }

    private void showTaskDetails(JSONObject task) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Détails de la Tâche", true);
        dialog.setLayout(new MigLayout("fill, wrap, gap 10", "[grow]", "[][][][][]"));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JLabel nameLabel = new JLabel("Nom : " + task.getString("name"));
        dialog.add(nameLabel, "span");

        JLabel descriptionLabel = new JLabel("Description : " + task.getString("description"));
        dialog.add(descriptionLabel, "span");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        JLabel startDateLabel = new JLabel("Date de Début : " + dateFormat.format(new Date(task.getLong("startDate"))));
        dialog.add(startDateLabel, "span");

        JLabel endDateLabel = new JLabel("Date de Fin : " + dateFormat.format(new Date(task.getLong("endDate"))));
        dialog.add(endDateLabel, "span");

        JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"en cours", "terminé", "échoué"});
        statusComboBox.setSelectedItem(task.getString("status"));
        dialog.add(statusComboBox, "span");

        JButton saveButton = new JButton("Enregistrer");
        saveButton.addActionListener(e -> {
            String newStatus = statusComboBox.getSelectedItem().toString();
            if ("terminé".equals(newStatus) && !"terminé".equals(task.getString("status"))) {
                long startDate = task.getLong("startDate");
                long endDate = task.getLong("endDate");
                long duration = endDate - startDate;
                long days = duration / (1000 * 60 * 60 * 24);
                long timeSinceStart = System.currentTimeMillis() - startDate;
                long daysSinceStart = timeSinceStart / (1000 * 60 * 60 * 24);

                // Vérifier si la tâche est marquée comme terminée trop tôt
                if (days >= 7 && daysSinceStart <= 1) {
                    JOptionPane.showMessageDialog(dialog, "Cette tâche s'étend sur " + days + " jours. Il n'est pas raisonnable de la marquer comme terminée après seulement " + daysSinceStart + " jour(s).", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try {
                JSONObject taskData = new JSONObject();
                taskData.put("status", newStatus);
                taskData.put("projectId", task.getString("projectId"));

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + task.getString("taskId") + "/update"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(taskData.toString()))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    task.put("status", newStatus);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Erreur lors de la mise à jour de la tâche.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                System.err.println("Erreur lors de la mise à jour de la tâche : " + ex.getMessage());
            }
        });
        dialog.add(saveButton, "span, center");

        dialog.setVisible(true);
    }

    private String getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "terminé":
                return "#4CAF50"; // Vert
            case "en cours":
                return "#FFCA28"; // Jaune
            case "échoué":
                return "#F44336"; // Rouge
            default:
                return "$Label.foreground";
        }
    }
}
