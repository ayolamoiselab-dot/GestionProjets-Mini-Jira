package com.aemtechnology.gestionprojet.swing.slider;

import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.VolatileImage;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

public class PanelSlider extends JLayeredPane {

    private PanelSnapshot panelSnapshot;
    private Component component;
    private Component oldComponent;
    private CardLayout cardLayout;
    private int componentCounter = 0; // Pour générer des noms uniques

    public PanelSlider() {
        init();
    }

    private void init() {
        setOpaque(true);
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        panelSnapshot = new PanelSnapshot();
        setLayer(panelSnapshot, JLayeredPane.DRAG_LAYER);
        add(panelSnapshot, "snapshot");
        panelSnapshot.setVisible(false);
    }

    public void addSlide(Component component, SliderTransition transition) {
        component.applyComponentOrientation(getComponentOrientation());
        if (this.component != null) {
            this.oldComponent = this.component;
        }
        this.component = component;

        // Vérifier si le composant est déjà dans le conteneur
        boolean componentExists = false;
        String componentName = null;
        for (Component comp : getComponents()) {
            if (comp == component) {
                componentExists = true;
                componentName = comp.getName();
                break;
            }
        }

        if (!componentExists) {
            componentName = "component_" + componentCounter++;
            component.setName(componentName);
            add(component, componentName);
            setLayer(component, JLayeredPane.DEFAULT_LAYER);
        }

        // S'assurer que le composant est visible
        component.setVisible(true);

        if (oldComponent == null) {
            revalidate();
            repaint();
        } else {
            if (transition != null) {
                doLayout();
                final String finalComponentName = componentName;
                SwingUtilities.invokeLater(() -> {
                    Image oldImage = createImage(oldComponent);
                    Image newImage = createImage(component);
                    if (oldComponent != null) {
                        oldComponent.setVisible(false);
                        remove(oldComponent);
                    }
                    panelSnapshot.animate(transition, oldImage, newImage);
                    cardLayout.show(this, finalComponentName);
                    revalidate();
                    repaint();
                });
            } else {
                if (oldComponent != null) {
                    oldComponent.setVisible(false);
                    remove(oldComponent);
                }
                cardLayout.show(this, componentName);
                revalidate();
                repaint();
            }
        }
    }

    public Image createImage(Component component) {
        boolean check = false;
        for (Component comp : getComponents()) {
            if (comp == component) {
                check = true;
                break;
            }
        }
        String tempName = "temp_" + componentCounter++;
        if (!check) {
            add(component, tempName);
            setLayer(component, JLayeredPane.DEFAULT_LAYER);
        }
        VolatileImage snapshot = component.createVolatileImage(getWidth(), getHeight());
        if (snapshot == null) {
            return null;
        }
        component.paint(snapshot.getGraphics());
        if (!check) {
            remove(component);
        }
        return snapshot;
    }

    public Image createOldImage() {
        if (oldComponent != null) {
            return createImage(oldComponent);
        }
        return null;
    }

    private class PanelSnapshot extends JComponent {

        private final Animator animator;
        private float animate;
        private SliderTransition sliderTransition;
        private Image oldImage;
        private Image newImage;

        public PanelSnapshot() {
            animator = new Animator(400, new Animator.TimingTarget() {
                @Override
                public void timingEvent(float v) {
                    animate = v;
                    repaint();
                }

                @Override
                public void end() {
                    if (sliderTransition.closeAfterAnimation()) {
                        setVisible(false);
                        if (oldImage != null) oldImage.flush();
                        if (newImage != null) newImage.flush();
                    }
                    component.setVisible(true);
                    revalidate();
                    repaint();
                }
            });
            animator.setInterpolator(CubicBezierEasing.EASE);
        }

        protected void animate(SliderTransition sliderTransition, Image oldImage, Image newImage) {
            if (animator.isRunning()) {
                animator.stop();
            }
            this.oldImage = oldImage;
            this.newImage = newImage;
            this.sliderTransition = sliderTransition;
            this.animate = 0f;
            repaint();
            setVisible(true);
            animator.start();
        }

        @Override
        public void paint(Graphics g) {
            if (sliderTransition != null) {
                int width = getWidth();
                int height = getHeight();
                sliderTransition.render(this, g, oldImage, newImage, width, height, animate);
            }
        }
    }
}