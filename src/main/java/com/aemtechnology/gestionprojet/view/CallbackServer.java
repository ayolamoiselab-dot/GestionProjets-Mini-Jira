/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aemtechnology.gestionprojet.view;
import fi.iki.elonen.NanoHTTPD;
/**
 *
 * @author aemtechnology
 */
public class CallbackServer extends NanoHTTPD {
    private final LoginView loginView;
    private final String provider;

    public CallbackServer(LoginView loginView, String provider) {
        super(8080); // Port local
        this.loginView = loginView;
        this.provider = provider;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String code = session.getParms().get("code");
        if (code != null) {
            // Envoyer le code au backend
            new Thread(() -> loginView.exchangeCodeForToken(provider, code)).start();
            return newFixedLengthResponse("Connexion réussie ! Fermez cette fenêtre.");
        }
        return newFixedLengthResponse("Erreur: code manquant");
    }
}
