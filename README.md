# Restaurant - Quarkus Microservices

In diesem Projekt findest du zwei Quarkus-Services, die über Kafka-Topics kommunizieren. Einer der Services bindet eine PostgreSQL-Datenbank an. Außerdem kannst du alles per Docker Compose starten und optional in die GitHub Container Registry (GHCR) pushen.

## Inhalt
1. [Überblick](#überblick)
2. [Voraussetzungen](#voraussetzungen)
3. [Lokaler Build & Start](#lokaler-build--start)
    - [Schritt-für-Schritt](#schritt-für-schritt)
4. [Wichtigste Anwendungsfälle](#wichtigste-anwendungsfälle)
    - [orders-service](#orders-service)
    - [inventory-service](#inventory-service)
5. [Docker Compose intern](#docker-compose-intern)
6. [Deployment in GitHub Container Registry (GHCR)](#deployment-in-github-container-registry-ghcr)
7. [Lizenz / Sonstiges](#lizenz--sonstiges)

---

## Überblick

- **orders-service**
    - Bietet einen REST-Endpunkt `/orders` (Port 8080) an.
    - Nimmt Bestellungen entgegen, speichert sie in einer PostgreSQL-Datenbank `ordersdb`.
    - Sendet eine Nachricht an das Kafka-Topic `orders`.

- **inventory-service**
    - Lauscht auf dem Kafka-Topic `orders`.
    - Sobald eine Bestellung eingeht, simuliert es eine Lagerbestandsprüfung und sendet eine Meldung ans Topic `inventory`.

- **Kafka & PostgreSQL**
    - Über Docker Compose werden Zookeeper, Kafka und eine Postgres-DB gestartet.

Ziel ist es, das Zusammenspiel von **Quarkus**, **Kafka** und einer **Datenbank** zu demonstrieren.

---

## Voraussetzungen

- **Docker** und **Docker Compose** (Version 1.29 oder höher)
- (Optional) **Maven** und **Java 17** (falls du das Projekt lokal kompilieren möchtest; sonst reicht Docker Compose aus)
- (Optional) **GitHub-Login** (falls du die Images aus GHCR ziehen willst oder dorthin pushen möchtest)

> **Hinweis:** Wir nutzen hier **Java 17**. Mit Java 21 kann es derzeit noch Byte Buddy-Kompatibilitätsprobleme geben.

---

## Lokaler Build & Start

### Schritt-für-Schritt

1. **Repository klonen**:
   ```bash
   git clone <URL-zu-deinem-GitRepo> restaurant
   cd restaurant
    ```
   
2. **Maven Build** nur nötig, wenn du Code geändert hast oder die JARs neu bauen willst: 
   ```bash
   mvn clean package -DskipTests
   ```
Dies kompiliert beide Submodule (`orders-service`, `inventory-service`) und legt die Artefakte im jeweiligen `target`/-Ordner ab.

3. **Docker-Images bauen** (manuell, falls du nicht mit Quarkus-Plugins arbeitest):
```bash
# 1) orders-service
cd orders-service
docker build -f src/main/docker/Dockerfile.jvm -t orders-service:1.0.0 .

# 2) inventory-service
cd ../inventory-service
docker build -f src/main/docker/Dockerfile.jvm -t inventory-service:1.0.0 .

# zurück ins Root-Verzeichnis
cd ..
```

4. **Docker Compose starten:**
```bash
docker-compose up --build
```

# Docker Compose intern
- zookeeper (Container Name: zookeeper, Port 2181)
- kafka (Container Name: kafka, Port 9092)
- postgres (Container Name: postgres, Port 5432)
- orders-service (Port 8080 -> Host: 8080)
- inventory-service (Port 8081 -> Host: 8081)
### Achtung:
Innerhalb des Docker-Netzwerks erreichst du Kafka unter kafka:9092 und Postgres unter postgres:5432. Diese URLs werden per Umgebungsvariablen an die Services übergeben (siehe docker-compose.yml).

5. **Testen der Applikation**
- Sende einen REST-POST an den ``orders-service``:
```bash
curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"product":"Laptop", "quantity":2}' \
     http://localhost:8080/orders
```
- Überprüfe in den Logs von orders-service und inventory-service, ob die Nachricht korrekt verarbeitet wird.
- Optional: Überprüfe in der Postgres-Datenbank, ob die Bestellung gespeichert wurde.