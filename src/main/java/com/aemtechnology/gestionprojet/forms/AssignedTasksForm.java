package com.aemtechnology.gestionprojet.forms;

import com.aemtechnology.gestionprojet.view.HomeScreen;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
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
import java.util.Date;

public class AssignedTasksForm extends JPanel {

    private JSONObject project;
    private String collaboratorUid;
    private JPanel tasksPanel;
    private HomeScreen homeScreen;
    private JButton updateButton;
    private boolean hasChanges = false;
    private JProgressBar projectProgressBar; // Référence à la barre de progression globale

    public AssignedTasksForm(Frame parent, JSONObject project, String collaboratorUid, HomeScreen homeScreen) {
        this.project = project;
        this.collaboratorUid = collaboratorUid;
        this.homeScreen = (HomeScreen) parent;
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, wrap, gap 10", "[grow]", "[][][grow][]"));
        putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,5%);"
                + "[dark]background:darken(@background,5%);"
                + "border:5,5,5,5,$Component.borderColor,,20");

        // Panneau pour le titre et le bouton de fermeture
        JPanel headerPanel = new JPanel(new MigLayout("fill, gap 10", "[grow][]", "[]"));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Tâches Assignées pour : " + project.getString("projectName"));
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
            Window dialog = SwingUtilities.getWindowAncestor(this);
            if (dialog != null) {
                dialog.dispose();
            }
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

        // Bouton "Mettre à jour"
        updateButton = new JButton("Mettre à jour");
        updateButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#4CAF50;"
                + "foreground:#FFFFFF;"
                + "borderWidth:0;"
                + "arc:10");
        updateButton.setVisible(false);
        updateButton.addActionListener(e -> saveChanges());
        add(updateButton, "span, center");

        loadTasks();
    }

    private void loadTasks() {
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

                checkTaskReminders(tasks);

                for (int i = 0; i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    JSONArray assignedToArray = task.optJSONArray("assignedTo");
                    if (assignedToArray != null) {
                        boolean isAssigned = false;
                        for (int j = 0; j < assignedToArray.length(); j++) {
                            String assignedUid = assignedToArray.getString(j);
                            HttpRequest uidRequest = HttpRequest.newBuilder()
                                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + assignedUid))
                                    .GET()
                                    .build();
                            HttpResponse<String> uidResponse = client.send(uidRequest, HttpResponse.BodyHandlers.ofString());
                            if (uidResponse.statusCode() == 200) {
                                JSONObject uidJson = new JSONObject(uidResponse.body());
                                assignedUid = uidJson.getString("uid");
                            }
                            if (assignedUid.equals(collaboratorUid)) {
                                isAssigned = true;
                                break;
                            }
                        }
                        if (isAssigned) {
                            JPanel taskCard = createTaskCard(task);
                            tasksPanel.add(taskCard, "growx");
                            // Stocker la tâche dans les propriétés du composant pour une récupération ultérieure
                            taskCard.putClientProperty("task", task);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des tâches : " + e.getMessage());
        }
    }

    private JPanel createTaskCard(JSONObject task) {
        JPanel card = new JPanel(new MigLayout("fill, wrap, gap 5", "[grow][]", "[][][]"));
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

        // Menu déroulant pour modifier le statut
        JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"en cours", "terminé", "échoué"});
        statusComboBox.setSelectedItem(status);
        statusComboBox.addActionListener(e -> {
            String newStatus = statusComboBox.getSelectedItem().toString();
            if (!newStatus.equals(task.getString("status"))) {
                hasChanges = true;
                updateButton.setVisible(true);
                task.put("status", newStatus);
                statusLabel.setText(newStatus);
                statusLabel.putClientProperty(FlatClientProperties.STYLE, ""
                        + "foreground:" + getStatusColor(newStatus) + ";");
                progressBar.setValue(calculateTaskProgress(startDate, endDate, currentTime, newStatus));
                if ("échoué".equals(newStatus)) {
                    progressBar.setForeground(Color.RED);
                    progressBar.setValue(0);
                    progressBar.setString("Échoué");
                } else if ("terminé".equals(newStatus)) {
                    progressBar.setForeground(new Color(0x4CAF50));
                    progressBar.setValue(100);
                    progressBar.setString("Terminé");
                } else {
                    progressBar.setForeground(new Color(0x4CAF50));
                    progressBar.setStringPainted(true);
                }
            }
        });
        card.add(statusComboBox, "growx");

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

        // Stocker la barre de progression dans les propriétés du composant
        card.putClientProperty("progressBar", progressBar);

        return card;
    }

    private void saveChanges() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            for (Component component : tasksPanel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel card = (JPanel) component;
                    JSONObject task = (JSONObject) card.getClientProperty("task");
                    if (task != null) {
                        String newStatus = task.getString("status");
                        JSONObject taskData = new JSONObject();
                        taskData.put("status", newStatus);
                        taskData.put("projectId", project.getString("projectId"));

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + task.getString("taskId") + "/update"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(taskData.toString()))
                                .build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() != 200) {
                            Notifications.getInstance().show(
                                    Notifications.Type.ERROR,
                                    Notifications.Location.TOP_CENTER,
                                    "Erreur lors de la mise à jour de la tâche."
                            );
                            return;
                        }

                        // Mettre à jour la barre de progression de la tâche
                        JProgressBar taskProgressBar = (JProgressBar) card.getClientProperty("progressBar");
                        if (taskProgressBar != null) {
                            if ("terminé".equals(newStatus)) {
                                taskProgressBar.setValue(100);
                                taskProgressBar.setString("Terminé");
                                taskProgressBar.setForeground(new Color(0x4CAF50));
                            } else if ("échoué".equals(newStatus)) {
                                taskProgressBar.setValue(0);
                                taskProgressBar.setString("Échoué");
                                taskProgressBar.setForeground(Color.RED);
                            } else {
                                long startDate = task.getLong("startDate");
                                long endDate = task.getLong("endDate");
                                long currentTime = System.currentTimeMillis();
                                int progress = calculateTaskProgress(startDate, endDate, currentTime, newStatus);
                                taskProgressBar.setValue(progress);
                                taskProgressBar.setString(progress + "%");
                                taskProgressBar.setForeground(new Color(0x4CAF50));
                            }
                        }

                        // Notifier tous les membres du projet
                        notifyProjectMembers(task, newStatus);
                    }
                }
            }

            // Afficher une notification toast de succès
            Notifications.getInstance().show(
                    Notifications.Type.SUCCESS,
                    Notifications.Location.TOP_CENTER,
                    "Tâches mises à jour avec succès !"
            );

            // Recalculer la progression globale du projet
            int newProjectProgress = calculateProjectProgress();

            // Animer la mise à jour de la barre de progression globale
            animateProgressBar(projectProgressBar, projectProgressBar.getValue(), newProjectProgress);

            hasChanges = false;
            updateButton.setVisible(false);

            // Afficher la fenêtre de félicitations
            showCongratulationDialog();

            // Rafraîchir les projets dans HomeScreen
            homeScreen.refreshProjectsAfterUpdate();
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des tâches : " + e.getMessage());
            Notifications.getInstance().show(
                    Notifications.Type.ERROR,
                    Notifications.Location.TOP_CENTER,
                    "Erreur serveur : " + e.getMessage()
            );
        }
    }

    // Méthode pour animer la barre de progression
    private void animateProgressBar(JProgressBar progressBar, int startValue, int endValue) {
        final int duration = 1000; // Durée de l'animation en millisecondes (1 seconde)
        final int steps = 50; // Nombre d'étapes pour l'animation
        final int stepValue = (endValue - startValue) / steps; // Valeur à incrémenter à chaque étape
        final int stepDelay = duration / steps; // Délai entre chaque étape

        Timer timer = new Timer(stepDelay, null);
        timer.addActionListener(e -> {
            int currentValue = progressBar.getValue();
            if ((stepValue > 0 && currentValue < endValue) || (stepValue < 0 && currentValue > endValue)) {
                int newValue = currentValue + stepValue;
                // S'assurer que la valeur reste dans les bornes
                if (stepValue > 0) {
                    newValue = Math.min(newValue, endValue);
                } else {
                    newValue = Math.max(newValue, endValue);
                }
                progressBar.setValue(newValue);
                progressBar.setString(newValue + "%");
            } else {
                progressBar.setValue(endValue);
                progressBar.setString(endValue + "%");
                timer.stop();
            }
        });
        timer.start();
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

            if (!"terminé".equals(status) && System.currentTimeMillis() > endDate) {
                task.put("status", "échoué");
                updateTaskStatus(task, "échoué");

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

    private void updateTaskStatus(JSONObject task, String newStatus) {
        try {
            task.put("status", newStatus);

            JSONObject taskData = new JSONObject();
            taskData.put("status", newStatus);
            taskData.put("projectId", project.getString("projectId"));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + task.getString("taskId") + "/update"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(taskData.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Notifications.getInstance().show(
                        Notifications.Type.SUCCESS,
                        Notifications.Location.TOP_CENTER,
                        "Statut de la tâche mis à jour avec succès !"
                );
            } else {
                Notifications.getInstance().show(
                        Notifications.Type.ERROR,
                        Notifications.Location.TOP_CENTER,
                        "Erreur lors de la mise à jour du statut de la tâche."
                );
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour de la tâche : " + e.getMessage());
            Notifications.getInstance().show(
                    Notifications.Type.ERROR,
                    Notifications.Location.TOP_CENTER,
                    "Erreur serveur : " + e.getMessage()
            );
        }
    }

    // Méthode pour notifier tous les membres du projet
    private void notifyProjectMembers(JSONObject task, String newStatus) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Récupérer les collaborateurs du projet
            HttpRequest collaboratorsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + project.getString("projectId") + "/collaborators"))
                    .GET()
                    .build();
            HttpResponse<String> collaboratorsResponse = client.send(collaboratorsRequest, HttpResponse.BodyHandlers.ofString());
            if (collaboratorsResponse.statusCode() != 200) {
                System.err.println("Erreur lors de la récupération des collaborateurs : " + collaboratorsResponse.body());
                return;
            }

            JSONObject collaboratorsJson = new JSONObject(collaboratorsResponse.body());
            JSONArray collaborators = collaboratorsJson.getJSONArray("collaborators");

            // Récupérer le nom de l'utilisateur actuel
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + collaboratorUid))
                    .GET()
                    .build();
            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            String userFullName = "Un utilisateur";
            if (userResponse.statusCode() == 200) {
                JSONObject userJson = new JSONObject(userResponse.body());
                userFullName = userJson.optString("fullName", "Un utilisateur");
            }

            // Créer le message de notification
            String message = userFullName + " a mis à jour la tâche '" + task.getString("name") + "' à l'état : " + newStatus;

            // Notifier chaque collaborateur
            for (int i = 0; i < collaborators.length(); i++) {
                JSONObject collaborator = collaborators.getJSONObject(i);
                String collaboratorUid = collaborator.getString("uid");
                String collaboratorEmail = collaborator.getString("email");

                // Créer une notification dans l'application
                JSONObject notificationData = new JSONObject();
                notificationData.put("receiverUid", collaboratorUid);
                notificationData.put("message", message);
                notificationData.put("type", "task_update");
                notificationData.put("createdAt", System.currentTimeMillis());

                HttpRequest notificationRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(notificationData.toString()))
                        .build();
                client.send(notificationRequest, HttpResponse.BodyHandlers.ofString());

                // Envoyer un e-mail
                try {
                    HttpRequest emailRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/send-email"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(new JSONObject()
                                    .put("to", collaboratorEmail)
                                    .put("subject", "Mise à jour de tâche sur le projet " + project.getString("projectName"))
                                    .put("body", message)
                                    .toString()))
                            .build();
                    client.send(emailRequest, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi de l'email à " + collaboratorEmail + " : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la notification des membres du projet : " + e.getMessage());
        }
    }

