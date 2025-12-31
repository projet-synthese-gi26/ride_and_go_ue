# 📱 Guide de Test E2E : Scénario Multi-Clients

**Prérequis** : Ouvre 3 onglets/fenêtres de ton navigateur avec Swagger.
1.  **Fenêtre A** : Passager (Client)
2.  **Fenêtre B** : Chauffeur 1 (Eto'o - Douala)
3.  **Fenêtre C** : Chauffeur 2 (Aboubakar - Yaoundé)

---

## 🔐 Phase 0 : Authentification (Sur chaque fenêtre)

*   **Fenêtre A (Passager)** : Clique sur `Authorize` -> `client-token`
*   **Fenêtre B (Chauffeur 1)** : Clique sur `Authorize` -> `driver-1-token`
*   **Fenêtre C (Chauffeur 2)** : Clique sur `Authorize` -> `driver-2-token`

---

## 📍 Phase 1 : Mise en place (GPS)
*Les chauffeurs doivent s'activer pour être visibles.*

1.  **Fenêtre B (Chauffeur 1)**
    *   **Route** : `POST /api/v1/location`
    *   **Body** : `{ "latitude": 3.8666, "longitude": 11.5166 }`
    *   **Attendu** : `200 OK`

2.  **Fenêtre C (Chauffeur 2)**
    *   **Route** : `POST /api/v1/location`
    *   **Body** : `{ "latitude": 3.8444, "longitude": 11.5000 }`
    *   **Attendu** : `200 OK`

---

## 🟢 SCÉNARIO PRINCIPAL : La Course Parfaite

### Étape 1 : Le besoin (Passager)
**Fenêtre A (Passager)**
*   **Action** : Publier une demande de course.
*   **Route** : `POST /api/v1/offers`
*   **Body** :
    ```json
    {
      "passengerId": "7f13909e-7170-4f91-872e-333333333333",
      "startPoint": "Bastos",
      "endPoint": "Poste Centrale",
      "price": 2500,
      "state": "PENDING"
    }
    ```
*   **Attendu** : `200 OK`. L'objet Offre est créé.
*   📝 **NOTE L'ID DE L'OFFRE (OFFER_ID) :** `__________________________`

### Étape 2 : La découverte (Chauffeurs)
**Fenêtre B (Chauffeur 1)**
*   **Action** : Vérifier s'il y a du boulot.
*   **Route** : `GET /api/v1/offers/available`
*   **Attendu** : Une liste JSON contenant ton `OFFER_ID`.

### Étape 3 : La candidature (Chauffeurs)
**Fenêtre B (Chauffeur 1)**
*   **Action** : "Je suis intéressé".
*   **Route** : `POST /api/v1/offers/{OFFER_ID}/apply?driverId=a1b2c3d4-e5f6-4a5b-8c9d-111111111111`
*   **Attendu** : `200 OK`.

**Fenêtre C (Chauffeur 2)**
*   **Action** : "Moi aussi".
*   **Route** : `POST /api/v1/offers/{OFFER_ID}/apply?driverId=a1b2c3d4-e5f6-4a5b-8c9d-222222222222`
*   **Attendu** : `200 OK`.

### Étape 4 : Le Choix (Passager)
**Fenêtre A (Passager)**
*   **Action** : Voir qui a postulé.
*   **Route** : `GET /api/v1/offers/{OFFER_ID}/bids`
*   **Attendu** : Liste de 2 chauffeurs avec leurs noms et ETA calculés.

**Fenêtre A (Passager)**
*   **Action** : Choisir Chauffeur 1 (Eto'o).
*   **Route** : `PATCH /api/v1/offers/{OFFER_ID}/select-driver?driverId=a1b2c3d4-e5f6-4a5b-8c9d-111111111111`
*   **Attendu** : `200 OK`.
    *   **Vérif** : `state` doit être **`DRIVER_SELECTED`**.

### Étape 5 : La Confirmation "Handshake" (Chauffeur 1)
*Le Chauffeur 1 reçoit une notif "Vous avez été choisi". Il doit confirmer.*

**Fenêtre B (Chauffeur 1)**
*   **Action** : "Ok, j'arrive !" (C'est ici que la Course est créée).
*   **Route** : `POST /api/v1/offers/{OFFER_ID}/accept?driverId=a1b2c3d4-e5f6-4a5b-8c9d-111111111111`
*   **Attendu** : `200 OK`. Retourne un objet **Ride**.
    *   **Vérif** : `state` du Ride est `CREATED`.
*   📝 **NOTE L'ID DE LA COURSE (TRIP_ID) :** `__________________________`

### Étape 6 : L'Approche et Pickup (Chauffeur 1)
*Le chauffeur roule vers le client... Il arrive.*

**Fenêtre B (Chauffeur 1)**
*   **Action** : "Client à bord, on démarre".
*   **Route** : `PATCH /api/v1/trips/{TRIP_ID}/status`
*   **Body** : `{ "status": "ONGOING" }`
*   **Attendu** : `200 OK`. `state` passe à `ONGOING`.

### Étape 7 : La Fin de Course (Chauffeur 1)
*Arrivée à destination.*

**Fenêtre B (Chauffeur 1)**
*   **Action** : "Course terminée, paiement cash".
*   **Route** : `PATCH /api/v1/trips/{TRIP_ID}/status`
*   **Body** : `{ "status": "COMPLETED" }`
*   **Attendu** : `200 OK`. `state` passe à `COMPLETED`.

---

## 🔴 SCÉNARIO ALTERNATIF : Annulation par le Client

*Condition : Ce test se fait sur une NOUVELLE offre, avant l'étape 5 (Acceptation chauffeur).*

### Cas 1 : Annulation avant sélection
1.  **Fenêtre A** : Créer offre (`PENDING`). Note `OFFER_ID_2`.
2.  **Fenêtre A** : "Finalement non, j'annule".
3.  **Route** : `POST /api/v1/offers/{OFFER_ID_2}/cancel`
4.  **Attendu** : `200 OK`. `state` passe à `CANCELLED`.
5.  **Vérif** : Les chauffeurs ne doivent plus voir cette offre dans `/available`.

### Cas 2 : Annulation après sélection (mais avant arrivée)
1.  **Fenêtre A** : Créer offre, attendre bids, faire `select-driver` (`DRIVER_SELECTED`).
2.  **Fenêtre A** : "Le chauffeur met trop de temps, j'annule".
3.  **Route** : `POST /api/v1/offers/{OFFER_ID_3}/cancel`
4.  **Attendu** : `200 OK`. `state` passe à `CANCELLED`.
5.  **Impact** : Le chauffeur qui tente d'`accept` doit recevoir une erreur (400 ou 404).

---

## 🧪 Règles de validation technique

1.  Un chauffeur **ne peut pas** accepter une offre qui ne lui est pas destinée (Test avec Fenêtre C essayant d'accepter l'offre de Fenêtre B).
2.  Un passager **ne peut pas** annuler une course (`Ride`) qui est déjà `ONGOING` (seul le chauffeur gère le statut du Ride).
3.  Les IDs (`OFFER_ID`, `TRIP_ID`) doivent être copiés-collés depuis les réponses JSON précédentes, ne les invente pas.