### Protocole de Test "1 Client + 2 Chauffeurs"

Suis ces étapes dans l'ordre exact :

#### 1. Préparation du terrain (GPS)
Il faut que les chauffeurs "existent" dans Redis pour que l'ETA puisse être calculé plus tard.
*   **Action** : Clique sur **Authorize** et saisis `driver-1-token`.
*   **Action** : Appelle `POST /api/v1/location` avec :
    ```json
    { "latitude": 3.8666, "longitude": 11.5166 }
    ```
*   **Action** : Change le token dans Authorize pour `driver-2-token`.
*   **Action** : Appelle `POST /api/v1/location` avec :
    ```json
    { "latitude": 3.8444, "longitude": 11.5000 }
    ```

#### 2. Publication de l'Offre
*   **Action** : Change le token pour `client-token`.
*   **Action** : Appelle `POST /api/v1/offers`.
    *   *Note* : Utilise le json `{
  "passengerId": "7f13909e-7170-4f91-872e-333333333333",
  "startPoint": "Bastos",
  "endPoint": "Poste Centrale",
  "price": 2500,
  "state": "PENDING"
}` dans le body pour être raccord avec le token.
    *   **Récupère l'ID de l'offre** créé dans la réponse.

#### 3. Bidding (Les chauffeurs postulent)
*   **Action** : Reprends le token `driver-1-token`.
*   **Action** : Appelle `POST /api/v1/offers/{id}/apply` (avec l'ID de l'offre et l'UUID driver `a1b2c3d4-e5f6-4a5b-8c9d-111111111111`).
*   **Action** : Fais de même avec le token et l'UUID du chauffeur 2 (`a1b2c3d4-e5f6-4a5b-8c9d-222222222222`).

#### 4. Consultation et Sélection (Le moment de vérité)
*   **Action** : Reprends le token `client-token`.
*   **Action** : Appelle `GET /api/v1/offers/{id}/bids`.
    *   **Vérification** : Tu devrais voir une liste de 2 `bids` avec les noms des chauffeurs (issus de Postgres), leurs coordonnées (issues de Redis) et un ETA (issu du service random).
*   **Action** : Appelle `PATCH /api/v1/offers/{id}/select-driver` avec l'ID d'un des chauffeurs.

---

### Points de vigilance (Debug)
*   Si le `POST /offers` échoue avec une erreur de clé étrangère (FK), vérifie que l'UUID du passager existe bien dans ta table `customers`.
*   Si le `GET /bids` renvoie des coordonnées `0.0`, c'est que le TTL de 5 minutes de Redis a expiré ou que le token n'était pas le bon lors du `POST /location`.
