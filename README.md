# GestionProjetsSwing

**GestionProjetsSwing** est une application Java Swing de gestion de projets collaboratifs, avec notifications en temps réel, gestion d'équipe, tâches, invitations, etc.

## 🚀 Fonctionnalités principales

- Authentification via un backend REST (déjà déployé)
- Gestion de projets (création, suppression, édition)
- Gestion des tâches et des collaborateurs
- Notifications et invitations en temps réel (WebSocket)
- Interface moderne avec FlatLaf
- Prise en charge du mode sombre/clair

## 🏁 Point d'entrée

Le programme principal est :
```
src/main/java/com/aemtechnology/gestionprojet/GestionProjetsSwing.java
```

## 📦 Prérequis

- **Java 17** ou supérieur ([OpenJDK](https://adoptium.net/) recommandé)
- **Maven 3.6+**
- Connexion Internet (pour accéder au backend déployé)

## ⚙️ Installation & Exécution

1. **Cloner le dépôt**
   ```sh
   git clone <url-du-repo>
   cd GestionProjetsSwing
   ```

2. **Compiler le projet**
   ```sh
   mvn clean install
   ```

3. **Exécuter l'application**
   ```sh
   mvn exec:java -Dexec.mainClass="com.aemtechnology.gestionprojet.GestionProjetsSwing"
   ```
   ou, après compilation :
   ```sh
   java -cp target/classes com.aemtechnology.gestionprojet.GestionProjetsSwing
   ```

## 🗂️ Structure du projet

- `src/main/java/com/aemtechnology/gestionprojet/` : code source principal
- `GestionProjetsSwing.java` : point d'entrée
- `view/`, `forms/`, `components/`, `menu/` : organisation par fonctionnalités
- `resources/` : ressources (icônes, thèmes, etc.)

## 🔗 Backend/API

- **Aucune installation locale du backend requise.**
- Le backend est déjà déployé sur [Render](https://teamworkatmini-jira.onrender.com).
- **Attention :** Lors de la première requête, il peut y avoir un délai (30-60s) car Render "réveille" le service (plan gratuit).
- Toutes les requêtes API et WebSocket sont déjà configurées pour pointer vers ce backend.

## 📝 Configuration

- **Aucune variable d'environnement n'est requise côté client.**
- Les identifiants de connexion sont à créer via l'interface (ou demander à un admin).
- Les URLs du backend sont codées en dur dans le code source (`https://teamworkatmini-jira.onrender.com`).

## 💡 Conseils d'utilisation

- **Patientez** lors du premier lancement ou après une longue inactivité (backend Render).
- Pour toute erreur de connexion, vérifiez votre accès Internet.
- Les notifications et invitations sont en temps réel grâce au WebSocket.
- Pour changer le thème (clair/sombre), utilisez le menu latéral.

## 🛠️ Développement

- Pour modifier le code, ouvrez le projet dans votre IDE Java favori (IntelliJ, Eclipse, NetBeans...).
- Les dépendances sont gérées par Maven (`pom.xml`).
- Pour ajouter des ressources (icônes, thèmes), placez-les dans `src/main/resources/`.

## ❓ FAQ

- **Q : Dois-je installer le backend ?**  
  R : Non, il est déjà déployé. Vous pouvez l'utiliser tel quel.

- **Q : Comment changer l'URL du backend ?**  
  R : Modifiez les URLs dans le code source si besoin (recherche `teamworkatmini-jira.onrender.com`).

- **Q : Où sont stockées les données ?**  
  R : Toutes les données sont gérées côté backend (MongoDB, etc.).

## 📄 Licence

Projet académique, usage libre pour tests et apprentissage.

---

**Contact** : AYOLA Essoréou Moise - moiseayola4@gmail.com

