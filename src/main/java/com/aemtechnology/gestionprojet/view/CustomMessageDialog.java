package com.aemtechnology.gestionprojet.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CustomMessageDialog extends JDialog {
    private JLabel titleLabel;
    private JTextPane messageText;
    private JButton okButton, cancelButton;
    private JPanel contentPanel;

    public CustomMessageDialog(Frame parent, String title, String message) {
        super(parent, title, true); // Modale
        setUndecorated(true); // Supprime la décoration par défaut
        setSize(400, 200);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Panneau de contenu avec effet glassmorphism
        contentPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 230)); // Fond semi-transparent
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        add(contentPanel, BorderLayout.CENTER);

        // Titre
        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(80, 80, 80));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Message
        messageText = new JTextPane();
        messageText.setEditable(false);
        messageText.setForeground(new Color(133, 133, 133));
        messageText.setText(message);
        messageText.setOpaque(false);
        messageText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(messageText, BorderLayout.CENTER);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        okButton = new JButton("OK") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(48, 170, 63));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        okButton.setOpaque(false);
        okButton.setContentAreaFilled(false);

        cancelButton = new JButton("Annuler") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(233, 233, 233));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        cancelButton.setOpaque(false);
        cancelButton.setContentAreaFilled(false);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Animation de fondu au démarrage
        setOpacity(0f);
        Timer fadeInTimer = new Timer(50, new ActionListener() {
            float opacity = 0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.05f;
                if (opacity >= 1f) {
                    opacity = 1f;
                    ((Timer) e.getSource()).stop();
                }
                setOpacity(opacity);
            }
        });
        fadeInTimer.start();
    }

    public void eventOK(ActionListener event) {
        okButton.addActionListener(event);
    }

    public static void showMessage(Component parent, String title, String message) {
        JDialog dialog = new CustomMessageDialog(
            (Frame) SwingUtilities.getWindowAncestor(parent), title, message
        );
        dialog.setVisible(true);
    }
}