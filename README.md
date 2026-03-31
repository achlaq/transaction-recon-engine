# 🚀 Transaction Recon Engine

Welcome to the **Transaction Recon Engine**! This Spring Boot service is your one-stop solution for handling financial transactions with style. It ingests transactions via REST, processes events using Kafka, stores data in PostgreSQL, and keeps detailed audit logs in Elasticsearch. To top it off, we use Redis for blacklisting and rate limiting.️

## 🛠️ Prerequisites
- Java 17
- Docker + Docker Compose

## 🐳 Running Dependencies
Get all the necessary services (Postgres, Kafka, etc.) up and running with a single command:
```bash
docker compose up -d
```

## ▶️ Running the Application
Fire up the engine with:
```bash
./mvnw spring-boot:run
```
The server will be live at `http://localhost:7654`.

## 📖 API Documentation
Once the application is running, you can explore the API documentation at:
[http://localhost:7654/swagger-ui.html](http://localhost:7654/swagger-ui.html)

## 🎯 Core Endpoints
Here are the key endpoints to interact with the engine:

- `POST /api/v1/transactions`: Ingest a new transaction into Kafka.
- `GET /api/v1/audit-logs`: Paginated audit logs.
- `GET /api/v1/audit-logs/user/{userId}`: Get logs for a specific user.
- `GET /api/v1/audit-logs/risk-level/{riskLevel}`: Filter logs by risk level.
- `GET /api/v1/audit-logs/search?userId={userId}`: Search logs.
- `GET /api/v1/analytics/high-value-users`: Identify high-value users.
- `POST /api/v1/blacklist/{userId}`: Add a user to the blacklist.
- `DELETE /api/v1/blacklist/{userId}`: Remove a user from the blacklist.
- `GET /api/v1/blacklist/{userId}`: Check if a user is blacklisted.
- `POST /api/v1/ledger/accounts`: Create a new ledger account.
- `POST /api/v1/ledger/journals`: Post a new journal entry.
- `POST /api/v1/recon/snapshots`: Create a reconciliation snapshot.
- `POST /api/v1/recon/run/{sourceSystem}/{referenceId}`: Run reconciliation.
- `GET /api/v1/recon/results?status={status}`: Get reconciliation results.

## 📝 Sample Requests

### Transaction Ingestion
```bash
curl -X POST http://localhost:7654/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "req-123",
    "userId": 1001,
    "amount": 120000,
    "currency": "IDR",
    "targetAccount": "acct-991"
  }'
```

### Journal Posting
```bash
# Create accounts first
curl -X POST http://localhost:7654/api/v1/ledger/accounts \
  -H "Content-Type: application/json" \
  -d '{"code":"CASH","name":"Cash Account"}'

curl -X POST http://localhost:7654/api/v1/ledger/accounts \
  -H "Content-Type: application/json" \
  -d '{"code":"REV","name":"Revenue Account"}'

# Post the journal
curl -X POST http://localhost:7654/api/v1/ledger/journals \
  -H "Content-Type: application/json" \
  -d '{
    "journalId": "jrn-001",
    "referenceId": "req-123",
    "description": "Sample posting",
    "entries": [
      { "accountId": 1, "entryType": "DEBIT", "amount": 100000, "description": "Cash in" },
      { "accountId": 2, "entryType": "CREDIT", "amount": 100000, "description": "Revenue" }
    ]
  }'
```

### Reconciliation
```bash
# Create a snapshot
curl -X POST http://localhost:7654/api/v1/recon/snapshots \
  -H "Content-Type: application/json" \
  -d '{
    "sourceSystem": "switch",
    "referenceId": "jrn-001",
    "amount": 100000,
    "currency": "IDR",
    "eventTime": "2025-01-01T10:00:00"
  }'

# Check results
curl "http://localhost:7654/api/v1/recon/results?status=MATCHED"
```

## 📨 Kafka Topics
- `trx-events`: For main transaction events.
- `trx-events-dlq`: Dead-letter queue for failed transaction events.
- `ledger-events`: For journal posting events.
- `ledger-events-dlq`: Dead-letter queue for ledger events.

If auto topic creation is disabled, please create these topics manually.

## 📊 Observability
Check the health and metrics of the service with our Actuator endpoints:
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## ⚙️ Configuration
All configurations for the database, Kafka, Redis, and Elasticsearch can be found in `src/main/resources/application.properties`.
