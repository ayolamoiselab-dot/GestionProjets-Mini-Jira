package com.aemtechnology.gestionprojet.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;

public class LoadingDialog extends JDialog {

    private JLabel loadingLabel;
    private Timer rotationTimer;
    private double angle = 0;
    private ImageIcon spinnerIcon;

    public LoadingDialog(JFrame parent) {
        super(parent, "Chargement", false); // Rendre la fenêtre NON modale
        setUndecorated(true); // Pas de bordure de fenêtre
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Créer un panneau avec un fond semi-transparent
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Charger une icône de spinner
        spinnerIcon = loadIcon("/icons/spinner.png", 40, 40);
        if (spinnerIcon == null) {
            spinnerIcon = new ImageIcon(); // Fallback si l'icône n'est pas trouvée
        }

        // Label pour le spinner
        JLabel spinnerLabel = new JLabel(spinnerIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AffineTransform transform = new AffineTransform();
                transform.rotate(angle, getWidth() / 2.0, getHeight() / 2.0);
                g2.setTransform(transform);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        spinnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Animation de rotation du spinner
        rotationTimer = new Timer(50, e -> {
            angle += Math.toRadians(10); // Rotation de 10 degrés à chaque étape
            spinnerLabel.repaint();
        });
        rotationTimer.start();

        // Label pour le message
        loadingLabel = new JLabel("Chargement en cours...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(spinnerLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(loadingLabel);

        add(panel);
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        try {
            java.net.URL imgURL = getClass().getResource(path);
            if (imgURL != null) {
                Image image = ImageIO.read(imgURL);
                if (image != null) {
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImage);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'icône : " + path + " - " + e.getMessage());
        }
        return null;
    }

    @Override
    public void dispose() {
        if (rotationTimer != null && rotationTimer.isRunning()) {
            rotationTimer.stop();
        }
        super.dispose();
    }
}
