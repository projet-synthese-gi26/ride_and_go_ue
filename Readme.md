# 🚗 Ride & Go API - Guide de démarrage rapide

Ce guide permet de configurer l'environnement de développement complet (Postgres, Redis, Kafka) en local.

## 📋 Prérequis
- **Java 21**
- **Docker & Docker Compose**

## 🚀 Étape 1 : Lancer l'infrastructure
Depuis la racine du projet :
```bash
docker-compose up -d
```
Ceci démarre :
- **Postgres** (Port 5432) : Stockage des utilisateurs et offres.
- **Redis** (Port 6379) : Tracking GPS temps réel (Mot de passe: `password`).
- **Redpanda/Kafka** (Port 9092) : Système de notifications.

## ⚙️ Étape 2 : Configuration de l'application
Assurez-vous que le profil `local` est activé dans `src/main/resources/application.yml` :
```yaml
spring:
  profiles:
    active: local
```

## 🛠️ Étape 3 : Lancer le projet
```bash
./mvnw clean spring-boot:run
```
Le système va automatiquement :
1. Créer le schéma central (Postgres).
2. Injecter **100 utilisateurs de test** (5 Admins, 30 Chauffeurs, 65 Passagers).

## ✅ Étape 4 : Tester le flux Marketplace
1. **Swagger UI** : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
2. **Identités de test (Tokens statiques)** :
   - Client : `Bearer client-token`
   - Chauffeur 1 : `Bearer driver-1-token`
   - Chauffeur 2 : `Bearer driver-2-token`

## 🐳 Utilitaires Docker
- Stopper l'infra : `docker-compose stop`
- Tout supprimer (volumes inclus) : `docker-compose down -v`
