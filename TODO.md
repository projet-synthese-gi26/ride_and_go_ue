C'est une excellente étape structurante. L'intégration du service véhicule (Vehicle Service) ne doit pas être juste "technique", elle doit fluidifier le parcours utilisateur du Chauffeur.

Voici ma proposition de conception avant de coder :

### 1. Modélisation de l'association (Véhicule <-> Driver)

Le service Véhicule est le **maître** des données véhicules. Le service Ride & Go (notre backend actuel) ne doit stocker qu'une **référence** (ID) et éventuellement un **cache** léger pour l'affichage rapide (plaque, modèle, couleur).

*   **Vehicle Service (Distant)** : Gère `Vehicle`, `Ownership` (Propriété), `Model`, `Make`, etc.
*   **Ride & Go (Local)** :
    *   Dans la table `drivers`, on ajoute une colonne `vehicle_id` (UUID).
    *   Cette colonne fera le lien vers le véhicule actif du chauffeur.

**Pourquoi pas une table de liaison ?**
Pour le MVP VTC, un chauffeur utilise *un* véhicule actif à la fois pour travailler. Si on veut gérer une flotte (plusieurs véhicules par chauffeur), le `Vehicle Service` gère déjà les `Ownerships`. Ride & Go a juste besoin de savoir : "Avec quelle voiture il roule *maintenant* ?".

### 2. Flux d'Onboarding "Devenir Driver" (Modifié)

Actuellement, `POST /api/v1/users/driver` crée juste une entrée vide dans la table `drivers`.
Nous allons transformer ce processus en un flux complet avec formulaire Multipart (pour uploader les photos du véhicule).

**Le nouveau scénario :**

1.  **Frontend** : L'utilisateur remplit un formulaire "Devenir Chauffeur" :
    *   Infos perso (Permis, etc.)
    *   Infos véhicule (Marque, Modèle, Plaque, Couleur, Année)
    *   Photos (Permis recto/verso, Carte grise, Photo véhicule)

2.  **Backend (Ride & Go - `DriverController`)** :
    *   Reçoit le Multipart.
    *   **Étape 1 (Appel Vehicle Service)** : Appelle `POST /vehicles` sur le service distant pour créer le véhicule.
    *   **Étape 2 (Récupération ID)** : Récupère le `vehicleId` créé.
    *   **Étape 3 (Création Driver Local)** : Crée l'entrée dans la table `drivers` avec `vehicle_id` + `license_number` + `is_profile_completed = false` (en attente de validation admin, ou true si auto-validé).
    *   **Étape 4 (Propagation Rôle)** : Ajoute le rôle `DRIVER` dans Auth Service.

### 3. Intégration dans le reste de l'App (Offres & Courses)

Une fois le lien fait, l'expérience change :

*   **Affichage des Offres (`/bids`)** :
    *   Quand un chauffeur postule, on ne renvoie plus des "Fake Data" (Toyota Yaris grise...).
    *   On récupère son `vehicle_id` depuis la table `drivers`.
    *   On appelle le `Vehicle Service` (ou on lit un cache Redis) pour avoir : Marque, Modèle, Couleur, Plaque, Photo.
    *   Le passager voit donc la *vraie* voiture qui va venir.

### Résumé des changements techniques à prévoir

1.  **Client API** : Créer `VehicleApiClient` pour parler au service externe (authentifié avec le token utilisateur ou service).
2.  **Adapter** : Créer `RemoteVehicleAdapter` pour gérer la logique d'appel (et potentiellement le cache).
3.  **Base de données** : Ajouter `vehicle_id` dans la table `drivers` (Liquibase).
4.  **DTOs** : Mettre à jour `RegisterDriverRequest` pour inclure les données véhicule (Multipart).
5.  **Service** : Mettre à jour `UserService.upgradeToDriver` pour orchestrer la création véhicule -> création driver.
6.  **Offre** : Mettre à jour `OfferService.getOfferWithEnrichedBids` pour aller chercher les vraies infos véhicule.

Est-ce que cette approche te convient ? Si oui, je lance la génération du code.