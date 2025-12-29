# 📄 Spécifications Techniques : API RIDE & GO (Sprint Express)

## 1. Présentation du projet
**Ride & Go** est une marketplace de transport urbain (VTC/Taxi). 
*   **Le Passager (Client)** : Calcule un coût estimé, publie une offre de trajet.
*   **Le Chauffeur (Driver)** : Parcourt les offres disponibles et postule (Bidding).
*   **Le Système** : Gère la transition de l'offre vers une course réelle (Trip) une fois le chauffeur validé.
*   **Stack** : Backend Spring Boot (Architecture Hexagonale) & Frontend React Native.

---

## 2. Modélisation des données & États (English)

### A. Offer States (Annonce client)
1.  `PENDING` : Offre créée, en attente de chauffeurs.
2.  `BID_RECEIVED` : Au moins un chauffeur a postulé.
3.  `DRIVER_SELECTED` : Le passager a choisi un chauffeur.
4.  `VALIDATED` : Le chauffeur a accepté le choix, l'offre devient un "Trip".
5.  `CANCELLED` : Annulée par le client ou expiration.

### B. Trip States (La course)
1.  `CREATED` : Course initialisée, chauffeur en route vers le client.
2.  `ONGOING` : Client récupéré, trajet en cours vers la destination.
3.  `COMPLETED` : Arrivée à destination, paiement confirmé.
4.  `CANCELLED` : Course interrompue.

---

Voici la section 3 mise à jour de façon exhaustive, en reprenant chaque action simple de ton brouillon et en y associant la route correspondante pour que ton binôme et toi sachiez exactement quoi appeler à chaque étape.

---

## 3. Flux Principal (User Journey & Route Mapping)

### 1. Auth : Identification des utilisateurs
*   **S'enregistrer** : Le chauffeur ou le passager crée son compte (`POST /api/auth/register`).
*   **Se connecter** : L'utilisateur accède à l'application et récupère son token (`POST /api/auth/login`).

### 2. Estimate : Préparation du trajet (Côté Client)
*   **Calculer le coût** : Le client saisit son trajet pour obtenir une estimation via le service externe (`POST /api/fares/estimate`).

    Endpoint principal d'estimation de prix.
    
    **Flexibilité des paramètres :**
    - Les coordonnées (`lat`/`lon`) sont **optionnelles** si un nom de lieu (`label`) est fourni.
    - L'API effectuera un géocodage automatique si nécessaire.
    - Les paramètres `heure`, `meteo`, `type_zone` sont **optionnels** (détectés automatiquement si omis).
    
    **Exemple minimaliste (Noms de lieux uniquement) :**
    ```json
    {
        "depart": {"label": "Poste Centrale"},
        "arrivee": {"label": "Mvan"}
    }
    ```
    response:

    {
"depart": "string",
"arrivee": "string",
"heure": "matin",
"meteo": 3,
"type_zone": 2,
"congestion_user": 10
}

*   **Visualiser l'estimation** : Le système renvoie le prix suggéré et la distance pour aider le client à fixer son offre.

### 3. Post Offer : Publication (Côté Client)
*   **Créer l'offre** : Le client publie son offre avec le prix définitif (`POST /api/offers`). -> État `PENDING`.
*   **Notifier les chauffeurs** : Le système rend l'offre visible pour les chauffeurs aux alentours. 

### 4. Bidding : Manifestation d'intérêt (Côté Chauffeur)
*   **Recevoir les clients** : Le chauffeur consulte la liste des offres/clients disponibles dans sa zone (`GET /api/offers/available`).
*   **Postuler à une offre** : Le chauffeur choisit une offre et envoie sa candidature (`POST /api/offers/{id}/apply`). -> État `BID_RECEIVED`.

### 5. Selection : Validation du binôme (Côté Client)
*   **Recevoir les chauffeurs** : Le client consulte la liste de tous les chauffeurs qui ont postulé à son offre (`GET /api/offers/{id}/bids`).
*   **Valider un chauffeur** : Le client choisit son chauffeur préféré parmi la liste (`PATCH /api/offers/{id}/select-driver`). -> État `DRIVER_SELECTED`.

C'est un point crucial pour l'expérience utilisateur. Voici le bloc **6. Trip Execution** mis à jour pour intégrer explicitement le suivi GPS pendant la phase d'approche (quand le chauffeur vient chercher le client) :

---

### 6. Trip Execution : Déroulement de la course

