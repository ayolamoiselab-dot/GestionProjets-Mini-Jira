package com.aemtechnology.gestionprojet.application.form.other;

import com.formdev.flatlaf.FlatClientProperties;
import raven.toast.Notifications;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.axis.NumberAxis;
import raven.chart.bar.HorizontalBarChart;

import raven.chart.data.pie.DefaultPieDataset;
import raven.chart.pie.PieChart;
import com.aemtechnology.gestionprojet.components.SimpleForm;
import java.awt.Dimension;

public class FormDashboard extends SimpleForm {

    private final String fullName;
    private final String email;
    private String uid;

    private ChartPanel lineChartPanel;
    private HorizontalBarChart barChart1;
    private HorizontalBarChart barChart2;
    private PieChart pieChart1;
    private PieChart pieChart2;
    private PieChart pieChart3;

    private boolean hasActivity = false;
    private final HttpClient httpClient;

    public FormDashboard(String fullName, String email, String uid) {
        this.fullName = fullName != null && !fullName.trim().isEmpty() ? fullName : "Utilisateur";
        this.email = email != null && !email.trim().isEmpty() ? email : "email@example.com";
        this.uid = uid != null && !uid.trim().isEmpty() ? uid : "";
        this.httpClient = HttpClient.newHttpClient();
        init();
    }

    @Override
    public void formRefresh() {
        if (hasActivity) {
            pieChart1.startAnimation();
            pieChart2.startAnimation();
            pieChart3.startAnimation();
            barChart1.startAnimation();
            barChart2.startAnimation();
            createLineChartData();
        }
    }

    @Override
    public void formInitAndOpen() {
        System.out.println("FormDashboard init and open");
    }

    @Override
    public void formOpen() {
        System.out.println("FormDashboard open");
        refreshCharts();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fill, gap 10", "fill", "[]10[]"));

        JLabel welcomeLabel = new JLabel("Bienvenue, " + this.fullName + " !");
        welcomeLabel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font;"
                + "border:0,0,5,0");
        add(welcomeLabel);

        checkUserActivity();
        if (!hasActivity) {
            JLabel noActivityLabel = new JLabel("Aucune activité pour le moment. Commencez par créer un projet ou rejoindre une collaboration !");
            noActivityLabel.putClientProperty(FlatClientProperties.STYLE, ""
                    + "font:+1;"
                    + "foreground:#FF5555;"
                    + "border:0,0,5,0");
            add(noActivityLabel, "center");
            return;
        }

