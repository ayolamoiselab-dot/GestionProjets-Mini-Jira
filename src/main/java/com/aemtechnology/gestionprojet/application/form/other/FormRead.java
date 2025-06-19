package com.aemtechnology.gestionprojet.application.form.other;

import javax.swing.*;
import java.awt.*;
import org.json.JSONArray;

public class FormRead extends JPanel {

    private final String fullName;
    private final String email;

    public FormRead(String fullName, String email) {
        this.fullName = fullName != null && !fullName.trim().isEmpty() ? fullName : "Utilisateur";
        this.email = email != null && !email.trim().isEmpty() ? email : "email@example.com";
        init();
    }

    private void init() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JLabel message = new JLabel("Section de lecture désactivée. Consultez vos notifications dans Inbox.");
        message.setHorizontalAlignment(SwingConstants.CENTER);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        message.setForeground(Color.WHITE);
        add(message, BorderLayout.CENTER);
    }

    // Méthode vide pour éviter toute mise à jour
    public void updateNotifications(JSONArray notifications) {
        // Ne rien faire, toute la gestion est dans FormInbox
    }



    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Read");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lb;
    // End of variables declaration//GEN-END:variables
}

