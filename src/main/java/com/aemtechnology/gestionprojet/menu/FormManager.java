package com.aemtechnology.gestionprojet.menu;

import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import java.awt.Image;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import com.aemtechnology.gestionprojet.components.MainForm;
import com.aemtechnology.gestionprojet.components.SimpleForm;
import com.aemtechnology.gestionprojet.forms.DashboardForm;
import com.aemtechnology.gestionprojet.forms.ProjectsForm;
import com.aemtechnology.gestionprojet.view.LoginView;
import com.aemtechnology.gestionprojet.swing.slider.PanelSlider;
import com.aemtechnology.gestionprojet.swing.slider.SimpleTransition;
import com.aemtechnology.gestionprojet.view.HomeScreen;
import com.aemtechnology.gestionprojet.view.LoginView;
import raven.drawer.Drawer;
import raven.popup.GlassPanePopup;
import raven.utils.UndoRedo;

public class FormManager {

    private static FormManager instance;
    private final JFrame frame;
    private final JPanel formContainer;
    private final MyDrawerBuilder drawerBuilder;
    private final UndoRedo<SimpleForm> forms = new UndoRedo<>();
    private boolean menuShowing = true;
    private final PanelSlider panelSlider;
    private final MainForm mainForm;
//    private final Menu menu;
    private final boolean undecorated;
    private final HomeScreen homeScreen;
    private DashboardForm dashboardForm;
    private ProjectsForm projectsForm;

    public static void install(JFrame frame, boolean undecorated, HomeScreen homeScreen, String fullName, String email, JPanel formContainer) {
        instance = new FormManager(frame, undecorated, homeScreen, fullName, email, formContainer);
        GlassPanePopup.install(frame);
    }

    private FormManager(JFrame frame, boolean undecorated, HomeScreen homeScreen, String fullName, String email, JPanel formContainer) {
        this.frame = frame;
        this.formContainer = formContainer;
        this.undecorated = undecorated;
        this.homeScreen = homeScreen;
        panelSlider = new PanelSlider();
        mainForm = new MainForm(undecorated);
        drawerBuilder = new MyDrawerBuilder(homeScreen, fullName, email);
        Drawer.getInstance().setDrawerBuilder(drawerBuilder);
     //   menu = new Menu(drawerBuilder);
        formContainer.removeAll();
        formContainer.add(panelSlider);
        formContainer.revalidate();
        formContainer.repaint();
        System.out.println("FormManager initialisé, panelSlider ajouté au formContainer");

        // Initialiser les formulaires principaux
        dashboardForm = new DashboardForm(fullName, email); // Passer fullName et email au DashboardForm
        projectsForm = new ProjectsForm(homeScreen);
        // S'assurer que les formulaires sont initialisés
        dashboardForm.formInitAndOpen();
        projectsForm.formInitAndOpen();
    }

    public static void showMenu() {
        if (instance != null) {
            instance.menuShowing = true;
//            instance.panelSlider.addSlide(instance.menu, SimpleTransition.getShowMenuTransition(instance.drawerBuilder.getDrawerWidth(), instance.undecorated));
            System.out.println("Menu affiché");
        } else {
            System.err.println("Erreur : FormManager non initialisé lors de showMenu");
        }
    }

    public static void showForm(SimpleForm component) {
        System.out.println("FormManager.showForm appelé avec: " + component.getClass().getSimpleName());
        if (isNewFormAble()) {
            if (instance.forms.getCurrent() != component) {
                instance.forms.add(component);
            }
            if (instance.menuShowing) {
                instance.menuShowing = false;
                instance.mainForm.setForm(component);
                instance.panelSlider.addSlide(instance.mainForm, SimpleTransition.getSwitchFormTransition(null, instance.drawerBuilder.getDrawerWidth()));
            } else {
                instance.mainForm.showForm(component);
            }
            component.formInitAndOpen();
            instance.panelSlider.revalidate();
            instance.panelSlider.repaint();
            System.out.println("Formulaire affiché: " + component.getClass().getSimpleName());
        } else {
            System.out.println("Impossible d'afficher le formulaire: formClose() a retourné false");
        }
    }

    public static void logout() {
        FlatAnimatedLafChange.showSnapshot();
        instance.frame.dispose();
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.add(new LoginView());
        loginFrame.pack();
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    public static void login(String fullName, String email) {
        System.out.println("Avant FormManager.login - fullName: " + fullName + ", email: " + email);
        FlatAnimatedLafChange.showSnapshot();
        instance.formContainer.removeAll();
        instance.formContainer.add(instance.panelSlider);
//        ((MyDrawerBuilder) instance.menu.getDrawerBuilder()).setUserInfo(fullName, email);
        instance.formContainer.revalidate();
        instance.formContainer.repaint();
        instance.frame.revalidate();
        instance.frame.repaint();
        SwingUtilities.invokeLater(() -> {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            showForm(instance.dashboardForm);
            System.out.println("FormManager.login appelé avec succès");
        });
    }

    public static void hideMenu() {
        instance.menuShowing = false;
        instance.panelSlider.addSlide(instance.mainForm, SimpleTransition.getHideMenuTransition(instance.drawerBuilder.getDrawerWidth(), instance.undecorated));
    }

    public static void undo() {
        if (isNewFormAble() && !instance.menuShowing && instance.forms.isUndoAble()) {
            instance.mainForm.showForm(instance.forms.undo(), SimpleTransition.getDefaultTransition(true));
            instance.forms.getCurrent().formOpen();
        }
    }

    public static void redo() {
        if (isNewFormAble() && !instance.menuShowing && instance.forms.isRedoAble()) {
            instance.mainForm.showForm(instance.forms.redo());
            instance.forms.getCurrent().formOpen();
        }
    }

    public static void refresh() {
        if (!instance.menuShowing) {
            instance.forms.getCurrent().formRefresh();
        }
    }

    public static UndoRedo<SimpleForm> getForms() {
        return instance.forms;
    }

    public static boolean isNewFormAble() {
        return instance.forms.getCurrent() == null || instance.forms.getCurrent().formClose();
    }

    public static void updateTempFormUI() {
        for (SimpleForm f : instance.forms) {
            SwingUtilities.updateComponentTreeUI(f);
        }
    }

    public static DashboardForm getDashboardForm() {
        return instance != null ? instance.dashboardForm : null;
    }

    public static ProjectsForm getProjectsForm() {
        return instance != null ? instance.projectsForm : null;
    }
}