*   **Initialiser la course** : Suite à la sélection, le système crée le contexte de trajet (`POST /api/trips`). -> État **`CREATED`**.
*   **Phase d'approche (Tracking)** : Pendant que le chauffeur se déplace vers le point de départ pour récupérer le client :
    *   **Envoi GPS (Chauffeur)** : Le chauffeur envoie sa position GPS en temps réel pour signaler sa progression (`POST /api/trips/{id}/location`).
    *   **Suivi de l'approche (Client)** : Le client suit en direct l'arrivée du chauffeur sur sa carte pour savoir exactement quand il sera là (`GET /api/trips/{id}/location`).
*   **Démarrer le trajet (Pickup)** : Une fois le client récupéré, le chauffeur confirme le début de la course dans l'app (`PATCH /api/trips/{id}/status` avec body `{ "status": "ONGOING" }`). -> État **`ONGOING`**.
*   **Phase de trajet (Tracking continu)** : Pendant que le client est à bord jusqu'à la destination :
    *   **Mise à jour GPS** : Le chauffeur continue d'envoyer sa position (`POST /api/trips/{id}/location`).
    *   **Consultation trajet** : Le client (et le système) suit le bon déroulement du trajet sur la carte (`GET /api/trips/{id}/location`).
*   **Terminer le trajet** : Arrivé à destination, le chauffeur clôture la course pour déclencher la facturation (`PATCH /api/trips/{id}/status` avec body `{ "status": "COMPLETED" }`). -> État **`COMPLETED`**.

---

**Astuce pour le code :**
Dans vos applications mobiles (React Native), utilisez un `setInterval` de **5 secondes** :
1.  **Côté Chauffeur** : Pour appeler le `POST` de localisation.
2.  **Côté Client** : Pour appeler le `GET` de localisation et mettre à jour le marqueur (icône voiture) sur la carte.

---

## 4. Contrats d'API (Requêtes & Réponses)

### I. Authentification (Service Externe)

**POST** `/api/auth/register`
*Request Body:*
```json
{
"username": "string",
"password": "stringst",
"email": "string",
"phone": "stringst",
"firstName": "string",
"lastName": "string",
"service": "RIDE_AND_GO",
"roles": [
    "string"
]
}
```
*Response (201):*
```json
{
"accessToken": "string",
"refreshToken": "string",
"user": {
    "id": "string",
    "username": "string",
    "email": "string",
    "phone": "string",
    "firstName": "string",
    "lastName": "string",
    "service": "LETS_GO",
    "roles": [
    "string"
    ],
    "permissions": [
    "string"
    ]
}
}
```

**POST** `/api/auth/login`
*Request Body:*
```json
{
"identifier": "nomo",
"password": "gabriel123"
}
```
*Response (200):*
```json
{
"accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIxMjU1YTgyMy1iNWE0LTQ3MzEtOGQ2OC1iZGY0ZTM2MmMwYjciLCJzdWIiOiJmMGExMjQ2NC0zZGY1LTQ5NDgtYjIzMi1lNGMwYjBhMWZmOGQiLCJpc3MiOiJhdXRoLXNlcnZpY2UiLCJ1c2VybmFtZSI6Im5vbW8iLCJwZXJtaXNzaW9ucyI6W10sInJvbGVzIjpbIkFETUlOIl0sImlhdCI6MTc2Njk5NTI0MSwiZXhwIjoxNzY2OTk2MTQxfQ.XuUaVcZXuZpgxY_sT3vnVBSSnhdACljfU4d8trrI0j4",
"refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMzAzMTE1Ni02MzRkLTRlMGQtODA3Yi1mZTliMmQ4NWY2ZTgiLCJzdWIiOiJmMGExMjQ2NC0zZGY1LTQ5NDgtYjIzMi1lNGMwYjBhMWZmOGQiLCJpc3MiOiJhdXRoLXNlcnZpY2UiLCJpYXQiOjE3NjY5OTUyNDEsImV4cCI6MTc2OTU4NzI0MX0.lPakd_x7ckCI2az32ni1dAcs8ut-9MmcjfCLPySC4Zg",
"user": {
    "id": "f0a12464-3df5-4948-b232-e4c0b0a1ff8d",
    "username": "nomo",
    "email": "gabriel@test.com",
    "phone": "612345678",
    "firstName": "gabriel",
    "lastName": "gabriel",
    "service": "LETS_GO",
    "roles": [
    "ADMIN"
    ],
    "permissions": []
}
}

refresh token `/api/auth/refresh`

`json
    {
"refreshToken": "string"
}
`
```

---

### II. Calcul des coûts (Service Externe / Fallback)


