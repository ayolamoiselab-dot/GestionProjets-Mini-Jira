package com.aemtechnology.gestionprojet.view;

import fi.iki.elonen.NanoHTTPD;
import javax.swing.SwingUtilities;
import java.io.IOException;

public class AuthServer extends NanoHTTPD {
    private static AuthServer instance = null;

    // Constructeur privé pour empêcher l'instanciation directe
    private AuthServer() throws IOException {
        super(8080); // Port 8080
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Serveur démarré sur http://localhost:8080");
    }

    // Méthode pour obtenir l'instance unique (singleton)
    public static synchronized AuthServer getInstance() throws IOException {
        if (instance == null) {
            instance = new AuthServer();
        }
        return instance;
    }

    // Méthode pour arrêter le serveur
    public void stopServer() {
        stop();
        instance = null; // Réinitialiser l'instance pour permettre un redémarrage ultérieur
        System.out.println("Serveur NanoHTTPD arrêté.");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri.startsWith("/auth-success")) {
            String uid = session.getParms().get("uid");
            String fullName = session.getParms().get("fullName");
            String email = session.getParms().get("email");
            // Mettre à jour l'interface Swing avec les infos
            SwingUtilities.invokeLater(() -> {
                new HomeScreen(uid, fullName, email).setVisible(true); // À adapter selon ton code
            });
            return newFixedLengthResponse("Authentification réussie ! Fermez cette fenêtre.");
        }
        return newFixedLengthResponse("Erreur : URL invalide.");
    }
}