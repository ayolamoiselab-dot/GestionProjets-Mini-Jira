package com.aemtechnology.gestionprojet.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProjectDetailsForm extends JDialog {

    private JSONObject project;
    private JPanel tasksPanel;
    private JProgressBar projectProgressBar;
    private WebSocketClient webSocketClient;
    private List<JSONObject> collaborators;

    public ProjectDetailsForm(Frame parent, JSONObject project) {
        super(parent, "Détails du Projet", true);
        this.project = project;
        this.collaborators = new ArrayList<>();
        fetchCollaborators();
        init();
        initWebSocket();
    }

    private void fetchCollaborators() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + project.getString("projectId") + "/collaborators"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray collaboratorsArray = jsonResponse.getJSONArray("collaborators");
                for (int i = 0; i < collaboratorsArray.length(); i++) {
                    JSONObject user = collaboratorsArray.getJSONObject(i);
                    collaborators.add(user);
                }
            } else {
                System.err.println("Erreur lors de la récupération des collaborateurs - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des collaborateurs : " + e.getMessage());
        }
    }

    private void initWebSocket() {
        try {
            URI uri = new URI("wss://teamworkatmini-jira.onrender.com/ws");
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("WebSocket connecté pour le projet : " + project.getString("projectId"));
                    // S'abonner au topic des mises à jour des tâches
                    JSONObject subscribeMessage = new JSONObject();
                    subscribeMessage.put("type", "subscribe");
                    subscribeMessage.put("destination", "/topic/tasks/" + project.getString("projectId"));
                    send(subscribeMessage.toString());
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("Message WebSocket reçu : " + message);
                    try {
                        JSONObject jsonMessage = new JSONObject(message);
                        String type = jsonMessage.optString("type", "");
                        if ("task_update".equals(type)) {
                            // Rafraîchir les tâches
                            SwingUtilities.invokeLater(() -> {
                                loadTasks();
                                updateProjectProgress();
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors du traitement du message WebSocket : " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket fermé : " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Erreur WebSocket : " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du WebSocket : " + e.getMessage());
        }
    }

    public void closeWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    private void init() {
        setLayout(new MigLayout("fill, wrap, gap 10", "[grow]", "[][][][][grow]"));
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 2));
        getRootPane().putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,5%);"
                + "[dark]background:darken(@background,5%);"
                + "border:5,5,5,5,$Component.borderColor,,20");

        // Panneau pour le titre et le bouton de fermeture
        JPanel headerPanel = new JPanel(new MigLayout("fill, gap 10", "[grow][]", "[]"));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(project.getString("projectName"));
        titleLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+3;"
                + "foreground:$Label.foreground;");
        headerPanel.add(titleLabel, "growx");

        JButton closeButton = new JButton("✕");
        closeButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "background:darken(@background,10%);"
                + "foreground:#FF5555;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0;"
                + "arc:5;"
                + "margin:5,10,5,10");
        closeButton.addActionListener(e -> {
            closeWebSocket();
            dispose();
        });
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.putClientProperty(FlatClientProperties.STYLE, ""
                        + "font:+1;"
                        + "background:darken(@background,15%);"
                        + "foreground:#FF7777;"
                        + "borderWidth:0;"
                        + "focusWidth:0;"
                        + "innerFocusWidth:0;"
                        + "arc:5;"
                        + "margin:5,10,5,10");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.putClientProperty(FlatClientProperties.STYLE, ""
                        + "font:+1;"
                        + "background:darken(@background,10%);"
                        + "foreground:#FF5555;"
                        + "borderWidth:0;"
                        + "focusWidth:0;"
                        + "innerFocusWidth:0;"
                        + "arc:5;"
                        + "margin:5,10,5,10");
            }
        });
        headerPanel.add(closeButton, "align right");
        add(headerPanel, "span, growx");

        // Informations supplémentaires sur le projet
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        long startDateMillis = project.optLong("startDate", 0);
        long endDateMillis = project.optLong("endDate", 0);
        String startDateText = startDateMillis != 0 ? dateFormat.format(new Date(startDateMillis)) : "Non spécifiée";
        String endDateText = endDateMillis != 0 ? dateFormat.format(new Date(endDateMillis)) : "Non spécifiée";

        JLabel typeLabel = new JLabel("Type: " + project.getString("type"));
        typeLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(typeLabel);

        JLabel datesLabel = new JLabel("Dates: " + startDateText + " - " + endDateText);
        datesLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(datesLabel);

        // Ajouter la description du projet
        JLabel descriptionLabel = new JLabel("Description: " + project.optString("description", "Aucune description"));
        descriptionLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(descriptionLabel);

        // Barre de progression globale du projet
        int projectProgress = calculateProjectProgress();
        projectProgressBar = new JProgressBar(0, 100);
        projectProgressBar.setValue(projectProgress);
        projectProgressBar.setStringPainted(true);
        projectProgressBar.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:#4CAF50;"
                + "background:darken(@background,10%);");
        add(new JLabel("Progression du Projet :"), "split 2");
        add(projectProgressBar, "growx");

        // Panneau des tâches
        tasksPanel = new JPanel(new MigLayout("wrap, fillx, gap 10", "[grow]", "[]"));
        tasksPanel.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,5%);"
                + "[dark]background:darken(@background,5%);"
                + "border:2,2,2,2,$Component.borderColor,,10");
        tasksPanel.setOpaque(false);

        JScrollPane tasksScroll = new JScrollPane(tasksPanel);
        tasksScroll.setOpaque(false);
        tasksScroll.getViewport().setOpaque(false);
        tasksScroll.setBorder(null);
        tasksScroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackInsets:5,5,5,5;"
                + "thumbInsets:5,5,5,5;"
                + "[light]background:@background;"
                + "[dark]background:@background;"
                + "[light]thumb:#666666;"
                + "[dark]thumb:#AAAAAA;");
        add(tasksScroll, "grow");

        loadTasks();

        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Animation d'ouverture
        setOpacity(0f);
        Timer timer = new Timer(20, null);
        final float[] opacity = {0f};
        timer.addActionListener(e -> {
            opacity[0] += 0.1f;
            setOpacity(Math.min(opacity[0], 1f));
            if (opacity[0] >= 1f) {
                timer.stop();
            }
        });
        timer.start();
    }

    private void loadTasks() {
        tasksPanel.removeAll();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + project.getString("projectId")))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray tasks = json.getJSONArray("tasks");

                // Vérifier les rappels
                checkTaskReminders(tasks);

                for (int i = 0; i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    JPanel taskCard = createTaskCard(task);
                    tasksPanel.add(taskCard, "growx");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des tâches : " + e.getMessage());
        }
        tasksPanel.revalidate();
        tasksPanel.repaint();
    }

    private JPanel createTaskCard(JSONObject task) {
        JPanel card = new JPanel(new MigLayout("fill, wrap, gap 5", "[grow][]", "[][]"));
        card.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "border:2,2,2,2,$Component.borderColor,,5");

        JPanel infoPanel = new JPanel(new MigLayout("fill, gap 5", "[grow][]", "[]"));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(task.getString("name"));
        nameLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "foreground:$Label.foreground;");
        infoPanel.add(nameLabel, "growx");

        String status = task.getString("status");
        JLabel statusLabel = new JLabel(status);
        statusLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:" + getStatusColor(status) + ";");
        infoPanel.add(statusLabel);

        JButton addCollaboratorButton = new JButton("➕");
        addCollaboratorButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "background:darken(@background,10%);"
                + "foreground:#4CAF50;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0;"
                + "arc:5;"
                + "margin:5,5,5,5");
        addCollaboratorButton.addActionListener(e -> showAddCollaboratorDialog(task));
        infoPanel.add(addCollaboratorButton);

        card.add(infoPanel, "growx");

        // Barre de progression de la tâche
        long startDate = task.getLong("startDate");
        long endDate = task.getLong("endDate");
        long currentTime = System.currentTimeMillis();
        int progress = calculateTaskProgress(startDate, endDate, currentTime, status);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(progress);
        progressBar.setStringPainted(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:#4CAF50;"
                + "background:darken(@background,10%);");
        if ("échoué".equals(status)) {
            progressBar.setForeground(Color.RED);
            progressBar.setValue(0);
            progressBar.setString("Échoué");
        } else if ("terminé".equals(status)) {
            progressBar.setValue(100);
            progressBar.setString("Terminé");
        }
        card.add(progressBar, "growx");

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.putClientProperty(FlatClientProperties.STYLE, ""
                        + "[light]background:lighten(@background,15%);"
                        + "[dark]background:darken(@background,15%);"
                        + "border:2,2,2,2,$Component.borderColor,,5");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.putClientProperty(FlatClientProperties.STYLE, ""
                        + "[light]background:lighten(@background,10%);"
                        + "[dark]background:darken(@background,10%);"
                        + "border:2,2,2,2,$Component.borderColor,,5");
            }
        });

        return card;
    }

    private void showAddCollaboratorDialog(JSONObject task) {
        JDialog dialog = new JDialog(this, "Ajouter un Collaborateur", true);
        dialog.setLayout(new MigLayout("fill, wrap, gap 10", "[grow]", "[][][]"));
        dialog.setSize(300, 300);
        dialog.setLocationRelativeTo(this);

        JLabel titleLabel = new JLabel("Sélectionner un Collaborateur");
        dialog.add(titleLabel, "span, center");

        DefaultListModel<String> collaboratorListModel = new DefaultListModel<>();
        JList<String> collaboratorList = new JList<>(collaboratorListModel);
        collaboratorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (JSONObject collaborator : collaborators) {
            collaboratorListModel.addElement(collaborator.getString("fullName"));
        }
        JScrollPane collaboratorScroll = new JScrollPane(collaboratorList);
        collaboratorScroll.setPreferredSize(new Dimension(0, 100));
        dialog.add(collaboratorScroll, "growx");

        JButton assignButton = new JButton("Assigner");
        assignButton.addActionListener(e -> {
            List<String> selectedNames = collaboratorList.getSelectedValuesList();
            if (selectedNames.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Veuillez sélectionner au moins un collaborateur.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<String> selectedUids = new ArrayList<>();
            for (String selectedName : selectedNames) {
                for (JSONObject collaborator : collaborators) {
                    if (collaborator.getString("fullName").equals(selectedName)) {
                        selectedUids.add(collaborator.getString("uid"));
                        break;
                    }
                }
            }

            // Mettre à jour la tâche avec les nouveaux collaborateurs
            try {
                JSONArray assignedToArray = task.optJSONArray("assignedTo");
                List<String> currentAssignedTo = new ArrayList<>();
                if (assignedToArray != null) {
                    for (int i = 0; i < assignedToArray.length(); i++) {
                        currentAssignedTo.add(assignedToArray.getString(i));
                    }
                }
                for (String uid : selectedUids) {
                    if (!currentAssignedTo.contains(uid)) {
                        currentAssignedTo.add(uid);
                    }
                }

                JSONObject taskData = new JSONObject();
                taskData.put("projectId", project.getString("projectId"));
                taskData.put("status", task.getString("status"));
                taskData.put("assignedTo", currentAssignedTo);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + task.getString("taskId") + "/update"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(taskData.toString()))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Mettre à jour l'objet task localement
                    task.put("assignedTo", currentAssignedTo);

                    // Envoyer une notification à chaque nouveau collaborateur
                    for (String uid : selectedUids) {
                        JSONObject notificationData = new JSONObject();
                        notificationData.put("receiverUid", uid);
                        notificationData.put("message", "Vous avez été assigné à la tâche : " + task.getString("name") + " dans le projet : " + project.getString("projectName"));
                        notificationData.put("type", "task_assignment");
                        notificationData.put("createdAt", System.currentTimeMillis());

                        HttpRequest notificationRequest = HttpRequest.newBuilder()
                                .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(notificationData.toString()))
                                .build();
                        client.send(notificationRequest, HttpResponse.BodyHandlers.ofString());
                    }

                    Notifications.getInstance().show(
                            Notifications.Type.SUCCESS,
                            Notifications.Location.TOP_CENTER,
                            "Collaborateurs assignés avec succès !"
                    );
                    dialog.dispose();
                } else {
                    Notifications.getInstance().show(
                            Notifications.Type.ERROR,
                            Notifications.Location.TOP_CENTER,
                            "Erreur lors de l'assignation des collaborateurs."
                    );
                }
            } catch (Exception ex) {
                System.err.println("Erreur lors de l'assignation des collaborateurs : " + ex.getMessage());
                Notifications.getInstance().show(
                        Notifications.Type.ERROR,
                        Notifications.Location.TOP_CENTER,
                        "Erreur serveur : " + ex.getMessage()
                );
            }
        });
        dialog.add(assignButton, "span, center");

        dialog.setVisible(true);
    }

    private int calculateTaskProgress(long startDate, long endDate, long currentTime, String status) {
        if (startDate == 0 || endDate == 0 || endDate <= startDate) {
            return 0;
        }
        if ("terminé".equals(status)) {
            return 100;
        }
        if ("échoué".equals(status)) {
            return 0;
        }
        if (currentTime < startDate) {
            return 0;
        }
        if (currentTime > endDate) {
            return 0;
        }
        long totalDuration = endDate - startDate;
        long elapsed = currentTime - startDate;
        return (int) ((elapsed * 100) / totalDuration);
    }

    private int calculateProjectProgress() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + project.getString("projectId")))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray tasks = json.getJSONArray("tasks");
                if (tasks.length() == 0) {
                    return 0;
                }
                int completedTasks = 0;
                for (int i = 0; i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    if ("terminé".equals(task.getString("status"))) {
                        completedTasks++;
                    }
                }
                return (completedTasks * 100) / tasks.length();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul de la progression du projet : " + e.getMessage());
        }
        return 0;
    }

    private void updateProjectProgress() {
        int projectProgress = calculateProjectProgress();
        projectProgressBar.setValue(projectProgress);
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

    private void checkTaskReminders(JSONArray tasks) {
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject task = tasks.getJSONObject(i);
            long endDate = task.getLong("endDate");
            String status = task.getString("status");
            JSONArray assignedToArray = task.optJSONArray("assignedTo");

            // Vérifier si la tâche est échouée à l'échéance
            if (!"terminé".equals(status) && System.currentTimeMillis() > endDate) {
                task.put("status", "échoué");
                updateTaskStatus(task);

                // Envoyer un rappel à chaque collaborateur assigné
                if (assignedToArray != null) {
                    for (int j = 0; j < assignedToArray.length(); j++) {
                        String assignedTo = assignedToArray.getString(j);
                        try {
                            JSONObject notificationData = new JSONObject();
                            notificationData.put("receiverUid", assignedTo);
                            notificationData.put("message", "La tâche '" + task.getString("name") + "' n'a pas été terminée à temps et a été marquée comme échouée.");
                            notificationData.put("type", "task_reminder");
                            notificationData.put("createdAt", System.currentTimeMillis());

                            HttpClient client = HttpClient.newHttpClient();
                            HttpRequest notificationRequest = HttpRequest.newBuilder()
                                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications"))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(notificationData.toString()))
                                    .build();
                            client.send(notificationRequest, HttpResponse.BodyHandlers.ofString());
                        } catch (Exception e) {
                            System.err.println("Erreur lors de l'envoi du rappel : " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void updateTaskStatus(JSONObject task) {
        try {
            JSONObject taskData = new JSONObject();
            taskData.put("status", task.getString("status"));
            taskData.put("projectId", project.getString("projectId"));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + task.getString("taskId") + "/update"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(taskData.toString()))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour de la tâche : " + e.getMessage());
        }
    }
}