**POST** `/api/fares/estimate`
Endpoint principal d'estimation de prix.
    
    **Flexibilité des paramètres :**
    - Les coordonnées (`lat`/`lon`) sont **optionnelles** si un nom de lieu (`label`) est fourni.
    - L'API effectuera un géocodage automatique si nécessaire.
    - Les paramètres `heure`, `meteo`, `type_zone` sont **optionnels** (détectés automatiquement si omis).
    
    **Exemple minimaliste (Noms de lieux uniquement) :**
    ```json
    {
        "depart": {"label": "Poste Centrale"},
        "arrivee": {"label": "Mvan"}
    }
    ```
*Request Body:*
```json
{
"depart": "string",
"arrivee": "string",
"heure": "matin",
"meteo": 3,
"type_zone": 2,
"congestion_user": 10
}
```
*Response (200):*
```json
{
"statut": "exact",
"prix_moyen": 0,
"prix_min": 0,
"prix_max": 0,
"distance": 0,
"duree": 0,
"estimations_supplementaires": {
    "ml_prediction": 0,
    "features_utilisees": {
    "distance_metres": 0,
    "duree_secondes": 0,
    "congestion": 0,
    "sinuosite": 0,
    "nb_virages": 0,
    "heure": "string",
    "meteo": 0,
    "type_zone": 0
    }
},
"ajustements_appliques": {
    "additionalProp1": "string",
    "additionalProp2": "string",
    "additionalProp3": "string"
},
"fiabilite": 1,
"message": "string",
"details_trajet": {
    "additionalProp1": "string",
    "additionalProp2": "string",
    "additionalProp3": "string"
},
"suggestions": [
    "string"
]
}
```

---

### III. Gestion des Offres (Côté Passager)

**POST** `/api/offers`
*Request Body:*
```json
{
"passengerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
"startPoint": "string",
"endPoint": "string",
"price": 0,
"state": "NEW"
}
```
*Response (201):*
```json
{
"id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
"passengerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
"startPoint": "string",
"endPoint": "string",
"price": 0,
"state": "NEW",
"interestedDrivers": [
    "3fa85f64-5717-4562-b3fc-2c963f66afa6"
]
}
```

**GET** `/api/offers/{id}/bids`
*Response (200):*
```json
[
{
    "driverId": "uuid-driver-1",
    "driverName": "Eto'o Fils",
    "carModel": "Toyota Avensis",
    "rating": 4.8,
    "eta": "5 min"
},
{
    "driverId": "uuid-driver-2",
    "driverName": "Aboubakar",
    "carModel": "Hyundai Elantra",
    "rating": 4.5,
    "eta": "8 min"
}
] // a bien concevoir
```

