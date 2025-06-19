# GestionProjetsSwing

**GestionProjetsSwing** est une application Java Swing de gestion de projets collaboratifs, avec notifications en temps réel, gestion d'équipe, tâches, invitations, etc.

## 🚀 Fonctionnalités principales

- Authentification via un backend REST (déjà déployé)
- Gestion de projets (création, suppression, édition)
- Gestion des tâches et des collaborateurs
- Notifications et invitations en temps réel (WebSocket)
- Interface moderne avec FlatLaf
- Prise en charge du mode sombre/clair

## 🔑 Configuration des secrets et variables d'environnement

Certaines fonctionnalités (authentification Google, Firebase, etc.) nécessitent des clés/API et credentials qui **ne sont pas dans le dépôt** pour des raisons de sécurité.

### 1. Fichier `.env`

Crée un fichier `.env` à la racine du projet (ou dans `src/main/resources/` selon ton usage) à partir du modèle suivant :

```env
# env.example

GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
FIREBASE_API_KEY=your-firebase-api-key
FIREBASE_AUTH_DOMAIN=your-firebase-auth-domain
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_STORAGE_BUCKET=your-firebase-storage-bucket
FIREBASE_MESSAGING_SENDER_ID=your-firebase-messaging-sender-id
FIREBASE_APP_ID=your-firebase-app-id
FIREBASE_MEASUREMENT_ID=your-firebase-measurement-id
```

**Ne jamais commiter ce fichier `.env` avec des vraies valeurs sur GitHub.**

### 2. Credentials Firebase Admin

Pour les fonctionnalités d'administration Firebase côté serveur, place le fichier de credentials (ex: `gestionprojetsswing-firebase-adminsdk-xxxxxx.json`) dans le dossier approprié (`src/main/resources/` ou autre selon le code).  
**Ce fichier ne doit jamais être commité.**

- Pour obtenir ce fichier :  
  - Va dans la [console Firebase](https://console.firebase.google.com/),  
  - Paramètres du projet > Comptes de service > Générer une nouvelle clé privée.

### 3. Nettoyage des secrets déjà committés

Si tu as déjà commité des secrets, **change-les immédiatement** dans la console Google Cloud/Firebase/GitHub, puis réécris l'historique git si besoin (voir [GitHub docs](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository)).

---

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

- **Variables d'environnement obligatoires** : voir section [Configuration des secrets et variables d'environnement](#-configuration-des-secrets-et-variables-denvironnement)
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

