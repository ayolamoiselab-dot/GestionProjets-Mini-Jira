package com.aemtechnology.gestionprojet.forms;

import com.aemtechnology.gestionprojet.components.SimpleForm;
import com.aemtechnology.gestionprojet.view.HomeScreen;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class ProjectsForm extends SimpleForm {

    private JPanel projectsPanel;
    private HomeScreen homeScreen;

    private JTextField searchField; // Champ de recherche
    private List<JSONObject> allProjects; // Liste de tous les projets pour le filtrage

    public ProjectsForm(HomeScreen homeScreen) {
        this.homeScreen = homeScreen;
        this.allProjects = new ArrayList<>();
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, gap 10", "[grow]", "[shrink 0][][shrink 0][grow]")); // Ajout d'une ligne pour la barre de recherche
        setOpaque(false);

        // Panneau supérieur pour le titre et le bouton
        JPanel topPanel = new JPanel(new MigLayout("fill, gap 10", "[grow][]", "[]"));
        topPanel.setOpaque(false);

        // Titre
        JLabel header = new JLabel("Vos Projets");
        header.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+2;"
                + "foreground:$Label.foreground;"
                + "border:0,0,5,0");
        topPanel.add(header, "growx");

        // Bouton pour ajouter un nouveau projet
        JButton addProjectBtn = new JButton("+ Nouveau Projet");
        addProjectBtn.putClientProperty(FlatClientProperties.STYLE, ""
                + "[light]background:darken(@background,10%);"
                + "[dark]background:lighten(@background,10%);"
                + "[light]foreground:#FFFFFF;"
                + "[dark]foreground:#000000;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0;"
                + "arc:10");
        addProjectBtn.addActionListener(e -> {
            System.out.println("Bouton 'Nouveau Projet' cliqué");
            homeScreen.showProjectCreationWizard();
        });
        topPanel.add(addProjectBtn, "align right");

        // Ajouter le panneau supérieur
        add(topPanel, "growx, wrap, shrink 0");

        // Barre de recherche
        JPanel searchPanel = new JPanel(new MigLayout("fill, gap 10", "[grow]", "[]"));
        searchPanel.setOpaque(false);

        searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:darken(@background,5%);"
                + "foreground:$TextField.foreground;"
                + "borderWidth:1;"
                + "focusWidth:1;"
                + "arc:10;"
                + "margin:5,10,5,10");
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher un projet...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterProjects(searchField.getText().trim().toLowerCase());
            }
        });
        searchPanel.add(searchField, "growx");

        // Ajouter la barre de recherche
        add(searchPanel, "growx, wrap, shrink 0");

        // Panneau pour les cartes de projets
        projectsPanel = new JPanel(new MigLayout("wrap 3, fillx, gap 20, insets 20", "[grow]", "[center]"));
        projectsPanel.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20;"
                + "[light]background:lighten(@background,5%);"
                + "[dark]background:darken(@background,5%);");
        projectsPanel.setOpaque(false);

        // Ajouter directement projectsPanel sans JScrollPane
        add(projectsPanel, "grow, wrap");

        // Charger les projets
        refreshProjects();
    }

    @Override
    public void formRefresh() {
        refreshProjects();
    }

    @Override
    public void formInitAndOpen() {
        System.out.println("ProjectsForm init and open");
    }

    @Override
    public void formOpen() {
        System.out.println("ProjectsForm open");
    }

    public void refreshProjects() {
        allProjects.clear(); // Réinitialiser la liste des projets
        projectsPanel.removeAll();

        try {
            var client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + homeScreen.getUid()))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Réponse de l'API - Status: " + response.statusCode() + ", Body: " + response.body());

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray projectsArray = jsonResponse.getJSONArray("projects");
            System.out.println("Nombre de projets trouvés : " + projectsArray.length());

            for (int i = 0; i < projectsArray.length(); i++) {
                JSONObject project = projectsArray.getJSONObject(i);
                allProjects.add(project); // Stocker tous les projets
            }

            // Afficher tous les projets initialement
            filterProjects("");
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des projets : " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur lors de la récupération des projets : " + e.getMessage());
        }

        projectsPanel.revalidate();
        projectsPanel.repaint();
    }

    private void filterProjects(String searchText) {
        projectsPanel.removeAll();

        for (JSONObject project : allProjects) {
            String projectName = project.getString("projectName").toLowerCase();
            if (searchText.isEmpty() || projectName.contains(searchText)) {
                String projectId = project.getString("projectId");
                String type = project.getString("type");
                long startDateMillis = project.optLong("startDate", 0);
                long endDateMillis = project.optLong("endDate", 0);
                int durationValue = project.optInt("durationValue", 0);
                String durationUnit = project.optString("durationUnit", "");
                long createdAt = project.optLong("createdAt", System.currentTimeMillis());

                // Vérifier si le projet est de type "équipe" et n'a pas de tâches
                if ("équipe".equalsIgnoreCase(type)) {
                    boolean hasTasks = homeScreen.checkIfProjectHasTasks(projectId);
                    if (!hasTasks) {
                        homeScreen.showNoTasksNotification(projectId, projectName);
                        if (!homeScreen.getProjectsWithoutTasks().containsKey(projectId)) {
                            homeScreen.getProjectsWithoutTasks().put(projectId, createdAt);
                            homeScreen.scheduleProjectDeletion(projectId, createdAt);
                        }
                    } else {
                        homeScreen.getProjectsWithoutTasks().remove(projectId);
                    }
                }

                // Créer et ajouter la carte
                JPanel card = homeScreen.createProjectCard(projectId, projectName, type, startDateMillis, endDateMillis, durationValue, durationUnit, null);
                projectsPanel.add(card, "grow, w 300!, h 200!");
            }
        }

        projectsPanel.revalidate();
        projectsPanel.repaint();
    }

    public JPanel getProjectsPanel() {
        return projectsPanel;
    }

    // Ajouter un getter pour uid (utilisé dans refreshProjects)
    public String getUid() {
        return homeScreen.getUid();
    }
}