**PATCH** `/api/offers/{id}/select-driver`
voir image piece jointe
```

---

### IV. Gestion des Offres (Côté Chauffeur)

**GET** `/api/offers/available`
*Response (200):*
```json
[
{
    "id": "uuid-offer-1",
    "origin": "Bastos",
    "destination": "Poste",
    "price": 2500,
    "timestamp": "2025-12-29T10:00:00Z"
}
]
```

**POST** `/api/offers/{id}/apply`
*Request Body:*
```json
{
"driverId": "uuid-driver",
"estimatedArrivalTime": 6 // temps en minutes
}
```
*Response (200):*
```json
{
"message": "Application successful",
"status": "BID_RECEIVED"
}
```

---

### V. Exécution de la Course (Trips)

**POST** `/api/trips`
*Request Body (Automatisé après validation offre):*
```json
{
"offerId": "uuid-offer",
"driverId": "uuid-driver"
}
```
*Response (201):*
```json
{
"tripId": "uuid-trip",
"status": "CREATED"
}
```

**PATCH** `/api/trips/{id}/status`
*Request Body:*
```json
{
"status": "ONGOING"
}
```
*Response (200):*
```json
{
"tripId": "uuid-trip",
"currentStatus": "ONGOING"
}
```

**POST** `/api/trips/{id}/location`
*Request Body:*
```json
{
"latitude": 3.8485,
"longitude": 11.5021
}
```
*Response (200):*
```json
{ "status": "Location updated" }
```

**GET** `/api/trips/{id}/location`
*Response (200):*
```json
{
"latitude": 3.8485,
"longitude": 11.5021,
"updatedAt": "2025-12-29T10:15:00Z"
}
```

---

## 5. Liste récapitulative pour le Swagger
Organise tes contrôleurs avec ces tags :
1.  **Auth-Service** : Inscription et Connexion.
2.  **Fare-Calculator** : Estimation du prix.
3.  **Offer-Controller** : Création, listing des offres disponibles, bidding (apply), et sélection du chauffeur.
4.  **Trip-Controller** : Initialisation du trajet, mise à jour du statut (CREATED/ONGOING/COMPLETED) et tracking GPS.

---

## 💡 Suggestions techniques

*   **Mock data** : Pour l'API de coût, si le service externe répond avec une erreur (500), fais en sorte que ton backend renvoie un prix par défaut (ex: 2000 XAF) pour ne pas bloquer les tests frontend.
*   **Validation Spring** : Utilise `@NotBlank` et `@Size` sur tes DTOs pour valider les JSON de "Register".
*   **React Native** : Utilise un intervalle (setInterval) ou une petite lib de polling pour le `GET /location` afin de simuler le déplacement de la voiture sur la carte du passager.

Bon code à vous deux ! N'hésite pas si tu as besoin d'un script `curl` pour tester une de ces routes.
---

# 🧠 Business Rules (Frontend-Oriented)

Cette section définit **les règles métier immuables** que le frontend doit respecter et faire appliquer via l’API.

---

## 1. Règles liées aux Offres (Offers)

1. **Postulation chauffeur**

* Un chauffeur **ne peut postuler qu’une seule fois** à une même offre.
* Toute tentative multiple doit être ignorée côté frontend (désactivation du bouton).

2. **Modification du choix chauffeur**

* Une fois un chauffeur sélectionné par le client (`DRIVER_SELECTED`),
    **le choix n’est plus modifiable**.
* Le frontend ne doit plus afficher la liste des chauffeurs après validation.

3. **Expiration d’une offre**

* Une offre **expire automatiquement après 5 minutes** si aucun chauffeur n’est sélectionné.
* État final : `CANCELLED`.
* Le frontend doit afficher un message :
    **« Aucune réponse reçue. L’offre a expiré. »**

4. **Annulation d’une offre**

* L’offre peut être annulée :

    * par le **client**
    * par le **chauffeur sélectionné**
* L’annulation n’est possible **uniquement avant le début de la course** (`CREATED`).
* Après démarrage (`ONGOING`), l’annulation n’est plus permise.

---

## 2. Règles liées aux Courses (Trips)

1. **Création de la course**

* Une course (`Trip`) est créée **uniquement après** :

    * sélection du chauffeur par le client
    * acceptation implicite du chauffeur
* État initial : `CREATED`.

2. **Paiement**

* **Aucun paiement n’est effectué via l’application**.
* Le paiement est effectué **en cash**, **après la course**, **hors système**.
* L’état `COMPLETED` signifie uniquement :

    * trajet terminé
    * pas une confirmation de paiement électronique.

3. **Démarrage de la course**

* Seul le **chauffeur** peut déclencher le passage à `ONGOING`.
* Le frontend client passe alors en mode **suivi trajet actif**.

4. **Fin de la course**

* Seul le **chauffeur** peut clôturer la course (`COMPLETED`).
* Une fois `COMPLETED`, la course devient **en lecture seule** côté frontend.

---

## 3. Règles de Tracking GPS

1. **Avant pickup (Approche)**

* Le chauffeur envoie sa position toutes les 5 secondes.
* Le client voit la progression en temps réel.

2. **Pendant le trajet**

* Le tracking continue jusqu’à `COMPLETED`.

3. **Responsabilité frontend**

* Le frontend **doit gérer le polling** (`setInterval`).
* Aucune logique temps réel (WebSocket) n’est requise à ce stade.

---

# ⚠️ Gestion des Erreurs (Frontend Contract)

Cette section définit **le comportement standard du frontend en cas d’erreur backend**.

---

## 1. Modèle de réponse d’erreur attendu

Le backend renvoie une erreur JSON standardisée :

```json
{
"timestamp": "2025-12-29T10:30:00Z",
"status": 400,
"error": "Bad Request",
"message": "Offer already expired",
"path": "/api/offers/123/apply"
}
```

---

## 2. Comportement du Frontend

1. **Aucune logique métier côté frontend**

* Le frontend **ne tente pas d’interpréter le type d’erreur**.
* Il ne distingue pas `400`, `403`, `409`, etc.

2. **Affichage utilisateur**

* En cas d’erreur backend :

    * afficher un message générique :

    > **« Une erreur est survenue »**
    * afficher ensuite le message retourné par le backend :

    > `error.message`

3. **Aucune action automatique**

* Le frontend :

    * ne retry pas automatiquement
    * ne change pas d’état local
    * attend une action utilisateur

---

## 3. Exemple de traitement Frontend (Conceptuel)

```ts
try {
await api.call()
} catch (error) {
showToast("Une erreur est survenue")
showToast(error.message)
}
```

*(Exemple conceptuel – non contractuel)*
