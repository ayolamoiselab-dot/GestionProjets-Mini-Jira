package com.aemtechnology.gestionprojet.view;

import com.aemtechnology.gestionprojet.application.form.MainForm;
import com.aemtechnology.gestionprojet.application.form.other.FormDashboard;
import com.aemtechnology.gestionprojet.application.form.other.FormInbox;
import com.aemtechnology.gestionprojet.application.form.other.FormRead;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.swing.border.AbstractBorder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.chart.ChartLegendRenderer;
import raven.chart.bar.HorizontalBarChart;
import raven.chart.data.category.DefaultCategoryDataset;
import raven.chart.data.pie.DefaultPieDataset;
import raven.chart.line.LineChart;
import raven.chart.pie.PieChart;
import com.aemtechnology.gestionprojet.components.SimpleForm;
import com.aemtechnology.gestionprojet.forms.AddCollaboratorForm;
import com.aemtechnology.gestionprojet.forms.AddTaskForm;
import com.aemtechnology.gestionprojet.forms.CollaborationsForm;
import raven.drawer.Drawer;
import raven.utils.DateCalculator;
import com.aemtechnology.gestionprojet.forms.DashboardForm;
import com.aemtechnology.gestionprojet.forms.ProjectDetailsForm;
import com.aemtechnology.gestionprojet.forms.ProjectsForm;
import com.aemtechnology.gestionprojet.menu.FormManager;
import com.aemtechnology.gestionprojet.menu.MyDrawerBuilder;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.UIScale;
import raven.drawer.component.DrawerPanel;
import raven.popup.GlassPanePopup;
import raven.toast.Notifications;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import java.net.URI;

import java.util.TimerTask;

public class HomeScreen extends JFrame {

    private CollaborationsForm collaborationsForm;

    private WebSocketClient webSocketClient;
    public final String uid;
    private final String fullName;
    private final String email;
    private List<JPanel> projectCards = new ArrayList<>();
    private MainForm mainForm;
    private FormDashboard dashboardForm;
    private ProjectsForm projectsForm;
    private FormInbox inboxForm;
    private FormRead readForm;

    private JSONArray notifications;
    private JSONArray pendingInvitations;

    private Map<String, Long> projectsWithoutTasks; // Map pour suivre les projets sans tâches et leur timestamp

    private boolean isUpdatingDates = false; // Variable pour éviter les appels récursifs

    // Wizard components as instance variables
    private JPanel summaryCard;
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JComboBox<String> durationComboInstance;
    private JSpinner durationSpinnerInstance;
    private JDatePickerImpl startDatePickerInstance;
    private JDatePickerImpl endDatePickerInstance;
    private ButtonGroup typeButtonGroup;
    private List<JRadioButton> typeRadioButtons;

    private ProjectData projectData = new ProjectData();

    public FormDashboard getDashboardForm() {
        return dashboardForm;
    }

    public ProjectsForm getProjectsForm() {
        return projectsForm;
    }

    public FormInbox getInboxForm() {
        return inboxForm;
    }

    public FormRead getReadForm() {
        return readForm;
    }

