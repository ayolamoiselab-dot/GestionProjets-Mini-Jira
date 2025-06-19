//package com.aemtechnology.gestionprojet.view;
//
//import org.java_websocket.client.WebSocketClient;
//import org.java_websocket.handshake.ServerHandshake;
//import org.json.JSONObject;
//import org.json.JSONException;
//
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Consumer;
//
//public class WebSocketClient {
//
//    // Instance unique (Singleton)
//    private static WebSocketClient instance;
//
//    private WebSocketClientImpl webSocket;
//    private String uid; // L'UID de l'utilisateur connecté
//    private boolean isConnected = false;
//    // Liste des listeners (écrans ou composants qui veulent recevoir les notifications)
//    private List<Consumer<JSONObject>> notificationListeners = new ArrayList<>();
//
//    // Constructeur privé pour empêcher l'instanciation directe
//    private WebSocketClient(String uid) {
//        this.uid = uid;
//        connect();
//    }
//
//    // Méthode pour obtenir l'instance unique (Singleton)
//    public static WebSocketClient getInstance(String uid) {
//        if (instance == null || !instance.uid.equals(uid)) {
//            instance = new WebSocketClient(uid) {
//                @Override
//                public void onOpen(ServerHandshake sh) {
//                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//                }
//
//                @Override
//                public void onMessage(String string) {
//                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//                }
//
//                @Override
//                public void onClose(int i, String string, boolean bln) {
//                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//                }
//
//                @Override
//                public void onError(Exception excptn) {
//                    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//                }
//            };
//        }
//        return instance;
//    }
//
//    // Méthode pour ajouter un listener (un écran qui veut recevoir les notifications)
//    public void addNotificationListener(Consumer<JSONObject> listener) {
//        notificationListeners.add(listener);
//    }
//
//    // Méthode pour retirer un listener (par exemple, quand un écran est détruit)
//    public void removeNotificationListener(Consumer<JSONObject> listener) {
//        notificationListeners.remove(listener);
//    }
//
//    private void connect() {
//        try {
//            // URL du WebSocket (utilise wss:// pour Render.com, ws:// pour local)
//            String wsUrl = "wss://teamworkatmini-jira.onrender.com/ws";
//            // Si tu testes en local, utilise : String wsUrl = "ws://localhost:8080/ws";
//
//            webSocket = new WebSocketClientImpl(new URI(wsUrl));
//            webSocket.connect();
//        } catch (Exception e) {
//            System.err.println("Erreur lors de la connexion WebSocket : " + e.getMessage());
//        }
//    }
//
//    public void disconnect() {
//        if (webSocket != null) {
//            webSocket.close();
//            isConnected = false;
//            instance = null; // Réinitialiser l'instance pour permettre une nouvelle connexion avec un autre UID
//        }
//    }
//
//    public boolean isConnected() {
//        return isConnected;
//    }
//
//    // Classe interne pour implémenter WebSocketClient
//    private class WebSocketClientImpl extends WebSocketClient {
//
//        public WebSocketClientImpl(URI serverUri) {
//            super(serverUri);
//        }
//
//        public void onOpen(ServerHandshake handshakedata) {
//            System.out.println("Connexion WebSocket établie !");
//            isConnected = true;
//            // S'abonner au canal des notifications via un message STOMP
//            String subscribeMessage = "SUBSCRIBE\nid:sub-0\ndestination:/topic/notifications/" + uid + "\n\n\0";
//            send(subscribeMessage);
//        }
//
//        public void onMessage(String message) {
//            System.out.println("Message WebSocket reçu : " + message);
//            // Traiter le message STOMP
//            if (message.contains("MESSAGE")) {
//                // Extraire le corps du message (la notification)
//                String[] lines = message.split("\n");
//                StringBuilder notificationBody = new StringBuilder();
//                boolean isBody = false;
//                for (String line : lines) {
//                    if (isBody) {
//                        notificationBody.append(line);
//                    }
//                    if (line.trim().isEmpty()) {
//                        isBody = true;
//                    }
//                }
//                String notificationMessage = notificationBody.toString().trim();
//                if (notificationMessage.endsWith("\0")) {
//                    notificationMessage = notificationMessage.substring(0, notificationMessage.length() - 1);
//                }
//                try {
//                    JSONObject notificationJson = new JSONObject(notificationMessage);
//                    // Notifier tous les listeners
//                    for (Consumer<JSONObject> listener : notificationListeners) {
//                        listener.accept(notificationJson);
//                    }
//                } catch (JSONException e) {
//                    System.err.println("Erreur lors du parsing de la notification : " + e.getMessage());
//                }
//            }
//        }
//
//        public void onClose(int code, String reason, boolean remote) {
//            System.out.println("WebSocket fermé : " + code + " - " + reason);
//            isConnected = false;
//            // Tenter une reconnexion après 5 secondes
//            new Thread(() -> {
//                try {
//                    Thread.sleep(5000);
//                    System.out.println("Tentative de reconnexion WebSocket...");
//                    connect();
//                } catch (InterruptedException e) {
//                    System.err.println("Erreur lors de la reconnexion : " + e.getMessage());
//                }
//            }).start();
//        }
//
//        public void onError(Exception ex) {
//            System.err.println("Erreur WebSocket : " + ex.getMessage());
//            isConnected = false;
//        }
//    }
//}