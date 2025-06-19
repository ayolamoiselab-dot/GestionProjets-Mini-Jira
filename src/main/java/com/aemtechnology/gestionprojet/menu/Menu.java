package com.aemtechnology.gestionprojet.menu;

import com.aemtechnology.gestionprojet.menu.mode.ToolBarAccentColor;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONObject;
import raven.menu.mode.LightDarkMode;
import raven.toast.Notifications;

public class Menu extends JPanel {

    private final String fullName;
    private final String email;
    private String profilePictureUrl;
    private JLabel header;
    private JButton editButton;
    private JPanel headerPanel;

    private final String menuItems[][] = {
        {"~MAIN~"},
        {"Dashboard"},
        {"Mes Projets"},
        {"Mes Collaborations"},
        {"~WEB APP~"},
        {"Notifications", "Invitations&Notifs"},
        {"~OTHER~"},
        {"Déconnexion"}
    };

    public boolean isMenuFull() {
        return menuFull;
    }

    public void setMenuFull(boolean menuFull) {
        this.menuFull = menuFull;
        if (menuFull) {
            header.setText(fullName);
            header.setHorizontalAlignment(getComponentOrientation().isLeftToRight() ? JLabel.LEFT : JLabel.RIGHT);
        } else {
            header.setText("");
            header.setHorizontalAlignment(JLabel.CENTER);
        }
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                ((MenuItem) com).setFull(menuFull);
            }
        }
        lightDarkMode.setMenuFull(menuFull);
        toolBarAccentColor.setMenuFull(menuFull);
    }

    private final List<MenuEvent> events = new ArrayList<>();
    private boolean menuFull = true;

    protected final boolean hideMenuTitleOnMinimum = true;
    protected final int menuTitleLeftInset = 5;
    protected final int menuTitleVgap = 5;
    protected final int menuMaxWidth = 250;
    protected final int menuMinWidth = 60;
    protected final int headerFullHgap = 5;

    public Menu(String fullName, String email) {
        this.fullName = fullName != null && !fullName.trim().isEmpty() ? fullName : "Utilisateur";
        this.email = email != null && !email.trim().isEmpty() ? email : "email@example.com";
        this.profilePictureUrl = fetchProfilePicture();
        init();
    }

    private void init() {
        setLayout(new MenuLayout());
        putClientProperty(FlatClientProperties.STYLE, ""
                + "border:20,2,2,2;"
                + "background:$Menu.background;"
                + "arc:10");

        header = new JLabel(fullName);
        updateProfilePicture();

        header.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.header.font;"
                + "foreground:$Menu.foreground");

        editButton = new JButton(loadIcon("edit.png", 16, 16));
        editButton.setContentAreaFilled(false);
        editButton.setBorderPainted(false);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadProfilePicture();
            }
        });

        headerPanel = new JPanel();
        headerPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        headerPanel.add(header);
        headerPanel.add(editButton);

        scroll = new JScrollPane();
        panelMenu = new JPanel(new MenuItemLayout(this));
        panelMenu.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5;"
                + "background:$Menu.background");

        scroll.setViewportView(panelMenu);
        scroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:null");

        JScrollBar vscroll = scroll.getVerticalScrollBar();
        vscroll.setUnitIncrement(10);

        String trackInsets = UIManager.getString("Menu.scroll.trackInsets");
        String thumbInsets = UIManager.getString("Menu.scroll.thumbInsets");
        String background = UIManager.getString("Menu.ScrollBar.background");
        String thumb = UIManager.getString("Menu.ScrollBar.thumb");

        if (trackInsets == null || trackInsets.trim().isEmpty()) {
            trackInsets = "5,5,5,5";
        }
        if (thumbInsets == null || thumbInsets.trim().isEmpty()) {
            thumbInsets = "5,5,5,5";
        }
        if (background == null || background.trim().isEmpty()) {
            background = "@background";
        }
        if (thumb == null || thumb.trim().isEmpty()) {
            thumb = "#666666";
        }

        vscroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "trackInsets:" + trackInsets + ";"
                + "thumbInsets:" + thumbInsets + ";"
                + "background:" + background + ";"
                + "thumb:" + thumb);

        createMenu();
        lightDarkMode = new LightDarkMode();
        toolBarAccentColor = new ToolBarAccentColor(this);
        toolBarAccentColor.setVisible(FlatUIUtils.getUIBoolean("AccentControl.show", false));
        add(headerPanel);
        add(scroll);
        add(lightDarkMode);
        add(toolBarAccentColor);
    }

    private String fetchProfilePicture() {
        System.out.println("Récupération de la photo de profil pour l'email : " + email);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/by-email/" + email))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Réponse de l'API /api/user/by-email - Status: " + response.statusCode() + ", Body: " + response.body());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String profilePicture = json.optString("profilePictureUrl", null);
                System.out.println("Photo de profil récupérée : " + profilePicture);
                return profilePicture;
            } else {
                System.out.println("Échec de la récupération de la photo de profil - Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la photo de profil : " + e.getMessage());
        }
        return null;
    }

    private void updateProfilePicture() {
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            try {
                Image image = ImageIO.read(new URI(profilePictureUrl).toURL());
                Image scaledImage = image.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                header.setIcon(new ImageIcon(scaledImage));
                System.out.println("Photo de profil chargée avec succès : " + profilePictureUrl);
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de la photo de profil : " + e.getMessage());
                setDefaultIcon();
            }
        } else {
            System.out.println("Aucune photo de profil disponible, affichage de l'icône par défaut.");
            setDefaultIcon();
        }
    }

    private void setDefaultIcon() {
        ImageIcon defaultIcon = loadIcon("user.png", 40, 40);
        header.setIcon(defaultIcon);
    }

    private void uploadProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choisir une photo de profil");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "jpeg"));
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                HttpClient client = HttpClient.newHttpClient();
                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("image", base64Image);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://teamworkatmini-jira.onrender.com/api/user/profile-picture"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());
                    this.profilePictureUrl = responseJson.getString("profilePictureUrl");
                    updateProfilePicture();
                    editButton.setVisible(false);
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "Photo de profil mise à jour avec succès !");
                } else {
                    Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur lors de la mise à jour de la photo de profil.");
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'upload de la photo : " + e.getMessage());
                Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Erreur lors de l'upload de la photo.");
            }
        }
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        try {
            Image image = ImageIO.read(getClass().getResource("/raven/icon/png/" + path));
            if (image != null) {
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'icône : " + path);
        }
        return new ImageIcon();
    }

    private void createMenu() {
        int index = 0;
        for (int i = 0; i < menuItems.length; i++) {
            String menuName = menuItems[i][0];
            if (menuName.startsWith("~") && menuName.endsWith("~")) {
                panelMenu.add(createTitle(menuName));
            } else {
                MenuItem menuItem = new MenuItem(this, menuItems[i], index++, events);
                panelMenu.add(menuItem);
            }
        }
    }

    private JLabel createTitle(String title) {
        String menuName = title.substring(1, title.length() - 1);
        JLabel lbTitle = new JLabel(menuName);
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.label.font;"
                + "foreground:$Menu.title.foreground");
        return lbTitle;
    }

    public void setSelectedMenu(int index, int subIndex) {
        runEvent(index, subIndex);
    }

    protected void setSelected(int index, int subIndex) {
        int size = panelMenu.getComponentCount();
        for (int i = 0; i < size; i++) {
            Component com = panelMenu.getComponent(i);
            if (com instanceof MenuItem) {
                MenuItem item = (MenuItem) com;
                if (item.getMenuIndex() == index) {
                    item.setSelectedIndex(subIndex);
                } else {
                    item.setSelectedIndex(-1);
                }
            }
        }
    }

    protected void runEvent(int index, int subIndex) {
        MenuAction menuAction = new MenuAction();
        for (MenuEvent event : events) {
            event.menuSelected(index, subIndex, menuAction);
        }
        if (!menuAction.isCancel()) {
            setSelected(index, subIndex);
        }
    }

    public void addMenuEvent(MenuEvent event) {
        events.add(event);
    }

    public void hideMenuItem() {
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                ((MenuItem) com).hideMenuItem();
            }
        }
        revalidate();
    }

    public boolean isHideMenuTitleOnMinimum() {
        return hideMenuTitleOnMinimum;
    }

    public int getMenuTitleLeftInset() {
        return menuTitleLeftInset;
    }

    public int getMenuTitleVgap() {
        return menuTitleVgap;
    }

    public int getMenuMaxWidth() {
        return menuMaxWidth;
    }

    public int getMenuMinWidth() {
        return menuMinWidth;
    }

    private JScrollPane scroll;
    private JPanel panelMenu;
    private LightDarkMode lightDarkMode;
    private ToolBarAccentColor toolBarAccentColor;

    private class MenuLayout implements LayoutManager {

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
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int gap = UIScale.scale(5);
                int sheaderFullHgap = UIScale.scale(headerFullHgap);
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int iconWidth = width;
                int iconHeight = headerPanel.getPreferredSize().height;
                int hgap = menuFull ? sheaderFullHgap : 0;
                int accentColorHeight = 0;
                if (toolBarAccentColor.isVisible()) {
                    accentColorHeight = toolBarAccentColor.getPreferredSize().height + gap;
                }

                headerPanel.setBounds(x + hgap, y, iconWidth - (hgap * 2), iconHeight);

                int ldgap = UIScale.scale(10);
                int ldWidth = width - ldgap * 2;
                int ldHeight = lightDarkMode.getPreferredSize().height;
                int ldx = x + ldgap;
                int ldy = y + height - ldHeight - ldgap - accentColorHeight;

                int menux = x;
                int menuy = y + iconHeight + gap;
                int menuWidth = width;
                int menuHeight = height - (iconHeight + gap) - (ldHeight + ldgap * 2) - (accentColorHeight);
                scroll.setBounds(menux, menuy, menuWidth, menuHeight);

                lightDarkMode.setBounds(ldx, ldy, ldWidth, ldHeight);

                if (toolBarAccentColor.isVisible()) {
                    int tbheight = toolBarAccentColor.getPreferredSize().height;
                    int tbwidth = Math.min(toolBarAccentColor.getPreferredSize().width, ldWidth);
                    int tby = y + height - tbheight - ldgap;
                    int tbx = ldx + ((ldWidth - tbwidth) / 2);
                    toolBarAccentColor.setBounds(tbx, tby, tbwidth, tbheight);
                }
            }
        }
    }
}