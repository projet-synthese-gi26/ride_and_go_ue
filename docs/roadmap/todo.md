### 📋 Roadmap API RIDE & GO 

- [x] **Tâche 1 :** Configuration du projet (config,Sécurité, alignement BD Centrale & Seeding,swagger).
- [x] **1.1 : Identité & Nettoyage (Metadata)**
    - [x] Mettre à jour le `pom.xml` (artifactId: `ride-and-go`, name: `Ride & Go API`).
    - [x] Renommer le package racine `com.yowyob.rideandgo` en `com.yowyob.rideandgo`.
    - [x] Supprimer tous les fichiers liés au domaine "Product" (Entities, Mappers, Services, Controllers).
    - [x] Nettoyer `application.yml` (nom de l'app, group-id Kafka, suppression des clés inutiles).

- [x] **1.2 : Alignement avec la DB Centrale (Structure)**
    - [x] Harmoniser les scripts SQL : Utiliser les noms de tables globaux (`users`, `roles`, `business_actors`) au lieu des préfixes `ride_and_go_`.
    - [x] Mettre à jour les entités Java (`UserEntity`, `RoleEntity`, etc.) avec les bonnes annotations `@Table`.
    - [x] Valider la cohérence des types UUID pour toutes les clés primaires et étrangères.

- [x] **1.3 : Sécurité Réactive & Documentation (Security/Swagger)**
    - [x] Implémentation de `SecurityConfig` : Configuration WebFlux réactive (Stateless, protection des routes, désactivation du mot de passe par défaut).
    - [x] Autorisation des routes Swagger et HealthCheck dans la chaîne de filtres.
    - [x] Organisation du Swagger UI par tags métier (Auth, Fares, Offers, Trips) selon les spécifications.

- [x] **1.4 : Automatisation & Seeding (Données de test)**
    - [x] Configurer `DatabaseInitConfig` pour une exécution séquentielle (Schema -> Check -> Data).
    - [x] Préparer `src/main/resources/local/data.sql` avec les rôles (`PASSENGER`, `DRIVER`) et des utilisateurs de test Ride & Go.
    - [x] **Validation finale** : Démarrage complet de l'application et vérification des 100+ utilisateurs via Swagger.

- [x] **Tâche 2 :** Gestion de l'Authentification (auth fake,Liaison TraMaSys & Profils).
    - [x] **2.1 : Contrats du Domaine**
        - [x] Mettre à jour `AuthPort` et `AuthUseCase` pour supporter Login/Register/Reset.
        - [x] Aligner le modèle `AuthResponse` sur le format TraMaSys (Token, Rôles, Permissions).
    - [x] **2.2 : Implémentation des Adaptateurs**
        - [x] Créer `FakeAuthAdapter` (Mode développement sans réseau).
        - [x] Créer `RemoteAuthAdapter` (Appels réels via `AuthApiClient`).
        - [x] Configurer `AuthConfig` pour le switch dynamique via `application.auth.mode`.
    - [x] **2.3 : Sécurisation par Token (JWT Validation)**
        - [x] Implémenter le `AuthenticationManager` réactif pour valider les tokens via TraMaSys.
        - [x] Configurer le filtre d'extraction du header `Authorization: Bearer`.
    - [x] **2.4 : API REST & Documentation**
        - [x] Finaliser `AuthController` avec les endpoints de Login et Register.
        - [x] **Validation Swagger** : Scénario "Login réel sur TraMaSys -> Récupération du JWT -> Accès au HealthCheck protégé".

- [x] **Tâche 3 : Gestion des Offres (Flux Marketplace Complet)**
    - [x] **3.1 : Stratégie d'Estimation (Fares - Stateless)**
        - [x] Créer `FakeFareAdapter` et `RemoteFareAdapter`.
        - [x] Configurer `FareConfig` (Switch application.fare.mode).
    - [x] **3.2 : Infrastructure & Cache (Redis & SQL)**
        - [x] Implémenter `LocationCachePort` dans `RedisAdapter` (TTL 5 min).
        - [x] Configurer le Repository SQL pour `offer_driver_linkages`.
    - [x] **3.3 : Modèles de Domaine & Ports**
        - [x] Créer le record `Bid` (driverId, name, eta, latitude, longitude, rating).
        - [x] Mettre à jour `Offer` pour inclure une `List<Bid>`.
    - [x] **3.4 : Services de Calcul & Tracking**
        - [x] Implémenter `EtaCalculatorService` (Logique dynamique via Redis).
        - [x] Implémenter `UpdateLocationUseCase` (Tracking acteur via JWT).
    - [x] **3.5 : Logique Métier Marketplace (Actions & États)**
        - [x] `CreateOfferUseCase` : Publication (PENDING).
        - [x] `GetAvailableOffersUseCase` : Liste des offres pour les chauffeurs.
        - [x] `ResponseToOfferUseCase` (Apply) : Inscription du postulant en SQL.
        - [x] `GetOfferBidsUseCase` : Agrégation réactive (SQL + Redis + ETA).
        - [x] `SelectDriverUseCase` : Validation du choix passager (Passage à DRIVER_SELECTED).
    - [x] **3.6 : API REST & Mapping**
        - [x] `POST /api/v1/fares/estimate` (Consultation).
        - [x] `POST /api/v1/location` (Tracking acteur).
        - [x] `GET /api/v1/offers/available` (Discovery chauffeur).
        - [x] `GET /api/v1/offers/{id}/bids` (Consultation passager).
        - [x] `PATCH /api/v1/offers/{id}/select-driver` (Action de sélection).
    - [x] **3.7 : test swagger d'un flux complet 1 client et 2 chauffeurs**

- [x] **Tâche 4 : Gestion des Courses (Trips - Core Domain)**
    - [x] 4.1 : Création du Trip (Transition depuis l'Offre).
    - [x] 4.2 : Machine à états (CREATED -> ONGOING -> COMPLETED/CANCELLED).
    - [x] 4.3 : API de gestion d'état (Start/End ride).
    - [x] 4.4 : Règles métier (Qui peut démarrer ? Qui peut finir ?).

- [x] **Tâche 5 : Gestion du GPS (Tracking Temps Réel)**
    - [x] 5.1 : Polling & Mise à jour Redis.
    - [x] 5.2 : API de consultation Smart (Suivre son partenaire : Chauffeur <-> Passager).
    - [x] 5.3 : Calcul de distance & ETA (Service dédié Haversine).

- [ ] **Tâche 6 : Gestion des Utilisateurs (Interne)**
    - [ ] 6.1 : CRUD Admin (si nécessaire).
    - [ ] 6.2 : Profil User & Driver (Rating, Info véhicule).

- [ ] **Tâche 7 : Intégration Services Externes (Switch Remote)**
    - [ ] 7.1 : Test Auth Service en réel.
    - [ ] 7.2 : Test Fare Calculator en réel.
    - [ ] 7.3 : Gestion des Timeouts & Circuit Breakers (Resilience4j).

- [ ] **Tâche 8 :** Services Périphériques & Notation (Calculs réels, Reviews,abonnement des chauffeurs).

- [ ] **Tâche 9 : Fine-tuning & Consolidation (Post-Integration)**
    - [ ] 9.1 : Back-office Admin (CRUD complet Trips/Offers pour le support/debug).
    - [ ] 9.2 : Durcissement Sécurité (Rate Limiting, Renforcement JWT, Cors).
    - [ ] 9.3 : Gestion Globale des Erreurs (Standardisation ProblemDetails 4xx/5xx).
    - [ ] 9.4 : Optimisation & Nettoyage (Refactoring, Logs de prod, Code mort).