### 📋 Roadmap API RIDE & GO 

- [ ] **Tâche 1 :** Configuration du projet (config,Sécurité, alignement BD Centrale & Seeding,swagger).
- [ ] **1.1 : Identité & Nettoyage (Metadata)**
    - [x] Mettre à jour le `pom.xml` (artifactId: `ride-and-go`, name: `Ride & Go API`).
    - [ ] Renommer le package racine `com.yowyob.rideandgo` en `com.yowyob.rideandgo`.
    - [ ] Supprimer tous les fichiers liés au domaine "Product" (Entities, Mappers, Services, Controllers).
    - [ ] Nettoyer `application.yml` (nom de l'app, group-id Kafka, suppression des clés inutiles).

- [ ] **1.2 : Alignement avec la DB Centrale (Structure)**
    - [ ] Harmoniser les scripts SQL : Utiliser les noms de tables globaux (`users`, `roles`, `business_actors`) au lieu des préfixes `ride_and_go_`.
    - [ ] Mettre à jour les entités Java (`UserEntity`, `RoleEntity`, etc.) avec les bonnes annotations `@Table`.
    - [ ] Valider la cohérence des types UUID pour toutes les clés primaires et étrangères.

- [ ] **1.3 : Sécurité Réactive & Documentation (Security/Swagger)**
    - [ ] Implémentation de `SecurityConfig` : Configuration WebFlux réactive (Stateless, protection des routes, désactivation du mot de passe par défaut).
    - [ ] Autorisation des routes Swagger et HealthCheck dans la chaîne de filtres.
    - [ ] Organisation du Swagger UI par tags métier (Auth, Fares, Offers, Trips) selon les spécifications.

- [ ] **1.4 : Automatisation & Seeding (Données de test)**
    - [ ] Configurer `DatabaseInitConfig` pour une exécution séquentielle (Schema -> Check -> Data).
    - [ ] Préparer `src/main/resources/local/data.sql` avec les rôles (`PASSENGER`, `DRIVER`) et des utilisateurs de test Ride & Go.
    - [ ] **Validation finale** : Démarrage complet de l'application et vérification des 100+ utilisateurs via Swagger.
- [ ] **Tâche 2 :** Gestion de l'Authentification (auth fake,Liaison TraMaSys & Profils).
- [ ] **Tâche 3 :** Gestion des Offres (Flux Marketplace : Estimation -> Publication -> Bidding -> Sélection).
- [ ] **Tâche 4 :** Gestion des Courses (Cycle de vie : Création -> Démarrage -> Fin).
- [ ] **Tâche 5 :** Gestion du GPS (Moteur de tracking & Polling de position).
- [ ] **Tâche 6 :** Services Périphériques & Notation (Calculs réels, Reviews).
