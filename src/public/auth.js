// Importer les modules Firebase nécessaires
import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.6.10/firebase-app.js';
import { getAuth, signInWithPopup, GoogleAuthProvider, GithubAuthProvider } from 'https://www.gstatic.com/firebasejs/9.6.10/firebase-auth.js';

// Configuration Firebase (identique à celle de ton auth.html)
const firebaseConfig = {
    apiKey: "AIzaSyDib42OIcXpJDePgJea920plc2hrKX0L1Y",
    authDomain: "gestionprojetsswing.firebaseapp.com",
    projectId: "gestionprojetsswing",
    storageBucket: "gestionprojetsswing.appspot.com",
    messagingSenderId: "536336958411",
    appId: "1:536336958411:web:593851c4a0c8dfaf42ac4e",
    measurementId: "G-XM33EWQLND"
};

// Initialiser Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

// Fonction pour gérer l'authentification
function handleAuth() {
    // Récupérer le provider depuis l'URL
    const urlParams = new URLSearchParams(window.location.search);
    const provider = urlParams.get('provider');

    if (provider) {
        let authProvider;
        if (provider === "google") {
            authProvider = new GoogleAuthProvider();
        } else if (provider === "github") {
            authProvider = new GithubAuthProvider();
        }

        signInWithPopup(auth, authProvider)
            .then((result) => {
                const user = result.user;
                const uid = user.uid;
                const fullName = user.displayName || "Utilisateur";
                // Rediriger vers ton backend sur Render
                window.location.href = `https://gestprojetsswing-backend.onrender.com/auth-success?uid=${uid}&fullName=${encodeURIComponent(fullName)}`;
            })
            .catch((error) => {
                alert("Erreur d'authentification : " + error.message);
            });
    } else {
        alert("Provider non spécifié.");
    }
}

// Appeler la fonction immédiatement
handleAuth();