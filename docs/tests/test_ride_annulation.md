### 🧪 Comment Tester l'Annulation (Swagger)

Une fois le fichier mis à jour et l'app redémarrée :

#### Cas A : Le Chauffeur annule (N'importe quand)
1.  **Auth** : `driver-1-token`
2.  **Route** : `PATCH /api/v1/trips/{TRIP_ID}/status`
3.  **Body** :
    ```json
    { "status": "CANCELLED" }
    ```
4.  **Résultat** : `200 OK`, état `CANCELLED`.

#### Cas B : Le Passager annule (Avant départ)
1.  **Auth** : `client-token`
2.  **Condition** : La course doit être en état `CREATED` (juste après l'acceptation).
3.  **Route** : `PATCH /api/v1/trips/{TRIP_ID}/status`
4.  **Body** : `{ "status": "CANCELLED" }`
5.  **Résultat** : `200 OK`.

#### Cas C : Le Passager essaie de tricher (Après départ)
1.  Fais passer la course à `ONGOING` avec le Chauffeur.
2.  Essaie d'annuler avec le Passager.
3.  **Résultat** : `400` ou `500` avec le message *"Too late to cancel..."*.