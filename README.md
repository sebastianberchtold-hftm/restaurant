# Restaurant - Quarkus Microservices

In diesem Projekt findest du zwei Quarkus-Services, die über Kafka-Topics kommunizieren. Einer der Services bindet eine PostgreSQL-Datenbank an. Außerdem kannst du alles per Docker Compose starten und optional in die GitHub Container Registry (GHCR) pushen.

## Inhalt

1. [Überblick](#überblick)
2. [Voraussetzungen](#voraussetzungen)
3. [Lokaler Build & Start](#lokaler-build--start)
   - [Schritt-für-Schritt-Anleitung](#schritt-für-schritt-anleitung)
4. [Wichtigste Anwendungsfälle und Testanleitung](#wichtigste-anwendungsfälle-und-testanleitung)
   - [Anwendungsfälle](#anwendungsfälle)
   - [Testanleitung](#testanleitung)
5. [Docker Compose intern](#docker-compose-intern)
6. [Deployment in GitHub Container Registry (GHCR) und Aktualitätsprüfung](#deployment-in-github-container-registry-ghcr-und-aktualitätsprüfung)
7. [Lizenz / Sonstiges](#lizenz--sonstiges)

---

## 1. Überblick

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

## 2. Voraussetzungen

- **Docker** und **Docker Compose** (Version 1.29 oder höher)
- (Optional) **Maven** und **Java 17** (falls du das Projekt lokal kompilieren möchtest; sonst reicht Docker Compose aus)
- (Optional) **GitHub-Login** (falls du die Images aus GHCR ziehen willst oder dorthin pushen möchtest)

> **Hinweis:** Wir nutzen hier **Java 17**. Mit Java 21 kann es derzeit noch Byte Buddy-Kompatibilitätsprobleme geben.

---

## 3. Lokaler Build & Start

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

## 4. Wichtigste Anwendungsfälle und Testanleitung

Dieses Kapitel beschreibt die zentralen Anwendungsfälle der Lösung und erklärt, wie du sie testen kannst.

 ### 4.1 Anwendungsfälle

1. **Bestellung erfassen (orders-service)**
   - **Beschreibung:**  
     Der `orders-service` bietet einen REST-Endpunkt `/orders`, über den Bestellungen als JSON-Daten empfangen werden. Eine Bestellung beinhaltet mindestens das Produkt und die Anzahl (quantity).  
   - **Ablauf:**  
     - Der Service empfängt die Bestellung und speichert sie in der PostgreSQL-Datenbank.
     - Nach erfolgreichem Speichern wird eine Nachricht, die die Details der Bestellung enthält, über Kafka (Channel `orders-out`) an das physische Topic `orders` gesendet.
     
2. **Bestellungsverarbeitung (inventory-service)**
   - **Beschreibung:**  
     Der `inventory-service` lauscht auf dem Kafka-Topic `orders` (über den internen Channel `orders-in`).
   - **Ablauf:**  
     - Sobald eine Nachricht vom `orders`-Topic empfangen wird, verarbeitet der Service diese – beispielsweise um den Lagerbestand zu prüfen oder eine Bestätigung zu generieren.
     - Anschließend sendet der Service über den Channel `inventory-out` eine Rückmeldung (z. B. "Inventory updated for order ...") an das Kafka-Topic `inventory`.

3. **Datenbankintegration**
   - **Beschreibung:**  
     Der `orders-service` verwendet Panache, um Bestellungen in der PostgreSQL-Datenbank zu speichern.  
   - **Ablauf:**  
     - Beim Empfang einer Bestellung wird ein Datensatz in der Tabelle `orders` erstellt.
     - Dadurch können Persistenz und spätere Abfragen (z. B. via psql, pgAdmin, oder DBeaver) überprüft werden.

4. **Asynchrone Kommunikation über Kafka**
   - **Beschreibung:**  
     Die Services kommunizieren asynchron über Kafka, sodass:
     - Der `orders-service` Bestellungen an das Kafka-Topic `orders` sendet.
     - Der `inventory-service` diese Nachrichten abonniert und verarbeitet.
   - **Ablauf:**  
     - Nach dem Senden einer Bestellung sollten die Kafka-Producer- und Consumer-Logs anzeigen, dass Nachrichten ankommen und verarbeitet werden.
     - Optional kann ein Kafka Console Consumer verwendet werden, um den Nachrichtenfluss direkt zu überwachen.

### 6.2 Testanleitung

#### A. Testen des REST-Endpunkts mit Postman

1. **GET-Test (Statusprüfer):**
   - Sende einen GET-Request an `http://localhost:8080/orders` (falls du einen einfachen Test-Endpoint implementiert hast, der z. B. einen Status zurückgibt).  
   - **Erwartetes Ergebnis:**  
     ```json
     {"status": "orders-service is up"}
     ```
   - Dies bestätigt, dass der Service erreichbar ist.

2. **POST-Test (Bestellung erfassen):**
   - Erstelle einen neuen POST-Request in Postman:
     - **URL:** `http://localhost:8080/orders`
     - **Header:** `Content-Type: application/json`
     - **Body (raw, JSON):**
       ```json
       {
         "product": "Laptop",
         "quantity": 2
       }
       ```
   - **Erwartetes Ergebnis:**  
     - Der Server antwortet mit einem HTTP 200 Status und der Nachricht:
       ```json
       {"message": "Order processed successfully"}
       ```
     - In den Logs des `orders-service` sollte ein Eintrag erscheinen, der anzeigt, dass die Bestellung empfangen, in die DB gespeichert und eine Kafka-Nachricht gesendet wurde.

#### B. Überprüfung der asynchronen Kommunikation über Kafka

1. **Kafka-Log-Überwachung:**
   - Öffne separate Terminalfenster und überwache die Logs des `inventory-service` (z. B. mit `docker logs -f inventory-service`).
   - **Erwartetes Verhalten:**  
     - Sobald eine Bestellung über den `orders-service` gesendet wird, sollte im Log des `inventory-service` ein Eintrag wie „Received order: …“ erscheinen.
     - Anschließend sollte eine Rückmeldung (z. B. „Inventory updated for order: …“) gesendet werden.

2. **Kafka Console Consumer (optional):**
   - Um den Nachrichtenfluss direkt zu überwachen, kannst du einen Kafka Console Consumer starten. Wechsle dazu in den Kafka-Container:
     ```bash
     docker exec -it kafka bash
     ```
   - Führe dann folgenden Befehl aus, um Nachrichten vom `inventory`-Topic zu sehen:
     ```bash
     kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic inventory --from-beginning
     ```

#### C. Datenbankprüfung

1. **Mit einem Postgres-Client:**
   - Verbinde dich mit der PostgreSQL-Datenbank (normalerweise unter `localhost:5432`).
   - Verwende die Zugangsdaten (`postgres` / `postgres`) und die Datenbank `ordersdb`.
   - Führe in einer SQL-Konsole folgenden Befehl aus:
     ```sql
     SELECT * FROM orders;
     ```
   - **Erwartetes Ergebnis:**  
     - Es sollte mindestens ein Datensatz erscheinen, der der Bestellung entspricht (z. B. `Laptop`, `quantity: 2`).

---

## 5. Docker Compose intern
- **zookeeper:** Container Name: zookeeper, Port: 2181.
- **kafka:** Container Name: kafka, Port: 9092.
- **postgres:** Container Name: postgres, Port: 5432.
- **orders-service:** Läuft auf Port 8080 (Host-Port 8080).
- **inventory-service:** Läuft auf Port 8081 (Host-Port 8081).

  **Achtung**:
  Innerhalb des Docker-Netzwerks erreichst du Kafka über kafka:9092 und Postgres über postgres:5432. Diese Informationen werden per Umgebungsvariablen an die Services übergeben.

 ## 6. Deployment in GitHub Container Registry (GHCR) und Aktualitätsprüfung

- **Deployment:**
    Die Docker-Images wurden in die GHCR gepusht. Die Docker Compose File verweist auf:

    - ``ghcr.io/sebastianberchtold-hftm/orders-service:1.0.0``
    - ``ghcr.io/sebastianberchtold-hftm/inventory-service:1.0.0``
- **Aktualitätsprüfung:**
    Um sicherzustellen, dass die neuesten Images vorliegen, kannst du:

    - Den Befehl docker images verwenden, um die Erstellungszeiten und Digests zu vergleichen.
    - In deinem GitHub-Profil unter dem Reiter "Packages" die aktuell gepushten Images überprüfen.
    - Testweise die Images mittels:
```bash
docker pull ghcr.io/sebastianberchtold-hftm/orders-service:1.0.0
```
   ausführen und mit docker inspect den Digest vergleichen.
---
## 7. Lizenz/ Sonstiges
Dieses Projekt ist ein Demoprojekt zum Thema „Messaging and Streaming mit Quarkus“. Der Code kann nach Bedarf erweitert und angepasst werden.

### Zusammenfassung

- **Orders-Service:** Erfasst Bestellungen via POST, speichert diese in der DB und sendet Kafka-Nachrichten.
- **Inventory-Service:** Empfängt Kafka-Nachrichten vom Topic `orders` und verarbeitet diese, indem es eine Rückmeldung sendet.
- **Testen:**  
  - Verwende Postman oder curl für den REST-Endpoint.
  - Überwache die Container-Logs, um den Nachrichtenfluss in Kafka zu überprüfen.
  - Prüfe die Datenbank, um zu bestätigen, dass die Bestellungen gespeichert wurden.

Diese Anleitung hilft dir dabei, sicherzustellen, dass alle Kernkomponenten deiner Lösung funktionieren. Wenn Simeonlin das Docker Compose File verwendet, wird er – sofern deine Images in GHCR aktuell sind – direkt in die produktionsähnliche Umgebung starten und die gesamte Funktionalität (REST, DB, Kafka) testen können.
- Überprüfe in den Logs von orders-service und inventory-service, ob die Nachricht korrekt verarbeitet wird.
- Optional: Überprüfe in der Postgres-Datenbank, ob die Bestellung gespeichert wurde.