        createPieChart();
        createLineChart();
        createBarChart();
        refreshCharts();
    }

    public void refreshCharts() {
        if (!hasActivity) {
            return;
        }

        pieChart1.setDataset(createProjectsByStatusData());
        pieChart2.setDataset(createCollaborationsByStatusData());
        pieChart3.setDataset(createTaskCompletionData());
        barChart1.setDataset(createTaskEvolutionData());
        barChart2.setDataset(createCollaborationActivityData());
        createLineChartData();

        pieChart1.startAnimation();
        pieChart2.startAnimation();
        pieChart3.startAnimation();
        barChart1.startAnimation();
        barChart2.startAnimation();
    }

    private void checkUserActivity() {
        try {
            String projectsUrl = "https://teamworkatmini-jira.onrender.com/api/projects/" + uid;
            HttpRequest projectsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(projectsUrl))
                    .GET()
                    .build();
            HttpResponse<String> projectsResponse = httpClient.send(projectsRequest, HttpResponse.BodyHandlers.ofString());
            if (projectsResponse.statusCode() == 200) {
                JSONObject projectsJson = new JSONObject(projectsResponse.body());
                JSONArray projects = projectsJson.getJSONArray("projects");
                if (projects.length() > 0) {
                    hasActivity = true;
                    return;
                }
            }

            String collaborationsUrl = "https://teamworkatmini-jira.onrender.com/api/collaborations/" + uid;
            HttpRequest collaborationsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(collaborationsUrl))
                    .GET()
                    .build();
            HttpResponse<String> collaborationsResponse = httpClient.send(collaborationsRequest, HttpResponse.BodyHandlers.ofString());
            if (collaborationsResponse.statusCode() == 200) {
                JSONObject collaborationsJson = new JSONObject(collaborationsResponse.body());
                JSONArray collaborations = collaborationsJson.getJSONArray("collaborations");
                if (collaborations.length() > 0) {
                    hasActivity = true;
                    return;
                }

                for (int i = 0; i < collaborations.length(); i++) {
                    JSONObject project = collaborations.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                    HttpRequest tasksRequest = HttpRequest.newBuilder()
                            .uri(URI.create(tasksUrl))
                            .GET()
                            .build();
                    HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                    if (tasksResponse.statusCode() == 200) {
                        JSONObject tasksJson = new JSONObject(tasksResponse.body());
                        JSONArray tasks = tasksJson.getJSONArray("tasks");
                        for (int j = 0; j < tasks.length(); j++) {
                            JSONObject task = tasks.getJSONObject(j);
                            JSONArray assignedTo = task.optJSONArray("assignedTo");
                            if (assignedTo != null) {
                                for (int k = 0; k < assignedTo.length(); k++) {
                                    if (uid.equals(assignedTo.getString(k))) {
                                        hasActivity = true;
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de l'activité de l'utilisateur : " + e.getMessage());
        }
    }

    private void createPieChart() {
        pieChart1 = new PieChart();
        JLabel header1 = new JLabel("Statut de vos Projets");
        header1.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1");
        pieChart1.setHeader(header1);
        pieChart1.getChartColor().addColor(Color.decode("#f87171"), Color.decode("#34d399"), Color.decode("#fbbf24"), Color.decode("#60a5fa"));
        pieChart1.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        add(pieChart1, "split 3, height 290");

        pieChart2 = new PieChart();
        JLabel header2 = new JLabel("Collaborations par Statut");
        header2.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1");
        pieChart2.setHeader(header2);
        pieChart2.getChartColor().addColor(Color.decode("#f87171"), Color.decode("#34d399"), Color.decode("#fbbf24"));
        pieChart2.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        add(pieChart2, "height 290");

        pieChart3 = new PieChart();
        JLabel header3 = new JLabel("Taux de Complétion des Tâches");
        header3.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1");
        pieChart3.setHeader(header3);
        pieChart3.getChartColor().addColor(Color.decode("#4CAF50"), Color.decode("#FFCA28"), Color.decode("#F44336"));
        pieChart3.setChartType(PieChart.ChartType.DONUT_CHART);
        pieChart3.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        add(pieChart3, "height 290");
    }

    private void createLineChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        JFreeChart chart = createCustomLineChart(dataset);
        lineChartPanel = new ChartPanel(chart);
        lineChartPanel.setPreferredSize(new Dimension(600, 400));
        lineChartPanel.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        add(lineChartPanel);
    }

    private JFreeChart createCustomLineChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createLineChart(
                "Activité des Projets par Mois",
                "Mois",
                "Nombre de Projets",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.decode("#38bdf8"));
        renderer.setSeriesShapesVisible(0, true);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        NumberFormat customFormat = new DecimalFormat("#,##0");
        rangeAxis.setNumberFormatOverride(customFormat);

        return chart;
    }

    private void createBarChart() {
        barChart1 = new HorizontalBarChart();
        JLabel header1 = new JLabel("Évolution des Tâches (Projets Actifs)");
        header1.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "border:0,0,5,0");
        barChart1.setHeader(header1);
        barChart1.setBarColor(Color.decode("#f97316"));
        JPanel panel1 = new JPanel(new BorderLayout());
        panel1.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        panel1.add(barChart1);
        add(panel1, "split 2, gap 0 20");

        barChart2 = new HorizontalBarChart();
        JLabel header2 = new JLabel("Activité dans les Collaborations");
        header2.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "border:0,0,5,0");
        barChart2.setHeader(header2);
        barChart2.setBarColor(Color.decode("#10b981"));
        //barChart2.setValueFormat(new DecimalFormat("#,##0"));
        JPanel panel2 = new JPanel(new BorderLayout());
        panel2.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        panel2.add(barChart2);
        add(panel2);
    }
    
    
    private DefaultPieDataset<String> createProjectsByStatusData() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        int created = 0, active = 0, completed = 0, overdue = 0;
        long currentTime = System.currentTimeMillis();

        try {
            String projectsUrl = "https://teamworkatmini-jira.onrender.com/api/projects/" + uid;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(projectsUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray projects = json.getJSONArray("projects");

                for (int i = 0; i < projects.length(); i++) {
                    JSONObject project = projects.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    long endDate = project.optLong("endDate", 0);

                    String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                    HttpRequest tasksRequest = HttpRequest.newBuilder()
                            .uri(URI.create(tasksUrl))
                            .GET()
                            .build();
                    HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                    boolean hasTasks = false;
                    boolean allTasksCompleted = true;

                    if (tasksResponse.statusCode() == 200) {
                        JSONObject tasksJson = new JSONObject(tasksResponse.body());
                        JSONArray tasks = tasksJson.getJSONArray("tasks");
                        if (tasks.length() > 0) {
                            hasTasks = true;
                            for (int j = 0; j < tasks.length(); j++) {
                                JSONObject task = tasks.getJSONObject(j);
                                String status = task.getString("status");
                                if (!"terminé".equals(status)) {
                                    allTasksCompleted = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (!hasTasks) {
                        created++;
                    } else if (allTasksCompleted) {
                        completed++;
                    } else if (endDate != 0 && endDate < currentTime) {
                        overdue++;
                    } else {
                        active++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des projets par statut : " + e.getMessage());
        }

        if (created > 0) {
            dataset.addValue("Projets Créés", created);
        }
        if (active > 0) {
            dataset.addValue("Actifs", active);
        }
        if (completed > 0) {
            dataset.addValue("Terminés", completed);
        }
        if (overdue > 0) {
            dataset.addValue("En Retard", overdue);
        }
        return dataset;
    }

    private DefaultPieDataset<String> createCollaborationsByStatusData() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        int active = 0, completed = 0, overdue = 0;
        long currentTime = System.currentTimeMillis();

        try {
            String collaborationsUrl = "https://teamworkatmini-jira.onrender.com/api/collaborations/" + uid;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(collaborationsUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray collaborations = json.getJSONArray("collaborations");

                for (int i = 0; i < collaborations.length(); i++) {
                    JSONObject project = collaborations.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    long endDate = project.optLong("endDate", 0);

                    String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                    HttpRequest tasksRequest = HttpRequest.newBuilder()
                            .uri(URI.create(tasksUrl))
                            .GET()
                            .build();
                    HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                    boolean allTasksCompleted = true;

                    if (tasksResponse.statusCode() == 200) {
                        JSONObject tasksJson = new JSONObject(tasksResponse.body());
                        JSONArray tasks = tasksJson.getJSONArray("tasks");
                        for (int j = 0; j < tasks.length(); j++) {
                            JSONObject task = tasks.getJSONObject(j);
                            String status = task.getString("status");
                            if (!"terminé".equals(status)) {
                                allTasksCompleted = false;
                                break;
                            }
                        }
                    }

                    if (allTasksCompleted) {
                        completed++;
                    } else if (endDate != 0 && endDate < currentTime) {
                        overdue++;
                    } else {
                        active++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des collaborations par statut : " + e.getMessage());
        }

        if (active > 0) {
            dataset.addValue("Actifs", active);
        }
        if (completed > 0) {
            dataset.addValue("Terminés", completed);
        }
        if (overdue > 0) {
            dataset.addValue("En Retard", overdue);
        }
        return dataset;
    }

    private DefaultPieDataset<String> createTaskCompletionData() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        int completed = 0, inProgress = 0, failed = 0;

        try {
            String projectsUrl = "https://teamworkatmini-jira.onrender.com/api/projects/" + uid;
            HttpRequest projectsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(projectsUrl))
                    .GET()
                    .build();
            HttpResponse<String> projectsResponse = httpClient.send(projectsRequest, HttpResponse.BodyHandlers.ofString());
            if (projectsResponse.statusCode() == 200) {
                JSONObject projectsJson = new JSONObject(projectsResponse.body());
                JSONArray projects = projectsJson.getJSONArray("projects");

                for (int i = 0; i < projects.length(); i++) {
                    JSONObject project = projects.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                    HttpRequest tasksRequest = HttpRequest.newBuilder()
                            .uri(URI.create(tasksUrl))
                            .GET()
                            .build();
                    HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                    if (tasksResponse.statusCode() == 200) {
                        JSONObject tasksJson = new JSONObject(tasksResponse.body());
                        JSONArray tasks = tasksJson.getJSONArray("tasks");
                        for (int j = 0; j < tasks.length(); j++) {
                            JSONObject task = tasks.getJSONObject(j);
                            String status = task.getString("status");
                            switch (status) {
                                case "terminé":
                                    completed++;
                                    break;
                                case "en cours":
                                    inProgress++;
                                    break;
                                case "échoué":
                                    failed++;
                                    break;
                            }
                        }
                    }
                }
            }

            String collaborationsUrl = "https://teamworkatmini-jira.onrender.com/api/collaborations/" + uid;
            HttpRequest collaborationsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(collaborationsUrl))
                    .GET()
                    .build();
            HttpResponse<String> collaborationsResponse = httpClient.send(collaborationsRequest, HttpResponse.BodyHandlers.ofString());
            if (collaborationsResponse.statusCode() == 200) {
                JSONObject collaborationsJson = new JSONObject(collaborationsResponse.body());
                JSONArray collaborations = collaborationsJson.getJSONArray("collaborations");

                for (int i = 0; i < collaborations.length(); i++) {
                    JSONObject project = collaborations.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                    HttpRequest tasksRequest = HttpRequest.newBuilder()
                            .uri(URI.create(tasksUrl))
                            .GET()
                            .build();
                    HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                    if (tasksResponse.statusCode() == 200) {
                        JSONObject tasksJson = new JSONObject(tasksResponse.body());
                        JSONArray tasks = tasksJson.getJSONArray("tasks");
                        for (int j = 0; j < tasks.length(); j++) {
                            JSONObject task = tasks.getJSONObject(j);
                            JSONArray assignedTo = task.optJSONArray("assignedTo");
                            boolean isAssignedToUser = false;
                            if (assignedTo != null) {
                                for (int k = 0; k < assignedTo.length(); k++) {
                                    if (uid.equals(assignedTo.getString(k))) {
                                        isAssignedToUser = true;
                                        break;
                                    }
                                }
                            }
                            if (isAssignedToUser) {
                                String status = task.getString("status");
                                switch (status) {
                                    case "terminé":
                                        completed++;
                                        break;
                                    case "en cours":
                                        inProgress++;
                                        break;
                                    case "échoué":
                                        failed++;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des données de complétion des tâches : " + e.getMessage());
        }

        if (completed > 0) {
            dataset.addValue("Terminées", completed);
        }
        if (inProgress > 0) {
            dataset.addValue("En Cours", inProgress);
        }
        if (failed > 0) {
            dataset.addValue("Échouées", failed);
        }
        return dataset;
    }

    private void createLineChartData() {
        DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
        SimpleDateFormat df = new SimpleDateFormat("MMM yyyy", Locale.FRENCH);
        Map<String, Integer> projectsPerMonth = new TreeMap<>();

        Calendar cal = Calendar.getInstance();
        long earliestDate = Long.MAX_VALUE;
        long latestDate = Long.MIN_VALUE;

        try {
            String projectsUrl = "https://teamworkatmini-jira.onrender.com/api/projects/" + uid;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(projectsUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray projects = json.getJSONArray("projects");

                for (int i = 0; i < projects.length(); i++) {
                    JSONObject project = projects.getJSONObject(i);
                    long createdAt = project.getLong("createdAt");
                    String monthYear = df.format(new Date(createdAt));
                    projectsPerMonth.put(monthYear, projectsPerMonth.getOrDefault(monthYear, 0) + 1);

                    if (createdAt < earliestDate) {
                        earliestDate = createdAt;
                    }
                    if (createdAt > latestDate) {
                        latestDate = createdAt;
                    }
                }

                if (projectsPerMonth.isEmpty()) {
                    return;
                }

                cal.setTimeInMillis(earliestDate);
                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(latestDate);

                while (!cal.after(endCal)) {
                    String monthYear = df.format(cal.getTime());
                    categoryDataset.addValue(projectsPerMonth.getOrDefault(monthYear, 0), "Projets Créés", monthYear);
                    cal.add(Calendar.MONTH, 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la création des données du graphique linéaire : " + e.getMessage());
        }

        JFreeChart chart = createCustomLineChart(categoryDataset);
        lineChartPanel.setChart(chart);
    }

    private DefaultPieDataset<String> createTaskEvolutionData() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        SimpleDateFormat df = new SimpleDateFormat("MMM yyyy", Locale.FRENCH);
        Map<String, Integer> tasksPerMonth = new TreeMap<>();

        try {
            String projectsUrl = "https://teamworkatmini-jira.onrender.com/api/projects/" + uid;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(projectsUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray projects = json.getJSONArray("projects");

                for (int i = 0; i < projects.length(); i++) {
                    JSONObject project = projects.getJSONObject(i);
                    long endDate = project.optLong("endDate", 0);
                    if (endDate == 0 || endDate > System.currentTimeMillis()) {
                        String projectId = project.getString("projectId");
                        String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                        HttpRequest tasksRequest = HttpRequest.newBuilder()
                                .uri(URI.create(tasksUrl))
                                .GET()
                                .build();
                        HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                        if (tasksResponse.statusCode() == 200) {
                            JSONObject tasksJson = new JSONObject(tasksResponse.body());
                            JSONArray tasks = tasksJson.getJSONArray("tasks");
                            for (int j = 0; j < tasks.length(); j++) {
                                JSONObject task = tasks.getJSONObject(j);
                                long createdAt = task.getLong("createdAt");
                                String monthYear = df.format(new Date(createdAt));
                                tasksPerMonth.put(monthYear, tasksPerMonth.getOrDefault(monthYear, 0) + 1);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Integer> entry : tasksPerMonth.entrySet()) {
                dataset.addValue(entry.getKey(), entry.getValue().doubleValue());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la création des données d'évolution des tâches : " + e.getMessage());
        }

        return dataset;
    }

    private DefaultPieDataset<String> createCollaborationActivityData() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        SimpleDateFormat df = new SimpleDateFormat("MMM yyyy", Locale.FRENCH);
        Map<String, Integer> tasksPerMonth = new TreeMap<>();

        try {
            String collaborationsUrl = "https://teamworkatmini-jira.onrender.com/api/collaborations/" + uid;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(collaborationsUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray collaborations = json.getJSONArray("collaborations");

                for (int i = 0; i < collaborations.length(); i++) {
                    JSONObject project = collaborations.getJSONObject(i);
                    String projectId = project.getString("projectId");
                    String tasksUrl = "https://teamworkatmini-jira.onrender.com/api/tasks/" + projectId;
                    HttpRequest tasksRequest = HttpRequest.newBuilder()
                            .uri(URI.create(tasksUrl))
                            .GET()
                            .build();
                    HttpResponse<String> tasksResponse = httpClient.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                    if (tasksResponse.statusCode() == 200) {
                        JSONObject tasksJson = new JSONObject(tasksResponse.body());
                        JSONArray tasks = tasksJson.getJSONArray("tasks");
                        for (int j = 0; j < tasks.length(); j++) {
                            JSONObject task = tasks.getJSONObject(j);
                            JSONArray assignedTo = task.optJSONArray("assignedTo");
                            boolean isAssignedToUser = false;
                            if (assignedTo != null) {
                                for (int k = 0; k < assignedTo.length(); k++) {
                                    if (uid.equals(assignedTo.getString(k))) {
                                        isAssignedToUser = true;
                                        break;
                                    }
                                }
                            }
                            if (isAssignedToUser) {
                                long createdAt = task.getLong("createdAt");
                                String monthYear = df.format(new Date(createdAt));
                                tasksPerMonth.put(monthYear, tasksPerMonth.getOrDefault(monthYear, 0) + 1);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Integer> entry : tasksPerMonth.entrySet()) {
                dataset.addValue(entry.getKey(), entry.getValue().doubleValue());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la création des données d'activité de collaboration : " + e.getMessage());
        }

        return dataset;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Dashboard");

        jButton1.setText("Show Notifications Test");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(325, 325, 325)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb)
                .addGap(173, 173, 173)
                .addComponent(jButton1)
                .addContainerGap(237, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Notifications.getInstance().show(Notifications.Type.INFO, Notifications.Location.TOP_CENTER, "Hello sample message");
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel lb;
    // End of variables declaration//GEN-END:variables
}
