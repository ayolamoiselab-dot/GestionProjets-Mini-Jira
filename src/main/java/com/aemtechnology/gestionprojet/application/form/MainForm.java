package com.aemtechnology.gestionprojet.application.form;

import com.aemtechnology.gestionprojet.forms.CollaborationsForm;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.aemtechnology.gestionprojet.menu.Menu;
import com.aemtechnology.gestionprojet.menu.MenuAction;
import com.aemtechnology.gestionprojet.menu.MenuItem;
import com.aemtechnology.gestionprojet.view.HomeScreen;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import com.aemtechnology.gestionprojet.application.form.other.FormDashboard;

public class MainForm extends JLayeredPane {

    private CollaborationsForm collaborationsForm;
    private JLabel notificationBadge;
    private JPanel badgePanel;
    private final String fullName;
    private final String email;
    private final HomeScreen homeScreen;
    private String uid;
    private WebSocketClient webSocketClient;
    private Component currentForm; // Pour suivre le formulaire actuellement affiché

    private int getEmailMenuYPosition() {
        Component[] components = menu.getComponents();
        for (Component comp : components) {
            if (comp instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) comp;
                JPanel panelMenu = (JPanel) scroll.getViewport().getView();
                Component[] menuComponents = panelMenu.getComponents();

                int menuItemCount = 0;
                for (Component menuComp : menuComponents) {
                    if (menuComp instanceof MenuItem) {
                        if (menuItemCount == 3) {
                            MenuItem emailItem = (MenuItem) menuComp;
                            return emailItem.getY() + scroll.getY() + menu.getY() + (emailItem.getHeight() / 2) - (badgePanel.getHeight() / 2);
                        }
                        menuItemCount++;
                    }
                }
            }
        }
        return UIScale.scale(180);
    }

    public MainForm(String fullName, String email, HomeScreen homeScreen) {
        this.fullName = fullName;
        this.email = email;
        this.homeScreen = homeScreen;
        collaborationsForm = new CollaborationsForm(homeScreen);
        fetchUid();
        init();
        fetchNotificationsAndInvitations();
        initWebSocket();
    }

    private void fetchUid() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + email))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                this.uid = json.getString("uid");
                System.out.println("UID récupéré : " + uid);
            } else {
                System.err.println("Erreur lors de la récupération de l'UID - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'UID : " + e.getMessage());
        }
    }

    private void initWebSocket() {
        if (uid == null) {
            System.err.println("UID non disponible, impossible d'initialiser le WebSocket.");
            return;
        }

        try {
            URI uri = new URI("wss://teamworkatmini-jira.onrender.com/ws");
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("WebSocket connecté pour l'utilisateur : " + uid);
                    // S'abonner aux notifications
                    JSONObject subscribeMessage = new JSONObject();
                    subscribeMessage.put("type", "subscribe");
                    subscribeMessage.put("destination", "/topic/notifications/" + uid);
                    send(subscribeMessage.toString());

                    // S'abonner aux mises à jour des projets et tâches
                    JSONObject subscribeProjectsMessage = new JSONObject();
                    subscribeProjectsMessage.put("type", "subscribe");
                    subscribeProjectsMessage.put("destination", "/topic/projects/" + uid);
                    send(subscribeProjectsMessage.toString());

                    JSONObject subscribeTasksMessage = new JSONObject();
                    subscribeTasksMessage.put("type", "subscribe");
                    subscribeTasksMessage.put("destination", "/topic/tasks/" + uid);
                    send(subscribeTasksMessage.toString());
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("Message WebSocket reçu : " + message);
                    try {
                        JSONObject jsonMessage = new JSONObject(message);
                        String type = jsonMessage.optString("type", "");

                        // Gérer les notifications (invitations, réponses, lectures)
                        if ("invitation".equals(type) || "invitation_response".equals(type)) {
                            SwingUtilities.invokeLater(() -> fetchNotificationsAndInvitations());
                        } else if ("project_update".equals(type) || "task_update".equals(type)) {
                            // Si le dashboard est affiché, rafraîchir les graphiques
                            SwingUtilities.invokeLater(() -> {
                                if (currentForm instanceof FormDashboard) {
                                    ((FormDashboard) currentForm).refreshCharts();
                                }
                            });
                        } else if ("read".equals(type)) {
                            String notificationId = jsonMessage.getString("notificationId");
                            homeScreen.updateNotificationStatus(notificationId, "read");
                            SwingUtilities.invokeLater(() -> fetchNotificationsAndInvitations());
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors du traitement du message WebSocket : " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket fermé : " + reason);
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            System.out.println("Tentative de reconnexion WebSocket...");
                            webSocketClient.reconnect();
                        } catch (InterruptedException e) {
                            System.err.println("Erreur lors de la reconnexion WebSocket : " + e.getMessage());
                        }
                    }).start();
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

    public void fetchNotificationsAndInvitations() {
        if (uid == null) {
            System.err.println("UID non disponible, impossible de récupérer les notifications.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();

            // Récupérer les notifications
            HttpRequest notificationsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/notifications/" + uid))
                    .GET()
                    .build();
            HttpResponse<String> notificationsResponse = client.send(notificationsRequest, HttpResponse.BodyHandlers.ofString());
            int unreadCount = 0;
            if (notificationsResponse.statusCode() == 200) {
                JSONObject notificationsJson = new JSONObject(notificationsResponse.body());
                JSONArray notifications = notificationsJson.getJSONArray("notifications");
                for (int i = 0; i < notifications.length(); i++) {
                    JSONObject notification = notifications.getJSONObject(i);
                    String status = notification.optString("status", "unread");
                    if (status.equals("unread")) {
                        unreadCount++;
                    }
                }
                homeScreen.setNotifications(notifications);
            } else {
                System.err.println("Erreur lors de la récupération des notifications - Status: " + notificationsResponse.statusCode());
            }

            // Récupérer les invitations en attente
            HttpRequest invitationsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/invitations/pending/" + uid))
                    .GET()
                    .build();
            HttpResponse<String> invitationsResponse = client.send(invitationsRequest, HttpResponse.BodyHandlers.ofString());
            if (invitationsResponse.statusCode() == 200) {
                JSONObject invitationsJson = new JSONObject(invitationsResponse.body());
                JSONArray invitations = invitationsJson.getJSONArray("invitations");
                unreadCount += invitations.length();
                homeScreen.setPendingInvitations(invitations);
            } else {
                System.err.println("Erreur lors de la récupération des invitations - Status: " + invitationsResponse.statusCode());
            }

            setNotificationBadge(unreadCount);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des notifications/invitations : " + e.getMessage());
        }
    }

    private void init() {
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new MainFormLayout());
        menu = new Menu(fullName, email);
        panelBody = new JPanel(new BorderLayout());
        initMenuArrowIcon();
        menuButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Menu.button.background;"
                + "arc:999;"
                + "focusWidth:0;"
                + "borderWidth:0");
        menuButton.addActionListener((ActionEvent e) -> {
            setMenuFull(!menu.isMenuFull());
        });
        initMenuEvent();
        addNotificationBadge();
        setLayer(menuButton, JLayeredPane.POPUP_LAYER);
        add(menuButton);
        add(menu);
        add(panelBody);
    }

    private void addNotificationBadge() {
        badgePanel = new JPanel(new BorderLayout());
        badgePanel.setOpaque(true);
        badgePanel.setBackground(new Color(34, 139, 34)); // Vert foncé pour une meilleure visibilité
        badgePanel.setPreferredSize(new Dimension(20, 20));

        notificationBadge = new JLabel("0");
        notificationBadge.setForeground(Color.WHITE); // Texte blanc pour contraste
        notificationBadge.setOpaque(false);
        notificationBadge.setHorizontalAlignment(SwingConstants.CENTER);
        notificationBadge.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        badgePanel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:999"); // Garder le style arrondi, mais supprimer background et foreground

        badgePanel.add(notificationBadge, BorderLayout.CENTER);
        badgePanel.setVisible(false);
        setLayer(badgePanel, JLayeredPane.POPUP_LAYER);
        add(badgePanel);
    }

    public void setNotificationBadge(int count) {
        notificationBadge.setText(String.valueOf(count));
        badgePanel.setVisible(count > 0);
        revalidate();
        repaint();
    }

    @Override
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        initMenuArrowIcon();
    }

    private void initMenuArrowIcon() {
        if (menuButton == null) {
            menuButton = new JButton();
        }
        String icon = (getComponentOrientation().isLeftToRight()) ? "menu_left.svg" : "menu_right.svg";
        menuButton.setIcon(new FlatSVGIcon("raven/icon/svg/" + icon, 0.8f));
    }

    private void initMenuEvent() {
        menu.addMenuEvent((int index, int subIndex, MenuAction action) -> {
            System.out.println("Menu sélectionné - Index: " + index + ", SubIndex: " + subIndex);
            if (index == 0) {
                homeScreen.showForm("dashboard");
            } else if (index == 1) {
                homeScreen.showForm("projects");
            } else if (index == 2) {
                homeScreen.showForm("collaborations");
            } else if (index == 3) {
                if (subIndex == 1) {
                    homeScreen.showForm("inbox");
                    fetchNotificationsAndInvitations();
                } else {
                    action.cancel();
                }
            } else if (index == 4) {
                System.out.println("Déconnexion déclenchée");
                homeScreen.logout();
            } else {
                System.out.println("Action annulée pour index: " + index);
                action.cancel();
            }
        });
    }

    private void setMenuFull(boolean full) {
        String icon;
        if (getComponentOrientation().isLeftToRight()) {
            icon = (full) ? "menu_left.svg" : "menu_right.svg";
        } else {
            icon = (full) ? "menu_right.svg" : "menu_left.svg";
        }
        menuButton.setIcon(new FlatSVGIcon("raven/icon/svg/" + icon, 0.8f));
        menu.setMenuFull(full);
        revalidate();
    }

    public void hideMenu() {
        menu.hideMenuItem();
    }

    public void showForm(Component component) {
        if (component instanceof CollaborationsForm) {
            ((CollaborationsForm) component).refreshCollaborations();
        }
        // Ajouter une vérification pour FormDashboard
        if (component instanceof FormDashboard) {
            ((FormDashboard) component).refreshCharts();
        }
        currentForm = component; // Mettre à jour le formulaire actuellement affiché
        panelBody.removeAll();
        panelBody.add(component);
        panelBody.repaint();
        panelBody.revalidate();
    }

    public void setSelectedMenu(int index, int subIndex) {
        menu.setSelectedMenu(index, subIndex);
    }

    private Menu menu;
    private JPanel panelBody;
    private JButton menuButton;

    private class MainFormLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(5, 5);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(0, 0);
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                boolean ltr = parent.getComponentOrientation().isLeftToRight();
                Insets insets = UIScale.scale(parent.getInsets());
                int x = insets.left;
                int y = insets.top;
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int menuWidth = UIScale.scale(menu.isMenuFull() ? menu.getMenuMaxWidth() : menu.getMenuMinWidth());
                int menuX = ltr ? x : x + width - menuWidth;
                menu.setBounds(menuX, y, menuWidth, height);

                int menuButtonWidth = menuButton.getPreferredSize().width;
                int menuButtonHeight = menuButton.getPreferredSize().height;
                int menubX = ltr ? (int) (x + menuWidth - (menuButtonWidth * (menu.isMenuFull() ? 0.5f : 0.3f)))
                        : (int) (menuX - (menuButtonWidth * (menu.isMenuFull() ? 0.5f : 0.7f)));
                menuButton.setBounds(menubX, UIScale.scale(30), menuButtonWidth, menuButtonHeight);

                int badgeX = ltr ? (menuX + menuWidth - 30) : (menuX + 10);
                int badgeY = getEmailMenuYPosition();
                badgePanel.setBounds(badgeX, badgeY, badgePanel.getPreferredSize().width, badgePanel.getPreferredSize().height);

                int gap = UIScale.scale(5);
                int bodyWidth = width - menuWidth - gap;
                int bodyHeight = height;
                int bodyx = ltr ? (x + menuWidth + gap) : x;
                int bodyy = y;
                panelBody.setBounds(bodyx, bodyy, bodyWidth, bodyHeight);
            }
        }
    }
}