    private String normalizeUidOrEmail(String identifier) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + identifier))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                return json.getString("uid");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la normalisation de l'identifiant : " + e.getMessage());
        }
        return identifier; // Retourner l'identifiant tel quel si la normalisation échoue
    }

    public HomeScreen(String uid, String fullName, String email) {
        System.out.println("Démarrage du constructeur de HomeScreen...");

        // Désactiver HomeScreen pour empêcher les interactions pendant le chargement
        setEnabled(false);

        // Créer et afficher la fenêtre de chargement
        LoadingDialog loadingDialog = new LoadingDialog(this);
        System.out.println("LoadingDialog créé et affiché.");
        loadingDialog.setVisible(true);

        this.uid = normalizeUidOrEmail(uid);
        this.projectsWithoutTasks = new HashMap<>();
        this.fullName = fullName != null && !fullName.trim().isEmpty() ? fullName : "User";
        this.email = email != null && !email.trim().isEmpty() ? email : "user@example.com";
        System.out.println("HomeScreen initialisé - uid: " + uid + ", fullName: " + fullName + ", email: " + email);

        // Charger les données dans un thread séparé
        new Thread(() -> {
            try {
                System.out.println("Début du chargement des données dans un thread séparé...");

                // Initialisation des composants
                System.out.println("Appel de initializeFrame...");
                initializeFrame();
                System.out.println("initializeFrame terminé.");

                System.out.println("Appel de initializeWizardComponents...");
                initializeWizardComponents();
                System.out.println("initializeWizardComponents terminé.");

                System.out.println("Appel de initializeForms...");
                initializeForms();
                System.out.println("initializeForms terminé.");

                System.out.println("Appel de initializeWebSocket...");
                initializeWebSocket(uid);
                System.out.println("initializeWebSocket terminé.");

                System.out.println("Appel de refreshNotifications...");
                refreshNotifications();
                System.out.println("refreshNotifications terminé.");

                System.out.println("Appel de refreshPendingInvitations...");
                refreshPendingInvitations();
                System.out.println("refreshPendingInvitations terminé.");

                // Une fois les données chargées, mettre à jour l'UI dans le thread EDT
                System.out.println("Données chargées, mise à jour de l'UI dans le thread EDT...");
                SwingUtilities.invokeLater(() -> {
                    try {
                        System.out.println("Appel de setupUI...");
                        setupUI();
                        System.out.println("setupUI terminé.");

                        System.out.println("Appel de showForm('dashboard')...");
                        showForm("dashboard");
                        System.out.println("showForm('dashboard') terminé.");

                        System.out.println("Rendre HomeScreen visible...");
                        setVisible(true);
                        System.out.println("HomeScreen rendu visible.");

                        // Réactiver HomeScreen pour permettre les interactions
                        System.out.println("Réactivation de HomeScreen...");
                        setEnabled(true);

                        System.out.println("Fermeture du LoadingDialog...");
                        loadingDialog.dispose();
                        System.out.println("LoadingDialog fermé.");
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la mise à jour de l'UI : " + e.getMessage());
                        e.printStackTrace();
                        loadingDialog.dispose();
                        setEnabled(true); // Réactiver même en cas d'erreur
                        JOptionPane.showMessageDialog(this, "Erreur lors de l'affichage de l'interface : " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des données : " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    setEnabled(true); // Réactiver même en cas d'erreur
                    JOptionPane.showMessageDialog(this, "Erreur lors du chargement des données : " + e.getMessage());
                });
            }
        }).start();
    }

    public class ProjectData {

        private String projectName = "";
        private String description = "";
        private String type = "";
        private String durationUnit = "";
        private int durationValue = 0;
        private Date startDate = null;
        private Date endDate = null;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName != null ? projectName : "";
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description != null ? description : "";
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type != null ? type : "";
        }

        public String getDurationUnit() {
            return durationUnit;
        }

        public void setDurationUnit(String durationUnit) {
            this.durationUnit = durationUnit != null ? durationUnit : "";
        }

        public int getDurationValue() {
            return durationValue;
        }

        public void setDurationValue(int durationValue) {
            this.durationValue = durationValue;
        }

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }

        public void reset() {
            this.projectName = "";
            this.description = "";
            this.type = "";
            this.durationUnit = "";
            this.durationValue = 0;
            this.startDate = null;
            this.endDate = null;
        }

        @Override
        public String toString() {
            return "ProjectData{"
                    + "projectName='" + projectName + '\''
                    + ", description='" + description + '\''
                    + ", type='" + type + '\''
                    + ", durationUnit='" + durationUnit + '\''
                    + ", durationValue=" + durationValue
                    + ", startDate=" + startDate
                    + ", endDate=" + endDate
                    + '}';
        }
    }

    private final boolean UNDECORATED = false; // Ajuste selon tes besoins

    private void initializeWebSocket(String uid) {
        try {
            URI uri = new URI("wss://teamworkatmini-jira.onrender.com/ws");
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("WebSocket connecté");
                    send("CONNECT\naccept-version:1.2\n\n\0");
                    send("SUBSCRIBE\nid:sub-0\ndestination:/topic/notifications/" + uid + "\n\n\0");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("Message WebSocket reçu : " + message);
                    if (message.startsWith("MESSAGE")) {
                        String body = message.substring(message.indexOf("\n\n") + 2, message.lastIndexOf("\0"));
                        System.out.println("Corps du message : " + body);
                        try {
                            JSONObject wsMessage = new JSONObject(body);
                            String type = wsMessage.getString("type");
                            if (type.equals("invitation_response")) {
                                SwingUtilities.invokeLater(() -> {
                                    Notifications.getInstance().show(
                                            Notifications.Type.INFO,
                                            Notifications.Location.TOP_CENTER,
                                            wsMessage.getString("message")
                                    );
                                    // Rafraîchir les notifications et invitations
                                    refreshNotifications();
                                    refreshPendingInvitations();
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur lors du traitement du message WebSocket : " + e.getMessage());
                        }
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
            System.err.println("Erreur WebSocket : " + e.getMessage());
        }
    }

    private void initializeFrame() {
        System.out.println("Initialisation du frame...");
        setTitle("Project Management Dashboard");
        setSize(UIScale.scale(new Dimension(1366, 768)));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        GlassPanePopup.install(this);
        System.out.println("Frame initialisé.");
    }

    private void initializeForms() {
        System.out.println("Initialisation des formulaires...");
        mainForm = new MainForm(fullName, email, this);
        dashboardForm = new FormDashboard(fullName, email, uid);
        projectsForm = new ProjectsForm(this);
        inboxForm = new FormInbox(fullName, email);
        readForm = new FormRead(fullName, email);
        collaborationsForm = new CollaborationsForm(this);
        System.out.println("Formulaires initialisés.");
    }

    private void initializeWizardComponents() {
        System.out.println("Initialisation des composants du wizard...");
        nameField = new JTextField();
        descriptionArea = new JTextArea();
        durationComboInstance = new JComboBox<>(new String[]{"Heures", "Jours", "Semaines", "Mois", "Années"});
        durationSpinnerInstance = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

        UtilDateModel startModel = new UtilDateModel();
        Properties properties = new Properties();
        properties.put("text.today", "Aujourd'hui");
        properties.put("text.month", "Mois");
        properties.put("text.year", "Année");
        JDatePanelImpl startDatePanel = new JDatePanelImpl(startModel, properties);
        startDatePickerInstance = new JDatePickerImpl(startDatePanel, new DateLabelFormatter());

        UtilDateModel endModel = new UtilDateModel();
        JDatePanelImpl endDatePanel = new JDatePanelImpl(endModel, properties);
        endDatePickerInstance = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());

        typeButtonGroup = new ButtonGroup();
        typeRadioButtons = new ArrayList<>();
        System.out.println("Composants du wizard initialisés.");
    }

    private void setupUI() {
        System.out.println("Configuration de l'UI...");
        setContentPane(mainForm);
        mainForm.setSelectedMenu(0, 0);
        System.out.println("UI configurée.");
    }

    public void showForm(String formName) {
        System.out.println("showForm appelé avec formName: " + formName);
        switch (formName.toLowerCase()) {
            case "dashboard":
                dashboardForm = new FormDashboard(fullName, email, uid);
                mainForm.showForm(dashboardForm);
                System.out.println("Affichage de FormDashboard");
                break;
            case "projects":
                projectsForm.refreshProjects();
                mainForm.showForm(projectsForm);
                System.out.println("Affichage de ProjectsForm");
                break;
            case "collaborations":
                collaborationsForm.refreshCollaborations();
                mainForm.showForm(collaborationsForm);
                System.out.println("Affichage de CollaborationsForm");
                break;
            case "inbox":
                refreshNotifications();
                refreshPendingInvitations();
                mainForm.showForm(inboxForm);
                System.out.println("Affichage de FormInbox");
                break;
            case "read":
                mainForm.showForm(readForm);
                System.out.println("Affichage de FormRead");
                break;
            default:
                System.out.println("Formulaire non reconnu: " + formName);
        }
    }

// Nouvelle méthode pour rafraîchir tous les projets
    public void refreshAllProjects() {
        projectsForm.refreshProjects();
        collaborationsForm.refreshCollaborations();
    }

    private void resetProjectData() {
        projectData.reset();
        if (nameField != null) {
            nameField.setText("");
        }
        if (descriptionArea != null) {
            descriptionArea.setText("");
        }
        if (durationComboInstance != null) {
            durationComboInstance.setSelectedIndex(0);
        }
        if (durationSpinnerInstance != null) {
            durationSpinnerInstance.setValue(1);
        }
        if (startDatePickerInstance != null) {
            ((UtilDateModel) startDatePickerInstance.getModel()).setValue(null);
        }
        if (endDatePickerInstance != null) {
            ((UtilDateModel) endDatePickerInstance.getModel()).setValue(null);
        }
        if (typeButtonGroup != null) {
            typeButtonGroup.clearSelection();
        }
    }

    private void loadNotifications() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications/" + getUid()))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray notifications = json.getJSONArray("notifications");

                for (int i = 0; i < notifications.length(); i++) {
                    JSONObject notification = notifications.getJSONObject(i);
                    String status = notification.optString("status", "unread");
                    if ("unread".equals(status)) {
                        String message = notification.getString("message");
                        Notifications.getInstance().show(
                                Notifications.Type.INFO,
                                Notifications.Location.TOP_CENTER,
                                message
                        );

                        // Marquer la notification comme lue
                        HttpRequest markReadRequest = HttpRequest.newBuilder()
                                .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications/" + notification.getString("notificationId") + "/mark-read"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        client.send(markReadRequest, HttpResponse.BodyHandlers.ofString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des notifications : " + e.getMessage());
        }
    }

    public void refreshProjects(JPanel projectsPanel) {
        System.out.println("Début de refreshProjects...");
        projectsPanel.removeAll();

        try {
            var client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + uid))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Réponse de l'API - Status: " + response.statusCode() + ", Body: " + response.body());

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray projectsArray = jsonResponse.getJSONArray("projects");
            System.out.println("Nombre de projets trouvés : " + projectsArray.length());

            // Calculer la taille nécessaire pour projectsPanel
            int cardsPerRow = 3; // Par exemple, 3 cartes par ligne (ajuste selon ton design)
            int cardWidth = 300;
            int cardHeight = 200;
            int gap = 10; // Espace entre les cartes
            int rows = (int) Math.ceil(projectsArray.length() / (double) cardsPerRow);
            int panelWidth = cardsPerRow * (cardWidth + gap) - gap;
            int panelHeight = rows * (cardHeight + gap) - gap;

            // Ajuster la taille préférée de projectsPanel
            projectsPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));

            for (int i = 0; i < projectsArray.length(); i++) {
                JSONObject project = projectsArray.getJSONObject(i);
                String projectId = project.getString("projectId");
                String projectName = project.getString("projectName");
                String description = project.optString("description", "Aucune description");
                String type = project.getString("type");
                long startDateMillis = project.optLong("startDate", 0);
                long endDateMillis = project.optLong("endDate", 0);
                int durationValue = project.optInt("durationValue", 0);
                String durationUnit = project.optString("durationUnit", "");
                long createdAt = project.optLong("createdAt", System.currentTimeMillis());

                // Normaliser les collaborateurs si présents
                if (project.has("collaborators")) {
                    JSONArray collaborators = project.getJSONArray("collaborators");
                    JSONArray normalizedCollaborators = new JSONArray();
                    for (int j = 0; j < collaborators.length(); j++) {
                        String collaborator = collaborators.getString(j);
                        String normalizedUid = normalizeUidOrEmail(collaborator);
                        normalizedCollaborators.put(normalizedUid);
                    }
                    project.put("collaborators", normalizedCollaborators);
                }

                // Vérifier si le projet est de type "équipe" et n'a pas de tâches
                if ("équipe".equalsIgnoreCase(type)) {
                    boolean hasTasks = checkIfProjectHasTasks(projectId);
                    if (!hasTasks) {
                        showNoTasksNotification(projectId, projectName);
                        if (!projectsWithoutTasks.containsKey(projectId)) {
                            projectsWithoutTasks.put(projectId, createdAt);
                            scheduleProjectDeletion(projectId, createdAt);
                        }
                    } else {
                        projectsWithoutTasks.remove(projectId);
                    }
                }

                // Créer une carte pour le projet
                JPanel card = createProjectCard(projectId, projectName, type, startDateMillis, endDateMillis, durationValue, durationUnit, null);
                projectsPanel.add(card, "grow, w 300!, h 200!");
                System.out.println("Projet ajouté : " + projectName);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des projets : " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur lors de la récupération des projets : " + e.getMessage());
        }

        projectsPanel.revalidate();
        projectsPanel.repaint();

        System.out.println("refreshProjects terminé");
    }

    public boolean checkIfProjectHasTasks(String projectId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray tasks = json.getJSONArray("tasks");
                return tasks.length() > 0;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification des tâches : " + e.getMessage());
        }
        return false;
    }

    public void showNoTasksNotification(String projectId, String projectName) {
        // Créer une notification toast plus grande
        Notifications.getInstance().show(
                Notifications.Type.WARNING,
                Notifications.Location.TOP_CENTER,
                5000, // Durée d'affichage (5 secondes)
                "Projet '" + projectName + "' sans tâches !"
                + "Ajoutez des tâches dans les 48h, sinon ce projet sera supprimé."
        );
    }

    public void scheduleProjectDeletion(String projectId, long createdAt) {
        long delay = 48 * 60 * 60 * 1000; // 48 heures en millisecondes
        long timeElapsed = System.currentTimeMillis() - createdAt;
        long timeRemaining = delay - timeElapsed;

        if (timeRemaining <= 0) {
            // Si le délai est déjà dépassé, supprimer immédiatement
            deleteProject(projectId);
        } else {
            // Planifier la suppression
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    // Vérifier une dernière fois si des tâches ont été ajoutées
                    if (!checkIfProjectHasTasks(projectId)) {
                        deleteProject(projectId);
                        projectsWithoutTasks.remove(projectId);
                        // Rafraîchir l'affichage
                        SwingUtilities.invokeLater(() -> {
                            refreshProjects(projectsForm.getProjectsPanel());
                        });
                    }
                }
            }, timeRemaining);
        }
    }

    private void deleteProject(String projectId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + projectId))
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Projet " + projectId + " supprimé avec succès.");
                Notifications.getInstance().show(
                        Notifications.Type.INFO,
                        Notifications.Location.TOP_CENTER,
                        "Le projet sans tâches a été supprimé automatiquement."
                );
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du projet : " + e.getMessage());
        }
    }

    public void showProjectCreationWizard() {
        JDialog wizardDialog = new JDialog(this, "Create New Project", true);
        wizardDialog.setSize(650, 650);
        wizardDialog.setLocationRelativeTo(this);
        wizardDialog.setLayout(new BorderLayout());
        wizardDialog.getContentPane().setBackground(new Color(50, 50, 60));

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        JPanel step1 = createStep1Panel(wizardDialog, cardLayout, cardPanel);
        JPanel step2 = createStep2Panel(wizardDialog, cardLayout, cardPanel);
        JPanel step3 = createStep3Panel(wizardDialog, cardLayout, cardPanel);
        JPanel step4 = createConfirmationPanel(wizardDialog, cardPanel, cardLayout);

        cardPanel.add(step1, "step1");
        cardPanel.add(step2, "step2");
        cardPanel.add(step3, "step3");
        cardPanel.add(step4, "step4");

        JPanel progressPanel = createProgressIndicator();
        wizardDialog.add(progressPanel, BorderLayout.NORTH);
        wizardDialog.add(cardPanel, BorderLayout.CENTER);

        cardLayout.show(cardPanel, "step1");
        updateProgressIndicator(progressPanel, 1);
        wizardDialog.setVisible(true);
    }

    private JPanel createProgressIndicator() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        panel.setOpaque(false);

        for (int i = 1; i <= 4; i++) {
            ImageIcon icon = loadIcon("step" + i + ".png", 24, 24);
            JLabel stepLabel = new JLabel(String.valueOf(i));
            stepLabel.setIcon(icon);
            stepLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            stepLabel.setForeground(new Color(180, 180, 200));
            stepLabel.setHorizontalTextPosition(JLabel.CENTER);
            stepLabel.setVerticalTextPosition(JLabel.BOTTOM);
            stepLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            panel.add(stepLabel);
        }

        return panel;
    }

    private void updateProgressIndicator(JPanel progressPanel, int activeStep) {
        Component[] steps = progressPanel.getComponents();
        for (int i = 0; i < steps.length; i++) {
            JLabel step = (JLabel) steps[i];
            if (i < activeStep - 1) {
                step.setForeground(new Color(100, 200, 100));
                step.setIcon(loadIcon("step" + (i + 1) + "_complete.png", 24, 24));
            } else if (i == activeStep - 1) {
                step.setForeground(new Color(70, 130, 180));
                step.setIcon(loadIcon("step" + (i + 1) + "_active.png", 24, 24));
            } else {
                step.setForeground(new Color(180, 180, 200));
                step.setIcon(loadIcon("step" + (i + 1) + ".png", 24, 24));
            }
        }
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        try {
            java.net.URL imgURL = getClass().getResource("/icons/" + path);
            if (imgURL != null) {
                Image image = ImageIO.read(imgURL);
                if (image != null) {
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImage);
                } else {
                    System.err.println("L'image est null pour le chemin : " + path);
                }
            } else {
                System.err.println("Icône non trouvée : " + path);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'icône : " + path + " - " + e.getMessage());
        }
        // Retourner une icône vide au lieu de provoquer une erreur
        return new ImageIcon();
    }

    private JPanel createStep1Panel(JDialog parent, CardLayout cardLayout, JPanel cardPanel) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel icon = new JLabel(loadIcon("project_info.png", 32, 32));
        JLabel title = new JLabel("Informations du Projet");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(10, 0)));
        header.add(title);
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel namePanel = createModernTextField("Nom du Projet", "Entrez le nom du projet");
        nameField = (JTextField) namePanel.getComponent(2);
        panel.add(namePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel descriptionPanel = createModernTextArea("Description", "Entrez la description du projet");
        descriptionArea = (JTextArea) ((JScrollPane) descriptionPanel.getComponent(2)).getViewport().getView();
        panel.add(descriptionPanel);

        JButton nextButton = new JButton("Suivant");
        styleWizardButton(nextButton);
        nextButton.addActionListener(e -> {
            String enteredName = nameField.getText().trim();
            String enteredDescription = descriptionArea.getText().trim();
            if (enteredName.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Veuillez entrer un nom de projet.");
                return;
            }
            projectData.setProjectName(enteredName);
            projectData.setDescription(enteredDescription);
            System.out.println("Step 1: " + projectData);
            cardLayout.show(cardPanel, "step2");
            updateProgressIndicator((JPanel) parent.getContentPane().getComponent(0), 2);
        });

        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(nextButton);

        return panel;
    }

    private JPanel createModernTextField(String label, String placeholder) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(new Color(200, 200, 220));
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(Color.WHITE);
        field.setOpaque(false);
        field.setBorder(new ModernBorder());
        field.putClientProperty("JTextField.placeholderText", placeholder);

        JPanel underline = new JPanel();
        underline.setPreferredSize(new Dimension(0, 2));
        underline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        underline.setBackground(new Color(100, 100, 120));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                animateUnderline(underline, new Color(70, 130, 180));
            }

            @Override
            public void focusLost(FocusEvent e) {
                animateUnderline(underline, new Color(100, 100, 120));
            }
        });

        panel.add(field);
        panel.add(underline);

        return panel;
    }

    private JPanel createModernTextArea(String label, String placeholder) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(new Color(200, 200, 220));
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JTextArea area = new JTextArea(3, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setForeground(Color.WHITE);
        area.setOpaque(false);
        area.setBorder(new ModernBorder());
        area.putClientProperty("JTextArea.placeholderText", placeholder);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel underline = new JPanel();
        underline.setPreferredSize(new Dimension(0, 2));
        underline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        underline.setBackground(new Color(100, 100, 120));

        area.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                animateUnderline(underline, new Color(70, 130, 180));
            }

            @Override
            public void focusLost(FocusEvent e) {
                animateUnderline(underline, new Color(100, 100, 120));
            }
        });

        panel.add(scroll);
        panel.add(underline);

        return panel;
    }

    private void animateUnderline(JPanel underline, Color targetColor) {
        Timer timer = new Timer(20, new ActionListener() {
            float[] current = underline.getBackground().getRGBColorComponents(null);
            float[] target = targetColor.getRGBColorComponents(null);
            int steps = 10;
            int step = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (step >= steps) {
                    ((Timer) e.getSource()).stop();
                    return;
                }

                float r = current[0] + (target[0] - current[0]) * step / steps;
                float g = current[1] + (target[1] - current[1]) * step / steps;
                float b = current[2] + (target[2] - current[2]) * step / steps;

                underline.setBackground(new Color(r, g, b));
                underline.repaint();
                step++;
            }
        });
        timer.start();
    }

    private JPanel createStep2Panel(JDialog parent, CardLayout cardLayout, JPanel cardPanel) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel icon = new JLabel(loadIcon("project_type.png", 32, 32));
        JLabel title = new JLabel("Type de Projet");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(10, 0)));
        header.add(title);
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        typeButtonGroup = new ButtonGroup();
        typeRadioButtons.clear();
        String[] types = {"Personnel", "Équipe", "Client", "Académique", "Open Source"};
        for (String type : types) {
            JPanel radioPanel = createModernRadioButton(type, typeButtonGroup);
            JRadioButton radio = (JRadioButton) radioPanel.getComponent(0);
            typeRadioButtons.add(radio);
            radioPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(radioPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.X_AXIS));
        navPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backButton = new JButton("Retour");
        styleWizardButton(backButton);
        backButton.addActionListener(e -> {
            cardLayout.show(cardPanel, "step1");
            updateProgressIndicator((JPanel) parent.getContentPane().getComponent(0), 1);
        });

        JButton nextButton = new JButton("Suivant");
        styleWizardButton(nextButton);
        nextButton.addActionListener(e -> {
            String selectedType = null;
            for (int i = 0; i < typeRadioButtons.size(); i++) {
                if (typeRadioButtons.get(i).isSelected()) {
                    selectedType = types[i];
                    break;
                }
            }
            if (selectedType == null) {
                JOptionPane.showMessageDialog(parent, "Veuillez sélectionner un type de projet.");
                return;
            }
            projectData.setType(selectedType);
            System.out.println("Step 2: " + projectData);
            cardLayout.show(cardPanel, "step3");
            updateProgressIndicator((JPanel) parent.getContentPane().getComponent(0), 3);
        });

        navPanel.add(backButton);
        navPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        navPanel.add(nextButton);

        panel.add(Box.createVerticalGlue());
        panel.add(navPanel);

        return panel;
    }

    private JPanel createModernRadioButton(String text, ButtonGroup group) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JRadioButton radio = new JRadioButton();
        radio.setOpaque(false);
        radio.setFocusPainted(false);
        radio.setIcon(loadIcon("radio_unchecked.png", 24, 24));
        radio.setSelectedIcon(loadIcon("radio_checked.png", 24, 24));
        group.add(radio);

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(Color.WHITE);

        panel.add(radio, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(new Color(180, 200, 255));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Color.WHITE);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                radio.setSelected(true);
            }
        });

        return panel;
    }

    private void calculateProjectDates() {
        if (isUpdatingDates) {
            return; // Éviter les appels récursifs
        }

        isUpdatingDates = true;
        try {
            String selectedDurationUnit = (String) durationComboInstance.getSelectedItem();
            int selectedDurationValue = (int) durationSpinnerInstance.getValue();
            Date startDate = projectData.getStartDate();
            Date endDate = projectData.getEndDate();

            // Si l'utilisateur a entré une date de début et une durée, calculer la date de fin
            if (startDate != null && selectedDurationValue > 0 && selectedDurationUnit != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);

                switch (selectedDurationUnit) {
                    case "Heures":
                        calendar.add(Calendar.HOUR_OF_DAY, selectedDurationValue);
                        break;
                    case "Jours":
                        calendar.add(Calendar.DAY_OF_MONTH, selectedDurationValue);
                        break;
                    case "Semaines":
                        calendar.add(Calendar.WEEK_OF_YEAR, selectedDurationValue);
                        break;
                    case "Mois":
                        calendar.add(Calendar.MONTH, selectedDurationValue);
                        break;
                    case "Années":
                        calendar.add(Calendar.YEAR, selectedDurationValue);
                        break;
                }

                Date calculatedEndDate = calendar.getTime();
                projectData.setEndDate(calculatedEndDate);
                // Mettre à jour le modèle du date picker
                UtilDateModel endModel = (UtilDateModel) endDatePickerInstance.getModel();
                endModel.setValue(null); // Réinitialiser pour éviter des problèmes d'affichage
                endModel.setValue(calculatedEndDate);
                endModel.setSelected(true); // Forcer la sélection
                System.out.println("Date de fin calculée et mise à jour : " + calculatedEndDate);
            } // Si l'utilisateur a entré une date de fin et une durée, calculer la date de début
            else if (endDate != null && selectedDurationValue > 0 && selectedDurationUnit != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDate);

                switch (selectedDurationUnit) {
                    case "Heures":
                        calendar.add(Calendar.HOUR_OF_DAY, -selectedDurationValue);
                        break;
                    case "Jours":
                        calendar.add(Calendar.DAY_OF_MONTH, -selectedDurationValue);
                        break;
                    case "Semaines":
                        calendar.add(Calendar.WEEK_OF_YEAR, -selectedDurationValue);
                        break;
                    case "Mois":
                        calendar.add(Calendar.MONTH, -selectedDurationValue);
                        break;
                    case "Années":
                        calendar.add(Calendar.YEAR, -selectedDurationValue);
                        break;
                }

                Date calculatedStartDate = calendar.getTime();
                projectData.setStartDate(calculatedStartDate);
                // Mettre à jour le modèle du date picker
                UtilDateModel startModel = (UtilDateModel) startDatePickerInstance.getModel();
                startModel.setValue(null); // Réinitialiser pour éviter des problèmes d'affichage
                startModel.setValue(calculatedStartDate);
                startModel.setSelected(true); // Forcer la sélection
                System.out.println("Date de début calculée et mise à jour : " + calculatedStartDate);
            }
        } finally {
            isUpdatingDates = false;
        }
    }

    private JPanel createStep3Panel(JDialog parent, CardLayout cardLayout, JPanel cardPanel) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel icon = new JLabel(loadIcon("calendar.png", 32, 32));
        JLabel title = new JLabel("Chronologie du Projet");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(10, 0)));
        header.add(title);
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        JLabel durationSectionLabel = new JLabel("Durée du Projet", SwingConstants.CENTER);
        durationSectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        durationSectionLabel.setForeground(new Color(200, 200, 220));
        durationSectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(durationSectionLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel durationPanel = new JPanel();
        durationPanel.setOpaque(false);
        durationPanel.setLayout(new BoxLayout(durationPanel, BoxLayout.X_AXIS));
        durationPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        durationComboInstance.setMaximumSize(new Dimension(150, 40));
        durationComboInstance.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        durationComboInstance.setForeground(Color.WHITE);
        durationComboInstance.setBackground(new Color(60, 60, 70));
        durationComboInstance.setBorder(new ModernBorder());
        durationComboInstance.setRenderer(new ModernComboRenderer());
        durationComboInstance.setUI(new ModernComboUI());

        durationSpinnerInstance.setMaximumSize(new Dimension(100, 40));
        durationSpinnerInstance.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        durationSpinnerInstance.setBorder(new ModernBorder());

        JLabel durationDisplay = new JLabel("1 Heures");
        durationDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        durationDisplay.setForeground(Color.WHITE);
        durationDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);

        durationSpinnerInstance.addChangeListener(e -> {
            durationDisplay.setText(durationSpinnerInstance.getValue() + " " + durationComboInstance.getSelectedItem());
            calculateProjectDates();
        });
        durationComboInstance.addActionListener(e -> {
            durationDisplay.setText(durationSpinnerInstance.getValue() + " " + durationComboInstance.getSelectedItem());
            calculateProjectDates();
        });

        durationPanel.add(durationComboInstance);
        durationPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        durationPanel.add(durationSpinnerInstance);

        panel.add(durationPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(durationDisplay);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel orLabel = new JLabel("- OU -", SwingConstants.CENTER);
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        orLabel.setForeground(new Color(180, 180, 200));
        orLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(orLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel datesSectionLabel = new JLabel("Dates du Projet", SwingConstants.CENTER);
        datesSectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        datesSectionLabel.setForeground(new Color(200, 200, 220));
        datesSectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(datesSectionLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel startDatePanel = createModernDatePicker("Date de Début");
        panel.add(startDatePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel endDatePanel = createModernDatePicker("Date de Fin");
        panel.add(endDatePanel);

// Ajouter des listeners pour recalculer la durée si les dates sont modifiées
// Ajouter des listeners pour recalculer la durée si les dates sont modifiées
        startDatePickerInstance.getModel().addChangeListener(e -> {
            UtilDateModel model = (UtilDateModel) startDatePickerInstance.getModel();
            Date selectedStartDate = model.getValue() != null ? model.getValue() : null;
            projectData.setStartDate(selectedStartDate);
            calculateProjectDates();
        });

        endDatePickerInstance.getModel().addChangeListener(e -> {
            UtilDateModel model = (UtilDateModel) endDatePickerInstance.getModel();
            Date selectedEndDate = model.getValue() != null ? model.getValue() : null;
            projectData.setEndDate(selectedEndDate);
            calculateProjectDates();
        });

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.X_AXIS));
        navPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backButton = new JButton("Retour");
        styleWizardButton(backButton);
        backButton.addActionListener(e -> {
            cardLayout.show(cardPanel, "step2");
            updateProgressIndicator((JPanel) parent.getContentPane().getComponent(0), 2);
        });

        JButton nextButton = new JButton("Suivant");
        styleWizardButton(nextButton);
        nextButton.addActionListener(e -> {
            String selectedDurationUnit = (String) durationComboInstance.getSelectedItem();
            int selectedDurationValue = (int) durationSpinnerInstance.getValue();
            Object startDateValue = startDatePickerInstance.getModel().getValue();
            Object endDateValue = endDatePickerInstance.getModel().getValue();

            Date selectedStartDate = null;
            Date selectedEndDate = null;

            if (startDateValue instanceof Date) {
                selectedStartDate = (Date) startDateValue;
            } else if (startDateValue instanceof Calendar) {
                selectedStartDate = ((Calendar) startDateValue).getTime();
            }

            if (endDateValue instanceof Date) {
                selectedEndDate = (Date) endDateValue;
            } else if (endDateValue instanceof Calendar) {
                selectedEndDate = ((Calendar) endDateValue).getTime();
            }

            // Validation robuste
            if (selectedStartDate == null && selectedEndDate == null && selectedDurationValue <= 0) {
                JOptionPane.showMessageDialog(parent, "Veuillez spécifier une durée ou des dates pour le projet.");
                return;
            }

            if (selectedStartDate != null && selectedEndDate != null) {
                if (selectedStartDate.after(selectedEndDate)) {
                    JOptionPane.showMessageDialog(parent, "La date de fin doit être après la date de début.");
                    return;
                }

                // Vérifier la cohérence entre la durée et les dates
                long diffInMillies = selectedEndDate.getTime() - selectedStartDate.getTime();
                long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);

                long expectedDays = 0;
                switch (selectedDurationUnit) {
                    case "Heures":
                        expectedDays = selectedDurationValue / 24;
                        break;
                    case "Jours":
                        expectedDays = selectedDurationValue;
                        break;
                    case "Semaines":
                        expectedDays = selectedDurationValue * 7;
                        break;
                    case "Mois":
                        expectedDays = selectedDurationValue * 30; // Approximation
                        break;
                    case "Années":
                        expectedDays = selectedDurationValue * 365;
                        break;
                }

                // Tolérance de 10% pour la différence
                long tolerance = expectedDays / 10;
                if (Math.abs(diffInDays - expectedDays) > tolerance && expectedDays > 0) {
                    int response = JOptionPane.showConfirmDialog(parent,
                            "La durée spécifiée (" + selectedDurationValue + " " + selectedDurationUnit + ") "
                            + "ne correspond pas à la différence entre les dates (" + diffInDays + " jours). "
                            + "Voulez-vous ajuster les dates automatiquement ?",
                            "Incohérence détectée",
                            JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        calculateProjectDates();
                        selectedEndDate = projectData.getEndDate();
                        selectedStartDate = projectData.getStartDate();
                    } else {
                        return;
                    }
                }
            } else if (selectedStartDate == null && selectedEndDate == null) {
                // Si aucune date n'est spécifiée, définir la date de début comme aujourd'hui
                selectedStartDate = new Date();
                projectData.setStartDate(selectedStartDate);
                ((UtilDateModel) startDatePickerInstance.getModel()).setValue(selectedStartDate);
                calculateProjectDates();
                selectedEndDate = projectData.getEndDate();
            }

            projectData.setDurationUnit(selectedDurationUnit);
            projectData.setDurationValue(selectedDurationValue);
            projectData.setStartDate(selectedStartDate);
            projectData.setEndDate(selectedEndDate);
            System.out.println("Step 3: " + projectData);
            cardLayout.show(cardPanel, "step4");
            updateProgressIndicator((JPanel) parent.getContentPane().getComponent(0), 4);
            updateConfirmationPanel();
        });

        navPanel.add(backButton);
        navPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        navPanel.add(nextButton);

        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(navPanel);

        return panel;
    }

    private JPanel createModernDatePicker(String label) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(new Color(200, 200, 220));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JDatePickerImpl datePicker;
        if (label.equals("Date de Début")) {
            datePicker = startDatePickerInstance;
        } else {
            datePicker = endDatePickerInstance;
        }

        // Augmenter la hauteur
        datePicker.setMaximumSize(new Dimension(200, 50)); // Hauteur augmentée de 40 à 50
        datePicker.getJFormattedTextField().setFont(new Font("Segoe UI", Font.PLAIN, 14));
        datePicker.getJFormattedTextField().setForeground(Color.WHITE);
        datePicker.getJFormattedTextField().setBackground(new Color(60, 60, 70));
        datePicker.getJFormattedTextField().setBorder(new ModernBorder());
        datePicker.getJFormattedTextField().setPreferredSize(new Dimension(200, 50)); // Forcer la hauteur

        panel.add(datePicker);

        return panel;
    }

    class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {

        private String datePattern = "dd/MM/yyyy";
        private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return dateFormatter.parseObject(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            }
            if (value instanceof Date) {
                return dateFormatter.format((Date) value);
            }
            if (value instanceof Calendar) {
                return dateFormatter.format(((Calendar) value).getTime());
            }
            throw new ParseException("Type non supporté: " + value.getClass(), 0);
        }
    }

    private JPanel createConfirmationPanel(JDialog parent, JPanel cardPanel, CardLayout cardLayout) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel icon = new JLabel(loadIcon("confirmation.png", 32, 32));
        JLabel title = new JLabel("Confirmation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(10, 0)));
        header.add(title);
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        summaryCard = new JPanel();
        summaryCard.setOpaque(false);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setBorder(new ModernBorder());
        updateConfirmationPanel();
        panel.add(summaryCard);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        System.out.println("Confirmation Panel: " + projectData);

        String name = projectData.getProjectName().isEmpty() ? "Non spécifié" : projectData.getProjectName();
        String description = projectData.getDescription().isEmpty() ? "Non spécifiée" : projectData.getDescription();
        String type = projectData.getType().isEmpty() ? "Non spécifié" : projectData.getType();
        String durationText = (projectData.getDurationValue() > 0 && !projectData.getDurationUnit().isEmpty())
                ? "Durée: " + projectData.getDurationValue() + " " + projectData.getDurationUnit()
                : "Durée: Non spécifiée";
        String startDateText = (projectData.getStartDate() != null)
                ? "Date de Début: " + dateFormat.format(projectData.getStartDate())
                : "Date de Début: Non spécifiée";
        String endDateText = (projectData.getEndDate() != null)
                ? "Date de Fin: " + dateFormat.format(projectData.getEndDate())
                : "Date de Fin: Non spécifiée";

        String[] details = {
            "Nom du Projet: " + name,
            "Description: " + description,
            "Type: " + type,
            durationText,
            startDateText,
            endDateText
        };

        for (String detail : details) {
            JLabel label = new JLabel(detail);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setForeground(Color.WHITE);
            label.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            summaryCard.add(label);
        }

        panel.add(summaryCard);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.X_AXIS));
        navPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backButton = new JButton("Retour");
        styleWizardButton(backButton);
        backButton.addActionListener(e -> {
            cardLayout.show(cardPanel, "step3");
            updateProgressIndicator((JPanel) parent.getContentPane().getComponent(0), 3);
        });

        JButton createButton = new JButton("Créer le Projet");
        styleWizardButton(createButton);
        createButton.setBackground(new Color(70, 160, 70));
        createButton.addActionListener(e -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                JSONObject projectDataJson = new JSONObject();
                projectDataJson.put("projectName", projectData.getProjectName());
                projectDataJson.put("description", projectData.getDescription());
                projectDataJson.put("type", projectData.getType());
                projectDataJson.put("uid", uid);
                if (projectData.getDurationValue() > 0 && !projectData.getDurationUnit().isEmpty()) {
                    projectDataJson.put("durationUnit", projectData.getDurationUnit());
                    projectDataJson.put("durationValue", projectData.getDurationValue());
                }
                if (projectData.getStartDate() != null) {
                    projectDataJson.put("startDate", projectData.getStartDate().getTime());
                }
                if (projectData.getEndDate() != null) {
                    projectDataJson.put("endDate", projectData.getEndDate().getTime());
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(projectDataJson.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    parent.dispose();
                    showSuccessAnimation();
                    resetProjectData();
                    refreshProjects(projectsForm.getProjectsPanel()); // Rafraîchir les projets dans ProjectsForm
                } else {
                    JOptionPane.showMessageDialog(parent, "Erreur lors de la création du projet: " + response.body());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Erreur de connexion au serveur: " + ex.getMessage());
            }
        });

        navPanel.add(backButton);
        navPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        navPanel.add(createButton);

        panel.add(navPanel);

        return panel;
    }

    private void updateConfirmationPanel() {
        summaryCard.removeAll();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        String name = projectData.getProjectName().isEmpty() ? "Non spécifié" : projectData.getProjectName();
        String description = projectData.getDescription().isEmpty() ? "Non spécifiée" : projectData.getDescription();
        String type = projectData.getType().isEmpty() ? "Non spécifié" : projectData.getType();
        String durationText = (projectData.getDurationValue() > 0 && !projectData.getDurationUnit().isEmpty())
                ? "Durée: " + projectData.getDurationValue() + " " + projectData.getDurationUnit()
                : "Durée: Non spécifiée";
        String startDateText = (projectData.getStartDate() != null)
                ? "Date de Début: " + dateFormat.format(projectData.getStartDate())
                : "Date de Début: Non spécifiée";
        String endDateText = (projectData.getEndDate() != null)
                ? "Date de Fin: " + dateFormat.format(projectData.getEndDate())
                : "Date de Fin: Non spécifiée";

        String[] details = {name, description, type, durationText, startDateText, endDateText};

        for (String detail : details) {
            JLabel label = new JLabel(detail);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setForeground(Color.WHITE);
            label.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            summaryCard.add(label);
        }

        summaryCard.revalidate();
        summaryCard.repaint();
    }

    private void showSuccessAnimation() {
        JDialog successDialog = new JDialog(this, "Succès", true);
        successDialog.setSize(300, 300);
        successDialog.setLocationRelativeTo(this);
        successDialog.setLayout(new BorderLayout());
        successDialog.getContentPane().setBackground(new Color(50, 50, 60));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));

        JLabel checkIcon = new JLabel(loadIcon("success.png", 80, 80));
        checkIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(checkIcon);

        JLabel message = new JLabel("Projet Créé avec Succès !");
        message.setFont(new Font("Segoe UI", Font.BOLD, 18));
        message.setForeground(Color.WHITE);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createRigidArea(new Dimension(0, 20)));
        content.add(message);

        JLabel infoMessage = new JLabel("N'oubliez pas d'ajouter des tâches pour éviter la suppression automatique après 48h.");
        infoMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoMessage.setForeground(new Color(200, 200, 255));
        infoMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(infoMessage);

        successDialog.add(content, BorderLayout.CENTER);

        Timer timer = new Timer(3000, e -> successDialog.dispose());
        timer.setRepeats(false);
        timer.start();

        successDialog.setVisible(true);
    }

    private void styleWizardButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(70, 130, 180));
        btn.setBorder(new RoundedBorder(16, new Color(70, 130, 180)));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(150, 40));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(90, 150, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(70, 130, 180));
            }
        });
    }

    class ModernBorder extends AbstractBorder {

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

    class ModernComboRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBackground(new Color(60, 60, 70));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return this;
        }
    }

    class ModernComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton();
            button.setIcon(loadIcon("arrow_down.png", 16, 16));
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder());
            return button;
        }
    }

    public void logout() {
        mainForm.closeWebSocket(); // Fermer le WebSocket lors de la déconnexion
        System.out.println("Début de la déconnexion...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/logout"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Déconnexion - Réponse du serveur : " + response.statusCode() + " - " + response.body());
        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion : " + e.getMessage());
        }

        System.out.println("Fermeture de HomeScreen...");
        this.dispose();

        // Arrêter le serveur NanoHTTPD
        try {
            AuthServer.getInstance().stopServer();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du serveur NanoHTTPD : " + e.getMessage());
        }

        System.out.println("Redirection vers LoginView...");
        SwingUtilities.invokeLater(() -> {
            System.out.println("Création de LoginView...");
            LoginView loginView = new LoginView();
            JFrame frame = new JFrame("Sign In");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(loginView);
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            System.out.println("LoginView affiché.");

            // Redémarrer le serveur après avoir créé LoginView
            try {
                AuthServer.getInstance();
            } catch (IOException e) {
                System.err.println("Erreur lors du redémarrage du serveur local : " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Erreur lors du redémarrage du serveur local : " + e.getMessage());
            }
        });
    }

    public JPanel createProjectCard(String projectId, String projectName, String type, long startDateMillis, long endDateMillis, int durationValue, String durationUnit, MouseAdapter customClickListener) {
        // Récupérer les détails complets du projet, y compris les collaborateurs
        JSONObject project = new JSONObject();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/project/" + projectId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                project = jsonResponse.getJSONObject("project");
            } else {
                System.err.println("Erreur lors de la récupération des détails du projet - Status: " + response.statusCode());
                // Fallback avec les données minimales
                project.put("projectId", projectId);
                project.put("projectName", projectName);
                project.put("type", type);
                project.put("startDate", startDateMillis);
                project.put("endDate", endDateMillis);
                project.put("durationValue", durationValue);
                project.put("durationUnit", durationUnit);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des détails du projet: " + e.getMessage());
            // Fallback
            project.put("projectId", projectId);
            project.put("projectName", projectName);
            project.put("type", type);
            project.put("startDate", startDateMillis);
            project.put("endDate", endDateMillis);
            project.put("durationValue", durationValue);
            project.put("durationUnit", durationUnit);
        }

        final JSONObject finalProject = project;

        // Vérifier si l'utilisateur est le créateur du projet
        boolean isCreator = isProjectCreator(projectId);

        // Déterminer si le mode est clair ou sombre
        boolean isLightMode = UIManager.getLookAndFeel().getName().contains("Light");

        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new RoundedBorder(20, isLightMode ? new Color(50, 50, 50, 100) : new Color(255, 255, 255, 30)));
        card.setPreferredSize(new Dimension(300, 200));
        card.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:darken(@background,5%);");

        // Contenu principal de la carte
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));

        // Titre du projet
        JLabel titleLabel = new JLabel(projectName);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(isLightMode ? Color.BLACK : Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        mainContent.add(titleLabel);

        // Barre de progression
        JProgressBar progressBar = new JProgressBar();
        int progress = calculateProgress(projectId);
        progressBar.setValue(progress);
        progressBar.setForeground(new Color(100, 200, 100));
        progressBar.setBackground(isLightMode ? new Color(200, 200, 200) : new Color(255, 255, 255, 50));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        progressBar.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        mainContent.add(progressBar);

        // Informations sur l'échéance
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String dueText;
        if (startDateMillis != 0 && endDateMillis != 0) {
            Date endDate = new Date(endDateMillis);
            long daysRemaining = (endDateMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            dueText = "Échéance: " + dateFormat.format(endDate) + " (" + daysRemaining + " jours restants)";
        } else if (durationUnit != null && !durationUnit.isEmpty()) {
            dueText = "Durée: " + durationValue + " " + durationUnit;
        } else {
            dueText = "Échéance: Non spécifiée";
        }
        JLabel dueLabel = new JLabel(dueText);
        dueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dueLabel.setForeground(isLightMode ? new Color(50, 50, 100) : new Color(200, 200, 255));
        dueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dueLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        mainContent.add(dueLabel);

        // Panneau des membres (pour les projets d'équipe)
        if ("équipe".equalsIgnoreCase(type)) {
            mainContent.add(createTeamMembersPanel());
        }

        card.add(mainContent, BorderLayout.CENTER);

        // Panneau des icônes d'actions (afficher uniquement si l'utilisateur est le créateur)
        if (isCreator) {
            JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            iconsPanel.setOpaque(false);

            JButton addTasksBtn = new JButton(loadIcon("add_task.png", 28, 28));
            addTasksBtn.setContentAreaFilled(false);
            addTasksBtn.setBorder(BorderFactory.createEmptyBorder());
            addTasksBtn.addActionListener(e -> {
                showAddTaskForm(finalProject);
            });
            iconsPanel.add(addTasksBtn);

            if ("équipe".equalsIgnoreCase(type)) {
                JButton settingsBtn = new JButton(loadIcon("settings.png", 28, 28));
                settingsBtn.setContentAreaFilled(false);
                settingsBtn.setBorder(BorderFactory.createEmptyBorder());
                settingsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Paramètres du projet: " + projectName));
                iconsPanel.add(settingsBtn);

                JButton addMembersBtn = new JButton(loadIcon("add_member.png", 28, 28));
                addMembersBtn.setContentAreaFilled(false);
                addMembersBtn.setBorder(BorderFactory.createEmptyBorder());
                addMembersBtn.addActionListener(e -> {
                    AddCollaboratorForm form = new AddCollaboratorForm(this, finalProject);
                    form.setVisible(true);
                });
                iconsPanel.add(addMembersBtn);
            }

            // Ajouter l'icône de suppression
            JButton deleteBtn = new JButton(loadIcon("delete.png", 28, 28));
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setBorder(BorderFactory.createEmptyBorder());
            deleteBtn.addActionListener(e -> {
                showDeleteConfirmationDialog(projectId, projectName);
            });
            iconsPanel.add(deleteBtn);

            card.add(iconsPanel, BorderLayout.NORTH);
        }

        // Ajouter des animations et transitions
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new RoundedBorder(20, isLightMode ? new Color(50, 50, 50, 150) : new Color(255, 255, 255, 100)));
                card.putClientProperty(FlatClientProperties.STYLE, ""
                        + "background:darken(@background,3%);");
                animateCard(card, true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new RoundedBorder(20, isLightMode ? new Color(50, 50, 50, 100) : new Color(255, 255, 255, 30)));
                card.putClientProperty(FlatClientProperties.STYLE, ""
                        + "background:darken(@background,5%);");
                animateCard(card, false);
            }
        });

        // Ajouter le MouseListener personnalisé s'il est fourni
        if (customClickListener != null) {
            card.addMouseListener(customClickListener);
        } else {
            // Comportement par défaut pour le créateur
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showProjectDetails(finalProject);
                }
            });
        }

        return card;
    }
    
    private void showDeleteConfirmationDialog(String projectId, String projectName) {
    JDialog confirmationDialog = new JDialog(this, "Confirmer la Suppression", true);
    confirmationDialog.setSize(400, 250);
    confirmationDialog.setLocationRelativeTo(this);
    confirmationDialog.setLayout(new BorderLayout());
    confirmationDialog.getContentPane().setBackground(new Color(50, 50, 60));

    JPanel contentPanel = new JPanel();
    contentPanel.setOpaque(false);
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    // Icône d'avertissement
    JLabel warningIcon = new JLabel(loadIcon("warning.png", 48, 48));
    warningIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
    contentPanel.add(warningIcon);

    // Message de confirmation
    JLabel messageLabel = new JLabel("<html><center>Voulez-vous vraiment supprimer le projet<br>\"" + projectName + "\" ?</center></html>");
    messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
    messageLabel.setForeground(Color.WHITE);
    messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    contentPanel.add(messageLabel);

    // Note d'avertissement
    JLabel noteLabel = new JLabel("Cette action est irréversible.");
    noteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    noteLabel.setForeground(new Color(255, 150, 150));
    noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    contentPanel.add(noteLabel);

    // Panneau des boutons
    JPanel buttonPanel = new JPanel();
    buttonPanel.setOpaque(false);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

    JButton cancelButton = new JButton("Annuler");
    styleWizardButton(cancelButton);
    cancelButton.setBackground(new Color(100, 100, 120));
    cancelButton.addActionListener(e -> confirmationDialog.dispose());
    buttonPanel.add(cancelButton);

    JButton confirmButton = new JButton("Supprimer");
    styleWizardButton(confirmButton);
    confirmButton.setBackground(new Color(200, 80, 80));
    confirmButton.addActionListener(e -> {
        confirmationDialog.dispose();
        // Vérifier si le projet peut être supprimé
        checkAndDeleteProject(projectId, projectName);
    });
    buttonPanel.add(confirmButton);

    confirmationDialog.add(contentPanel, BorderLayout.CENTER);
    confirmationDialog.add(buttonPanel, BorderLayout.SOUTH);

    confirmationDialog.setVisible(true);
}
    
    private void checkAndDeleteProject(String projectId, String projectName) {
    try {
        HttpClient client = HttpClient.newHttpClient();

        // Étape 1 : Vérifier si le projet peut être supprimé
        HttpRequest canDeleteRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + projectId + "/can-delete"))
                .GET()
                .build();
        HttpResponse<String> canDeleteResponse = client.send(canDeleteRequest, HttpResponse.BodyHandlers.ofString());

        if (canDeleteResponse.statusCode() != 200) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la vérification du projet : " + canDeleteResponse.body());
            return;
        }

        JSONObject canDeleteJson = new JSONObject(canDeleteResponse.body());
        boolean canDelete = canDeleteJson.getBoolean("canDelete");

        if (!canDelete) {
            String reason = canDeleteJson.optString("reason", "Le projet ne peut pas être supprimé.");
            JOptionPane.showMessageDialog(this, reason);
            return;
        }

        // Étape 2 : Supprimer le projet
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/" + projectId))
                .DELETE()
                .build();
        HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        if (deleteResponse.statusCode() == 200) {
            Notifications.getInstance().show(
                    Notifications.Type.SUCCESS,
                    Notifications.Location.TOP_CENTER,
                    "Projet \"" + projectName + "\" supprimé avec succès."
            );
            // Rafraîchir l'affichage des projets
            refreshProjects(projectsForm.getProjectsPanel());
        } else {
            JOptionPane.showMessageDialog(this, "Erreur lors de la suppression du projet : " + deleteResponse.body());
        }
    } catch (Exception e) {
        System.err.println("Erreur lors de la suppression du projet : " + e.getMessage());
        JOptionPane.showMessageDialog(this, "Erreur lors de la suppression du projet : " + e.getMessage());
    }
}

    public void refreshProjectsAfterUpdate() {
        if (projectsForm != null) {
            refreshProjects(projectsForm.getProjectsPanel());
        }
    }

    private boolean isProjectCreator(String projectId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/projects/project/" + projectId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Réponse de /api/projects/project/" + projectId + " - Status: " + response.statusCode() + ", Body: " + response.body());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONObject projectData = jsonResponse.getJSONObject("project");
                String creatorUid = projectData.getString("uid");
                return uid.equals(creatorUid);
            } else {
                System.err.println("Erreur lors de la récupération du projet - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du créateur du projet: " + e.getMessage());
        }
        return false; // Par défaut, considérer que l'utilisateur n'est pas le créateur
    }

    private void refreshNotifications() {
        try {
            System.out.println("Début de refreshNotifications pour uid: " + uid);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications/" + uid))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Réponse de l'API /notifications - Status: " + response.statusCode() + ", Body: " + response.body());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                notifications = jsonResponse.getJSONArray("notifications"); // Stocker les notifications
                int unreadCount = 0;
                for (int i = 0; i < notifications.length(); i++) {
                    JSONObject notification = notifications.getJSONObject(i);
                    String status = notification.optString("status", "unread");
                    if ("unread".equals(status)) {
                        unreadCount++;
                    }
                }
                System.out.println("Nombre de notifications non lues: " + unreadCount);
                // Mettre à jour le badge dans le menu
                mainForm.setNotificationBadge(unreadCount);
                System.out.println("Badge mis à jour avec unreadCount: " + unreadCount);
                // Mettre à jour FormInbox
                inboxForm.updateNotifications(notifications);
                // Mettre à jour FormRead
                readForm.updateNotifications(notifications);
            } else {
                System.err.println("Erreur lors de la récupération des notifications - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des notifications: " + e.getMessage());
        }
    }

    public void setNotifications(JSONArray notifications) {
        this.notifications = notifications;
        if (inboxForm != null) {
            inboxForm.updateNotifications(notifications);
        }
        if (readForm != null) {
            readForm.updateNotifications(notifications);
        }
    }

    public void setPendingInvitations(JSONArray invitations) {
        this.pendingInvitations = invitations;
        if (inboxForm != null) {
            inboxForm.updatePendingInvitations(invitations);
        }
    }

    public void updateNotificationStatus(String notificationId, String status) {
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject notification = notifications.getJSONObject(i);
            if (notification.getString("notificationId").equals(notificationId)) {
                notification.put("status", status);
                break;
            }
        }
        inboxForm.updateNotifications(notifications);
    }

    private void refreshPendingInvitations() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/invitations/pending/" + uid))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                pendingInvitations = jsonResponse.getJSONArray("invitations"); // Stocker les invitations
                // Mettre à jour FormInbox pour les invitations en attente
                inboxForm.updatePendingInvitations(pendingInvitations);
                // Recalculer le nombre total pour le badge (notifications non lues + invitations en attente)
                int unreadCount = 0;
                for (int i = 0; i < notifications.length(); i++) {
                    JSONObject notification = notifications.getJSONObject(i);
                    String status = notification.optString("status", "unread");
                    if ("unread".equals(status)) {
                        unreadCount++;
                    }
                }
                unreadCount += pendingInvitations.length(); // Ajouter les invitations en attente
                mainForm.setNotificationBadge(unreadCount);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des invitations en attente: " + e.getMessage());
        }
    }

    private void showProjectDetails(JSONObject project) {
        ProjectDetailsForm form = new ProjectDetailsForm(this, project);
        form.setVisible(true);
    }

    private void showAddTaskForm(JSONObject project) {
        AddTaskForm form = new AddTaskForm(this, project);
        form.setVisible(true);
        refreshProjectsAfterUpdate(); // Rafraîchir les projets après l'ajout d'une tâche
    }

    private int calculateProgress(String projectId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray tasks = json.getJSONArray("tasks");

                if (tasks.length() == 0) {
                    return 0; // Pas de tâches, progression = 0%
                }

                int totalTasks = tasks.length();
                int completedTasks = 0;

                for (int i = 0; i < totalTasks; i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    String status = task.getString("status");
                    if ("terminé".equalsIgnoreCase(status)) {
                        completedTasks++;
                    }
                }

                return (int) ((completedTasks * 100.0) / totalTasks);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul de la progression : " + e.getMessage());
        }
        return 0; // En cas d'erreur, retourner 0%
    }

    private void animateCard(JPanel card, boolean isHover) {
        Timer timer = new Timer(10, null);
        int steps = 10;
        final int[] step = {0}; // Utilisation d'un tableau pour stocker 'step'
        float initialScale = isHover ? 1.0f : 1.05f;
        float targetScale = isHover ? 1.05f : 1.0f;

        timer.addActionListener(e -> {
            if (step[0] >= steps) {
                timer.stop();
                return;
            }

            float scale = initialScale + (targetScale - initialScale) * step[0] / steps;
            card.setPreferredSize(new Dimension((int) (300 * scale), (int) (200 * scale)));
            card.revalidate();
            step[0]++;
        });
        timer.start();
    }

    private JPanel createTeamMembersPanel() {
        JPanel teamPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        teamPanel.setOpaque(false);
        teamPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        teamPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        String[] emojis = {"👨", "👩", "🧑", "👨‍💻", "👩‍💻"};
        for (int i = 0; i < 3; i++) {
            JLabel member = new JLabel(emojis[(int) (Math.random() * emojis.length)]);
            member.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            teamPanel.add(member);
        }

        JLabel moreLabel = new JLabel("+" + (int) (1 + Math.random() * 5));
        moreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        moreLabel.setForeground(new Color(200, 200, 255));
        teamPanel.add(moreLabel);

        return teamPanel;
    }

    public static void main(String[] args) throws IOException {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("com.aemtechnology.gestionprojet.theme");
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacDarkLaf.setup();

        UIManager.put("Drawer.background", new Color(30, 30, 40));

        Desktop.getDesktop().setOpenURIHandler(e -> {
            URI uri = e.getURI();
            if ("gestionprojetsswing".equals(uri.getScheme())) {
                Map<String, String> params = parseQuery(uri.getQuery());

                if (params.containsKey("error")) {
                    JOptionPane.showMessageDialog(null, "Erreur d'authentification: " + params.get("error"));
                    System.exit(1);
                } else {
                    String token = params.get("token");
                    String uid = params.get("uid");
                    String email = params.get("email");
                    String fullName = params.get("fullName");

                    SwingUtilities.invokeLater(() -> {
                        HomeScreen homeScreen = new HomeScreen(uid, fullName != null ? fullName : email, email);
                        homeScreen.setVisible(true);
                    });
                }
            }
        });

        if (args.length > 0 && args[0].startsWith("gestionprojetsswing://")) {
            URI uri = URI.create(args[0]);
            Map<String, String> params = parseQuery(uri.getQuery());
        } else {
            SwingUtilities.invokeLater(() -> {
                LoginView loginDialog = new LoginView();
                loginDialog.setVisible(true);
            });
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    try {
                        result.put(pair[0], URLDecoder.decode(pair[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        // Gérer l'erreur
                    }
                }
            }
        }
        return result;
    }

    // Ajouter ces getters à la fin de la classe HomeScreen
    public String getUid() {
        return uid;
    }

    public Map<String, Long> getProjectsWithoutTasks() {
        return projectsWithoutTasks;
    }
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
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius, radius, radius, radius);
    }

}
