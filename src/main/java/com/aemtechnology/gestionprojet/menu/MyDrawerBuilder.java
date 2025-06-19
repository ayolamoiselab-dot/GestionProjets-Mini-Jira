package com.aemtechnology.gestionprojet.menu;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import raven.drawer.component.DrawerPanel;
import raven.drawer.component.SimpleDrawerBuilder;
import raven.drawer.component.footer.SimpleFooterData;
import raven.drawer.component.header.SimpleHeaderData;
import raven.drawer.component.header.SimpleHeaderStyle;
import raven.drawer.component.menu.MenuAction;
import raven.drawer.component.menu.MenuEvent;
import raven.drawer.component.menu.SimpleMenuOption;
import raven.drawer.component.menu.SimpleMenuStyle;
import raven.drawer.component.menu.data.Item;
import raven.drawer.component.menu.data.MenuItem;
import raven.swing.AvatarIcon;
import com.aemtechnology.gestionprojet.view.HomeScreen;
import raven.drawer.Drawer;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

    private final HomeScreen homeScreen;
    private String fullName;
    private String email;
    private final ThemesChange themesChange;
    private int lastSelectedIndex = 1; // Par défaut, on sélectionne "Dashboard" (index 1)

    public MyDrawerBuilder(HomeScreen homeScreen, String fullName, String email) {
        this.homeScreen = homeScreen;
        this.fullName = fullName != null && !fullName.trim().isEmpty() ? fullName : "User";
        this.email = email != null && !email.trim().isEmpty() ? email : "user@example.com";
        this.themesChange = new ThemesChange();
        System.out.println("MyDrawerBuilder initialisé - fullName: " + this.fullName + ", email: " + this.email);
    }

    public void setUserInfo(String fullName, String email) {
        System.out.println("Mise à jour des infos utilisateur - fullName: " + fullName + ", email: " + email);
        if (fullName != null && !fullName.trim().isEmpty()) {
            this.fullName = fullName;
        } else {
            this.fullName = "User";
        }
        if (email != null && !email.trim().isEmpty()) {
            this.email = email;
        } else {
            this.email = "user@example.com";
        }
        // Mettre à jour le header dynamiquement
        DrawerPanel drawerPanel = Drawer.getInstance().getDrawerPanel();
        if (drawerPanel != null) {
            SimpleHeaderData headerData = getSimpleHeaderData();
            headerData.setTitle(this.fullName);
            headerData.setDescription(this.email);
            drawerPanel.revalidate();
            drawerPanel.repaint();
        }
    }

    @Override
    public Component getFooter() {
        return themesChange;
    }

    @Override
    public void build(DrawerPanel drawerPanel) {
        drawerPanel.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Drawer.background");
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        AvatarIcon icon = new AvatarIcon(getClass().getResource("/images/profile.png"), 60, 60, 999);
        icon.setBorder(2);
        return new SimpleHeaderData()
                .setIcon(icon)
                .setTitle(fullName)
                .setDescription(email)
                .setHeaderStyle(new SimpleHeaderStyle() {
                    @Override
                    public void styleTitle(JLabel label) {
                        label.putClientProperty(FlatClientProperties.STYLE, ""
                                + "[light]foreground:#FAFAFA;"
                                + "[dark]foreground:#E1E1E1");
                    }

                    @Override
                    public void styleDescription(JLabel label) {
                        label.putClientProperty(FlatClientProperties.STYLE, ""
                                + "[light]foreground:#E1E1E1;"
                                + "[dark]foreground:#B0B0B0");
                    }
                });
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData();
    }

    @Override
    public SimpleMenuOption getSimpleMenuOption() {
        MenuItem[] items = new MenuItem[]{
            new Item.Label("MAIN"),
            new Item("Dashboard", "dashboard.svg"),
            new Item("My Projects", "calendar.svg"),
            new Item.Label("OTHER"),
            new Item("Calendar", "calendar.svg"),
            new Item("Settings", "settings.svg"),
            new Item("Logout", "logout.svg")
        };

        SimpleMenuOption simpleMenuOption = new SimpleMenuOption() {
            @Override
            public Icon buildMenuIcon(String path, float scale) {
                FlatSVGIcon icon = new FlatSVGIcon(path, scale);
                FlatSVGIcon.ColorFilter colorFilter = new FlatSVGIcon.ColorFilter();
                colorFilter.add(Color.decode("#969696"), Color.decode("#FAFAFA"), Color.decode("#969696"));
                icon.setColorFilter(colorFilter);
                return icon;
            }
        };

        simpleMenuOption.setMenuStyle(new SimpleMenuStyle() {
            @Override
            public void styleMenuItem(JButton menu, int[] index) {
                menu.putClientProperty(FlatClientProperties.STYLE, ""
                        + "[light]foreground:#FAFAFA;"
                        + "arc:10");
            }

            @Override
            public void styleMenu(JComponent component) {
                component.putClientProperty(FlatClientProperties.STYLE, ""
                        + "background:$Drawer.background");
            }

            @Override
            public void styleLabel(JLabel label) {
                label.putClientProperty(FlatClientProperties.STYLE, ""
                        + "[light]foreground:darken(#FAFAFA,15%);"
                        + "[dark]foreground:darken($Label.foreground,30%)");
            }
        });

        simpleMenuOption.addMenuEvent(new MenuEvent() {
            @Override
            public void selected(MenuAction action, int[] index) {
                System.out.println("Menu sélectionné, index: " + java.util.Arrays.toString(index));
                if (index.length == 1) {
                    int menuIndex = index[0];

                    // Ignorer les étiquettes (index 0 et 3)
                    if (menuIndex == 0 || menuIndex == 3) {
                        System.out.println("Étiquette sélectionnée, aucune action (index: " + menuIndex + ")");
                        return;
                    }

                    // Mettre à jour l'index sélectionné
                    lastSelectedIndex = menuIndex;

                    switch (menuIndex) {
                        case 1: // Dashboard
                            System.out.println("Affichage de DashboardForm");
                            homeScreen.showForm("dashboard");
                            break;
                        case 2: // My Projects
                            System.out.println("Affichage de ProjectsForm");
                            homeScreen.showForm("projects");
                            break;
                        case 4: // Calendar
                            JOptionPane.showMessageDialog(null, "La fonctionnalité 'Calendar' n'est pas encore implémentée.", "Fonctionnalité indisponible", JOptionPane.INFORMATION_MESSAGE);
                            System.out.println("Affichage du formulaire Calendar (non implémenté)");
                            // Revenir à la dernière vue valide
                            restoreLastValidView();
                            break;
                        case 5: // Settings
                            JOptionPane.showMessageDialog(null, "La fonctionnalité 'Settings' n'est pas encore implémentée.", "Fonctionnalité indisponible", JOptionPane.INFORMATION_MESSAGE);
                            System.out.println("Affichage du formulaire Settings (non implémenté)");
                            // Revenir à la dernière vue valide
                            restoreLastValidView();
                            break;
                        case 6: // Logout
                            System.out.println("Déconnexion");
                            homeScreen.logout();
                            break;
                        default:
                            System.out.println("Index non géré: " + menuIndex);
                            // Revenir à la dernière vue valide
                            restoreLastValidView();
                    }
                }
            }

            private void restoreLastValidView() {
                // Restaurer la dernière vue valide (Dashboard ou Projects)
                switch (lastSelectedIndex) {
                    case 1:
                        System.out.println("Retour à DashboardForm");
                        homeScreen.showForm("dashboard");
                        break;
                    case 2:
                        System.out.println("Retour à ProjectsForm");
                        homeScreen.showForm("projects");
                        break;
                    default:
                        System.out.println("Retour par défaut à DashboardForm");
                        homeScreen.showForm("dashboard"); // Par défaut, revenir au Dashboard
                }
            }
        });

        simpleMenuOption.setMenus(items)
                .setBaseIconPath("images")
                .setIconScale(0.45f);
        return simpleMenuOption;
    }

    @Override
    public int getDrawerWidth() {
        return 200;
    }
}