package com.aemtechnology.gestionprojet.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AddTaskForm extends JDialog {

    private JTextField nameField;
    private JTextArea descriptionArea;
    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;
    private JButton saveButton;
    private JButton cancelButton;
    private JSONObject project;
    private JCheckBox assignToCollaboratorCheckBox;
    private JTextField collaboratorSearchField;
    private JList<String> collaboratorList;
    private DefaultListModel<String> collaboratorListModel;
    private List<JSONObject> collaborators;
    private List<String> selectedCollaboratorUids; // Liste pour stocker les UIDs des collaborateurs sélectionnés

    public AddTaskForm(Frame parent, JSONObject project) {
        super(parent, "Ajouter une Tâche", true);
        this.project = project;
        this.collaborators = new ArrayList<>();
        this.selectedCollaboratorUids = new ArrayList<>(); // Initialisation de la liste
        fetchCollaborators();
        init();
    }

    private void fetchCollaborators() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + project.getString("projectId") + "/collaborators"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Réponse de /api/projects/" + project.getString("projectId") + "/collaborators: " + response.body());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray collaboratorsArray = jsonResponse.getJSONArray("collaborators");
                for (int i = 0; i < collaboratorsArray.length(); i++) {
                    JSONObject user = collaboratorsArray.getJSONObject(i);
                    // S'assurer que l'UID est bien récupéré
                    String uid = user.optString("uid");
                    if (uid == null || uid.isEmpty()) {
                        // Si l'UID n'est pas présent, essayer de le récupérer via l'email
                        String email = user.optString("email");
                        if (email != null && !email.isEmpty()) {
                            HttpRequest uidRequest = HttpRequest.newBuilder()
                                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + email))
                                    .GET()
                                    .build();
                            HttpResponse<String> uidResponse = client.send(uidRequest, HttpResponse.BodyHandlers.ofString());
                            if (uidResponse.statusCode() == 200) {
                                JSONObject uidJson = new JSONObject(uidResponse.body());
                                user.put("uid", uidJson.getString("uid"));
                            }
                        }
                    }
                    collaborators.add(user);
                }
            } else {
                System.err.println("Erreur lors de la récupération des collaborateurs - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des collaborateurs : " + e.getMessage());
        }
    }

    private void init() {
        setLayout(new MigLayout("fill, wrap, gap 10", "[grow]", "[][][][][][][][]"));
        getRootPane().putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,5%);"
                + "[dark]background:darken(@background,5%);"
                + "border:5,5,5,5,$Component.borderColor,,20");

        // Titre
        JLabel titleLabel = new JLabel("Ajouter une Tâche au Projet : " + project.getString("projectName"));
        titleLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+2;"
                + "foreground:$Label.foreground;");
        add(titleLabel, "span, center");

        // Nom de la tâche
        JLabel nameLabel = new JLabel("Nom de la Tâche");
        nameLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(nameLabel);

        nameField = new JTextField();
        nameField.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "foreground:$TextField.foreground;"
                + "border:2,2,2,2,$Component.borderColor,,5");
        add(nameField, "growx");

        // Description
        JLabel descriptionLabel = new JLabel("Description");
        descriptionLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(descriptionLabel);

        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "border:2,2,2,2,$Component.borderColor,,5");
        descriptionArea.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$TextArea.foreground;"
                + "background:$TextArea.background;");
        add(descriptionScroll, "growx");

        // Date de début
        JLabel startDateLabel = new JLabel("Date de Début");
        startDateLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(startDateLabel);

        SpinnerDateModel startDateModel = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        startDateSpinner = new JSpinner(startDateModel);
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd/MM/yyyy"));
        startDateSpinner.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "foreground:$Spinner.foreground;");
        add(startDateSpinner, "growx");

        // Date de fin
        JLabel endDateLabel = new JLabel("Date de Fin");
        endDateLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(endDateLabel);

        SpinnerDateModel endDateModel = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        endDateSpinner = new JSpinner(endDateModel);
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "dd/MM/yyyy"));
        endDateSpinner.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "foreground:$Spinner.foreground;");
        add(endDateSpinner, "growx");

        // Option pour attribuer à un collaborateur
        assignToCollaboratorCheckBox = new JCheckBox("Attribuer à un collaborateur");
        assignToCollaboratorCheckBox.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:$Label.foreground;");
        add(assignToCollaboratorCheckBox, "span");

        // Barre de recherche pour les collaborateurs
        collaboratorSearchField = new JTextField();
        collaboratorSearchField.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "foreground:$TextField.foreground;"
                + "border:2,2,2,2,$Component.borderColor,,5");
        collaboratorSearchField.putClientProperty("JTextField.placeholderText", "Rechercher un collaborateur...");
        collaboratorSearchField.setVisible(false);
        add(collaboratorSearchField, "growx");

        // Liste des collaborateurs
        collaboratorListModel = new DefaultListModel<>();
        collaboratorList = new JList<>(collaboratorListModel);
        collaboratorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Activer la sélection multiple
        collaboratorList.setVisible(false);
        JScrollPane collaboratorScroll = new JScrollPane(collaboratorList);
        collaboratorScroll.setPreferredSize(new Dimension(0, 100));
        collaboratorScroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:lighten(@background,10%);"
                + "[dark]background:darken(@background,10%);"
                + "border:2,2,2,2,$Component.borderColor,,5");
        add(collaboratorScroll, "growx");

        // Charger les collaborateurs dans la liste
        for (JSONObject collaborator : collaborators) {
            collaboratorListModel.addElement(collaborator.getString("fullName"));
        }

        // Gestion de la visibilité des champs de recherche
        assignToCollaboratorCheckBox.addActionListener(e -> {
            boolean isSelected = assignToCollaboratorCheckBox.isSelected();
            collaboratorSearchField.setVisible(isSelected);
            collaboratorList.setVisible(isSelected);
            collaboratorSearchField.setText("");
            collaboratorListModel.clear();
            for (JSONObject collaborator : collaborators) {
                collaboratorListModel.addElement(collaborator.getString("fullName"));
            }
            revalidate();
            repaint();
        });

        // Filtrer les collaborateurs lors de la recherche
        collaboratorSearchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String searchText = collaboratorSearchField.getText().trim().toLowerCase();
                collaboratorListModel.clear();
                for (JSONObject collaborator : collaborators) {
                    String fullName = collaborator.getString("fullName").toLowerCase();
                    if (searchText.isEmpty() || fullName.contains(searchText)) {
                        collaboratorListModel.addElement(collaborator.getString("fullName"));
                    }
                }
            }
        });

        // Sélectionner plusieurs collaborateurs
        collaboratorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedCollaboratorUids.clear(); // Réinitialiser la liste des UIDs sélectionnés
                List<String> selectedNames = collaboratorList.getSelectedValuesList();
                for (String selectedName : selectedNames) {
                    for (JSONObject collaborator : collaborators) {
                        if (collaborator.getString("fullName").equals(selectedName)) {
                            selectedCollaboratorUids.add(collaborator.getString("uid"));
                            break;
                        }
                    }
                }
            }
        });

        // Boutons
        JPanel buttonPanel = new JPanel(new MigLayout("gap 10", "[][]", "[]"));
        buttonPanel.setOpaque(false);

        saveButton = new JButton("Enregistrer");
        saveButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:#4CAF50;"
                + "[dark]background:#66BB6A;"
                + "foreground:#FFFFFF;"
                + "borderWidth:0;"
                + "arc:10");
        saveButton.addActionListener(e -> saveTask());
        buttonPanel.add(saveButton);

        cancelButton = new JButton("Annuler");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:#F44336;"
                + "[dark]background:#EF5350;"
                + "foreground:#FFFFFF;"
                + "borderWidth:0;"
                + "arc:10");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel, "span, center");

        setSize(400, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void saveTask() {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        Date startDate = (Date) startDateSpinner.getValue();
        Date endDate = (Date) endDateSpinner.getValue();

        // Validation des données
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom de la tâche est requis.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Vérifier la cohérence des dates avec le projet
        long projectStartDate = project.optLong("startDate", 0);
        long projectEndDate = project.optLong("endDate", 0);

        if (projectStartDate != 0 && startDate.getTime() < projectStartDate) {
            JOptionPane.showMessageDialog(this, "La date de début de la tâche ne peut pas être avant la date de début du projet.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (projectEndDate != 0 && endDate.getTime() > projectEndDate) {
            JOptionPane.showMessageDialog(this, "La date de fin de la tâche ne peut pas être après la date de fin du projet.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (startDate.after(endDate)) {
            JOptionPane.showMessageDialog(this, "La date de fin doit être après la date de début.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Enregistrer la tâche
        try {
            JSONObject taskData = new JSONObject();
            taskData.put("projectId", project.getString("projectId"));
            taskData.put("name", name);
            taskData.put("description", description);
            taskData.put("startDate", startDate.getTime());
            taskData.put("endDate", endDate.getTime());
            taskData.put("status", "en cours");
            taskData.put("createdAt", System.currentTimeMillis());
            if (assignToCollaboratorCheckBox.isSelected() && !selectedCollaboratorUids.isEmpty()) {
                taskData.put("assignedTo", selectedCollaboratorUids); // Envoyer un tableau d'UIDs
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(taskData.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Si la tâche est assignée à des collaborateurs, envoyer une notification à chacun
                if (assignToCollaboratorCheckBox.isSelected() && !selectedCollaboratorUids.isEmpty()) {
                    for (String collaboratorUid : selectedCollaboratorUids) {
                        JSONObject notificationData = new JSONObject();
                        notificationData.put("receiverUid", collaboratorUid);
                        notificationData.put("message", "Vous avez été assigné à la tâche : " + name + " dans le projet : " + project.getString("projectName"));
                        notificationData.put("type", "task_assignment");
                        notificationData.put("createdAt", System.currentTimeMillis());

                        HttpRequest notificationRequest = HttpRequest.newBuilder()
                                .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(notificationData.toString()))
                                .build();
                        client.send(notificationRequest, HttpResponse.BodyHandlers.ofString());
                    }
                }

                Notifications.getInstance().show(
                        Notifications.Type.SUCCESS,
                        Notifications.Location.TOP_CENTER,
                        "Tâche ajoutée avec succès !"
                );
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout de la tâche.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout de la tâche : " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur serveur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}
