package com.aemtechnology.gestionprojet.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import net.miginfocom.swing.MigLayout;

public class OnboardingView extends JPanel {

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private int currentSlide = 0;
    private IndicatorPanel indicatorPanel;
    private JButton nextButton;
    private JButton finishButton;
    private JButton skipButton;
    private final int totalSlides = 3;
    private boolean darkMode = true; // Ajout de la variable darkMode
    private JLabel toggleModeLabel; // Bouton pour basculer le mode

    public OnboardingView() {
        FlatLightLaf.setup();

        setPreferredSize(new Dimension(1000, 700));
        setLayout(new BorderLayout());

        // Ajouter un panneau pour le bouton de basculement (en haut à droite)
        JPanel topBar = new JPanel(new MigLayout("insets 10", "push[]", ""));
        topBar.setOpaque(false);
        toggleModeLabel = new JLabel("\u263D"); // Symbole de la lune
        toggleModeLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        toggleModeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleModeLabel.setForeground(Color.BLACK);
        toggleModeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleDarkMode();
            }
        });
        topBar.add(toggleModeLabel);
        add(topBar, BorderLayout.NORTH);

        JPanel backgroundPanel = new JPanel();
        backgroundPanel.setBackground(new Color(173, 216, 230));
        backgroundPanel.setLayout(new BorderLayout());
        add(backgroundPanel, BorderLayout.CENTER);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        backgroundPanel.add(cardPanel, BorderLayout.CENTER);

        addSlide("Slide 1", "Bienvenue sur TeamWork@Mini-Jira", "Gérez vos projets avec simplicité", "Créez, organisez et suivez vos projets en un seul endroit.", "/images/slide1.jpeg");
        addSlide("Slide 2", "Collaborez efficacement", "Travaillez en équipe", "Partagez des tâches et collaborez en temps réel avec vos collègues.", "/images/slide2.png");
        addSlide("Slide 3", "Suivez vos progrès", "Restez informé", "Visualisez l'avancement de vos tâches avec des graphiques dynamiques.", "/images/slide3.jpeg");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        skipButton = createModernButton("Passer", false);
        skipButton.addActionListener(e -> navigateToLogin());
        bottomPanel.add(skipButton, BorderLayout.WEST);

        indicatorPanel = new IndicatorPanel(totalSlides);
        bottomPanel.add(indicatorPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);
        nextButton = createModernButton("Suivant", true);
        finishButton = createModernButton("Terminer", true);
        finishButton.setVisible(false);
        nextButton.addActionListener(e -> goToNextSlide());
        finishButton.addActionListener(e -> navigateToLogin());
        rightPanel.add(nextButton);
        rightPanel.add(finishButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        Component comp = cardPanel.getComponent(0);
        if (comp instanceof AnimatedPanel) {
            ((AnimatedPanel) comp).startAnimation();
        }
        indicatorPanel.setCurrentSlide(currentSlide);
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        if (darkMode) {
            toggleModeLabel.setText("\u2600"); // Symbole du soleil
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.themes.FlatMacDarkLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            toggleModeLabel.setText("\u263D"); // Symbole de la lune
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
        revalidate();
        repaint();
    }

    private void goToNextSlide() {
        if (currentSlide < totalSlides - 1) {
            animateSlideTransition(() -> {
                currentSlide++;
                cardLayout.next(cardPanel);
                Component comp = cardPanel.getComponent(currentSlide);
                if (comp instanceof AnimatedPanel) {
                    ((AnimatedPanel) comp).startAnimation();
                }
                indicatorPanel.setCurrentSlide(currentSlide);
                if (currentSlide == totalSlides - 1) {
                    nextButton.setVisible(false);
                    finishButton.setVisible(true);
                }
            });
        }
    }

    private void animateSlideTransition(Runnable callback) {
        AnimatedPanel currentPanel = (AnimatedPanel) cardPanel.getComponent(currentSlide);
        Timer timer = new Timer(15, null);
        timer.addActionListener(new ActionListener() {
            float alpha = 1f;

            @Override
            public void actionPerformed(ActionEvent e) {
                alpha -= 0.08f;
                if (alpha <= 0f) {
                    alpha = 0f;
                    timer.stop();
                    callback.run();
                }
                currentPanel.setAnimationAlpha(alpha);
                currentPanel.repaint();
            }
        });
        timer.start();
    }

    private void navigateToLogin() {
        try {
            AuthServer.getInstance();
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur local : " + e.getMessage());
            return;
        }

        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.getContentPane().removeAll();

        // Créer une nouvelle instance de LoginView
        LoginView loginView = new LoginView();

        // Synchroniser le mode sombre (optionnel, si OnboardingView avait un mode sombre)
        // Par défaut, OnboardingView utilise FlatLightLaf, donc on suppose mode clair
        // Si tu veux ajouter un mode sombre à OnboardingView, tu peux ajouter une variable darkMode ici
        // loginView.setDarkMode(darkMode);
        // Appliquer un fond au JFrame qui correspond au thème de LoginView
        frame.setBackground(new Color(173, 216, 230)); // Même couleur que OnboardingView pour la cohérence

        // Appliquer les styles au JFrame (similaire à ceux de LoginView)
//        frame.putClientProperty(FlatClientProperties.STYLE, ""
//                + "arc:20;"
//                + "[light]background:darken(@background,3%);"
//                + "[dark]background:lighten(@background,3%)");
        // Définir le nouveau contenu
        frame.setContentPane(loginView);
        frame.pack();

        // Définir une taille minimale pour le JFrame
        frame.setMinimumSize(new Dimension(800, 600)); // Ajuste selon tes besoins

        // Centrer la fenêtre sur l'écran
        frame.setLocationRelativeTo(null);

        frame.revalidate();
        frame.repaint();
    }

    private JButton createModernButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text);
        if (isPrimary) {
            btn.setBackground(new Color(0, 120, 215));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(220, 220, 220));
            btn.setForeground(new Color(50, 50, 50));
        }
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setOpaque(true);
        btn.setBorderPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(isPrimary ? new Color(0, 100, 180) : new Color(200, 200, 200));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 90, 160), 1),
                        BorderFactory.createEmptyBorder(11, 24, 11, 24)));
                btn.setSize(btn.getWidth() + 5, btn.getHeight() + 5);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(isPrimary ? new Color(0, 120, 215) : new Color(220, 220, 220));
                btn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
                btn.setSize(btn.getWidth() - 5, btn.getHeight() - 5);
            }
        });
        return btn;
    }

    private void addSlide(String slideName, String title, String header, String description, String imagePath) {
        AnimatedPanel slide = new AnimatedPanel();
        slide.setLayout(new BorderLayout(20, 20));
        slide.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        slide.setOpaque(false);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        titleLabel.setForeground(new Color(44, 62, 80));
        slide.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        slide.add(centerPanel, BorderLayout.CENTER);

        AnimatedImageLabel imageLabel = new AnimatedImageLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            InputStream is = getClass().getResourceAsStream(imagePath);
            BufferedImage img = ImageIO.read(is);
            if (img != null) {
                Image scaled = img.getScaledInstance(350, 350, Image.SCALE_SMOOTH); // Réduction de la taille à 400x400
                BufferedImage circularImage = makeRoundedImage(scaled);
                imageLabel.setIcon(new ImageIcon(circularImage));
            } else {
                imageLabel.setText("Image introuvable");
            }
        } catch (IOException ex) {
            imageLabel.setText("Erreur de chargement");
        }
        slide.setImageLabel(imageLabel);
        centerPanel.add(imageLabel, BorderLayout.CENTER);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        textPanel.setOpaque(false);
        JLabel headerLabel = new JLabel(header, SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        headerLabel.setForeground(new Color(52, 73, 94));
        JLabel descLabel = new JLabel(description, SwingConstants.CENTER);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        descLabel.setForeground(new Color(127, 140, 141));
        textPanel.add(headerLabel);
        textPanel.add(descLabel);
        centerPanel.add(textPanel, BorderLayout.SOUTH);

        cardPanel.add(slide, slideName);
    }

    private BufferedImage makeRoundedImage(Image img) {
        int size = 350; // Réduction de la taille à 400x400
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(img, 0, 0, size, size, null);
        g2.dispose();
        return output;
    }

    class AnimatedPanel extends JPanel {

        private float alpha = 0f;
        private Timer animationTimer;
        private AnimatedImageLabel imageLabel;

        public AnimatedPanel() {
            setOpaque(false);
        }

        public void setImageLabel(AnimatedImageLabel imageLabel) {
            this.imageLabel = imageLabel;
        }

        public void startAnimation() {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            alpha = 0f;
            animationTimer = new Timer(20, null);
            animationTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    alpha += 0.05f;
                    if (alpha >= 1f) {
                        alpha = 1f;
                        animationTimer.stop();
                    }
                    repaint();
                    if (imageLabel != null) {
                        System.out.println("Déclenchement de l'animation de l'image pour le slide " + (currentSlide + 1));
                        imageLabel.startAnimation();
                    }
                }
            });
            animationTimer.start();
        }

        public void setAnimationAlpha(float alpha) {
            this.alpha = alpha;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    class AnimatedImageLabel extends JLabel {

        private float alpha = 0f;
        private float translateY = -50f;
        private Timer animationTimer;

        public AnimatedImageLabel() {
            setOpaque(false);
        }

        public void startAnimation() {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            alpha = 0f;
            translateY = -50f;

            System.out.println("Lancement de l'animation de l'image (fondu + glissement)");

            animationTimer = new Timer(20, null);
            animationTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    alpha += 0.04f;
                    translateY += 2f;
                    if (alpha >= 1f) {
                        alpha = 1f;
                        translateY = 0f;
                        animationTimer.stop();
                        System.out.println("Animation de l'image terminée");
                    }
                    System.out.println("Étape d'animation - Alpha: " + alpha + ", TranslateY: " + translateY);
                    repaint();
                }
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.translate(0, translateY);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    class IndicatorPanel extends JPanel {

        private int totalSlides;
        private int currentSlide;
        private float[] fillProgress;

        public IndicatorPanel(int totalSlides) {
            this.totalSlides = totalSlides;
            this.fillProgress = new float[totalSlides];
            setOpaque(false);
            setPreferredSize(new Dimension(150, 40));
        }

        public void setCurrentSlide(int slide) {
            this.currentSlide = slide;
            Timer timer = new Timer(15, null);
            timer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fillProgress[slide] += 0.06f;
                    if (fillProgress[slide] >= 1f) {
                        fillProgress[slide] = 1f;
                        timer.stop();
                    }
                    repaint();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int diameter = 15;
            int spacing = 20;
            int totalWidth = totalSlides * diameter + (totalSlides - 1) * spacing;
            int startX = (getWidth() - totalWidth) / 2;
            int y = getHeight() / 2 - diameter / 2;
            for (int i = 0; i < totalSlides; i++) {
                g2.setColor(new Color(189, 189, 189));
                g2.fillOval(startX + i * (diameter + spacing), y, diameter, diameter);
                if (i <= currentSlide) {
                    g2.setColor(new Color(0, 120, 215));
                    int fillHeight = (int) (diameter * fillProgress[i]);
                    g2.fillOval(startX + i * (diameter + spacing), y + diameter - fillHeight, diameter, fillHeight);
                }
            }
            g2.dispose();
        }
    }
}
