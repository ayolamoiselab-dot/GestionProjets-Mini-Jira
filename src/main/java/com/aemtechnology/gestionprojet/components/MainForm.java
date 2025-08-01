package com.aemtechnology.gestionprojet.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.miginfocom.swing.MigLayout;
import com.aemtechnology.gestionprojet.menu.FormManager;
import com.aemtechnology.gestionprojet.swing.slider.PanelSlider;
import com.aemtechnology.gestionprojet.swing.slider.SimpleTransition;
import com.aemtechnology.gestionprojet.swing.slider.SliderTransition;

public class MainForm extends JPanel {

    private final boolean undecorated;
    private JPanel header;
    private JButton cmdMenu;
    private JButton cmdUndo;
    private JButton cmdRedo;
    private JButton cmdRefresh;
    private PanelSlider panelSlider;

    public MainForm(boolean undecorated) {
        this.undecorated = undecorated;
        init();
    }

    private void init() {
        // Appliquer le même style de fond que le Drawer
        putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Drawer.background;" // Utilise le même fond que le menu
                + (undecorated ? "border:5,5,5,5;arc:30;" : ""));

        setLayout(new MigLayout("wrap,fillx", "[fill]", ""));
        header = createHeader();
        panelSlider = new PanelSlider();
        JScrollPane scroll = new JScrollPane(panelSlider);
        scroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:0,0,0,0;" // Pas de bordure pour le scroll
                + "background:$Drawer.background"); // Même fond que le Drawer
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;"
                + "width:10;"
                + "background:$Drawer.background"); // Assure que la barre de défilement a le même fond
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        add(header);
        add(scroll);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new MigLayout("insets 3"));
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Drawer.background"); // Même fond que le Drawer

        cmdMenu = createButton(new FlatSVGIcon("icon/menu.svg"));
        cmdUndo = createButton(new FlatSVGIcon("icon/undo.svg"));
        cmdRedo = createButton(new FlatSVGIcon("icon/redo.svg"));
        cmdRefresh = createButton(new FlatSVGIcon("icon/refresh.svg"));
        cmdMenu.addActionListener(e -> FormManager.showMenu());
        cmdUndo.addActionListener(e -> FormManager.undo());
        cmdRedo.addActionListener(e -> FormManager.redo());
        cmdRefresh.addActionListener(e -> FormManager.refresh());

        panel.add(cmdMenu);
        panel.add(cmdUndo);
        panel.add(cmdRedo);
        panel.add(cmdRefresh);
        return panel;
    }

    private JButton createButton(Icon icon) {
        JButton button = new JButton(icon);
        button.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Button.toolbar.background;"
                + "arc:10;"
                + "margin:3,3,3,3;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");
        return button;
    }

    public void showForm(Component component, SliderTransition transition) {
        checkButton();
        panelSlider.addSlide(component, transition);
        revalidate();
    }

    public void showForm(Component component) {
        showForm(component, SimpleTransition.getDefaultTransition(false));
    }

    public void setForm(Component component) {
        checkButton();
        panelSlider.addSlide(component, null);
    }

    private void checkButton() {
        cmdUndo.setEnabled(FormManager.getForms().isUndoAble());
        cmdRedo.setEnabled(FormManager.getForms().isRedoAble());
        cmdRefresh.setEnabled(FormManager.getForms().getCurrent() != null);
    }
}