// Méthode pour afficher la fenêtre de félicitations
// Dans AssignedTasksForm
    private void showCongratulationDialog() {
        try {
            // Récupérer le nom de l'utilisateur
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + collaboratorUid))
                    .GET()
                    .build();
            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            String userFullName = "Utilisateur";
            if (userResponse.statusCode() == 200) {
                JSONObject userJson = new JSONObject(userResponse.body());
                userFullName = userJson.optString("fullName", "Utilisateur");
            }

            // Créer le message de félicitations
            String message = "🎉 Merci pour ta contribution au projet " + project.getString("projectName") + ", " + userFullName + " ! 🎉";

            // Récupérer la fenêtre parente
            Window parentWindow = SwingUtilities.getWindowAncestor(this);

            // Créer une fenêtre modale de félicitations
            JDialog congratsDialog = new JDialog(parentWindow, "Félicitations !", Dialog.ModalityType.APPLICATION_MODAL);
            congratsDialog.setLayout(new MigLayout("fill, wrap, gap 10", "[center]", "[center][center]"));
            congratsDialog.setSize(400, 200);
            congratsDialog.setLocationRelativeTo(parentWindow);

            JLabel messageLabel = new JLabel("<center>" + message + "</center>");
            messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            messageLabel.setForeground(new Color(0x4CAF50));
            congratsDialog.add(messageLabel, "span, center");

            JButton okButton = new JButton("OK");
            okButton.putClientProperty(FlatClientProperties.STYLE, ""
                    + "background:#4CAF50;"
                    + "foreground:#FFFFFF;"
                    + "borderWidth:0;"
                    + "arc:10");
            okButton.addActionListener(e -> {
                congratsDialog.dispose();
                // Fermer le JDialog principal (AssignedTasksForm)
                Window dialog = SwingUtilities.getWindowAncestor(this);
                if (dialog != null) {
                    dialog.dispose();
                }
                // Notifier HomeScreen pour rafraîchir les projets
                homeScreen.refreshAllProjects();
            });
            congratsDialog.add(okButton, "span, center");

            congratsDialog.setVisible(true);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage de la fenêtre de félicitations : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
