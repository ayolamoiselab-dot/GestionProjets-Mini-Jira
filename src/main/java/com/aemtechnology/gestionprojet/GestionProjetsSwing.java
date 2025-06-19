package com.aemtechnology.gestionprojet;

import com.aemtechnology.gestionprojet.config.FirebaseInitializer;
import com.aemtechnology.gestionprojet.view.OnboardingView; // Importer OnboardingView
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public class GestionProjetsSwing {
    public static void main(String[] args) {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("raven.theme");
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        UIManager.put("Drawer.background", new Color(30, 30, 40));
        FlatMacDarkLaf.setup();

        FirebaseInitializer.initialize();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bienvenue dans TeamWork@Mini-Jira");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new OnboardingView()); // Afficher OnboardingView au lieu de LoginView
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}