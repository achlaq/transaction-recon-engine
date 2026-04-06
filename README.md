# Transaction Recon Engine

A robust, event-driven financial transaction processing and reconciliation engine built with Spring Boot. 

This service is designed to handle high-volume financial transactions asynchronously. It ingests transactions via REST, processes them through a multi-stage security and risk evaluation pipeline, records movements in a double-entry ledger, and performs automated reconciliation against external systems.

## Architecture Overview

The system utilizes an event-driven choreography pattern to ensure high availability, decoupling, and fault tolerance:

1. **Ingestion & Validation:** Transactions are ingested via REST API and immediately pushed to Apache Kafka, returning a fast acknowledgment to the client.
2. **Security & Risk Pipeline:** A background processor consumes events, applies distributed locks (Redis) to prevent race conditions, checks for rate limits and blacklists, and evaluates risk based on amount and currency.
3. **Persistence & Audit:** Valid transactions are stored in PostgreSQL. Every decision (success, rejected, frozen) is audited and indexed in Elasticsearch.
4. **Asynchronous Ledger:** Successful transactions publish events to a ledger topic, triggering a separate module to record double-entry bookkeeping (Debit/Credit).
5. **Automated Reconciliation:** The system simulates receiving external snapshots and automatically attempts to match them against internal ledger entries.

## Tech Stack

* **Backend Framework:** Java 17, Spring Boot 3
* **Message Broker:** Apache Kafka (Event streaming, DLQs)
* **Database & Migration:** PostgreSQL, Flyway (Schema management & data seeding)
* **Caching & Locking:** Redis (Distributed locks, rate limiting)
* **Search & Audit:** Elasticsearch (High-speed audit trails)
* **Observability:** Prometheus, Spring Boot Actuator
* **Testing:** Testcontainers, JUnit 5, Awaitility

## Prerequisites

Ensure you have the following installed before running the application:
* Java 17+
* Maven
* Docker & Docker Compose (required for running infrastructure and Testcontainers)

## Running the Application

1. **Start Infrastructure Services**
   Spin up the required services (PostgreSQL, Kafka, Zookeeper, Redis, Elasticsearch) using Docker Compose:
   ```bash
   docker compose up -d
   ```
   Wait a few moments for all containers to become healthy.

2. **Run the Spring Boot Service**
   Start the application. Flyway will automatically execute database migrations and seed initial Chart of Accounts (COA) data.
   ```bash
   ./mvnw spring-boot:run
   ```
   The service will be available at `http://localhost:7654`.

## API Documentation

Once the application is running, the Swagger UI is available at:
[http://localhost:7654/swagger-ui.html](http://localhost:7654/swagger-ui.html)

## End-to-End Testing Guide

Here is a quick guide to test the complete flow (Transaction -> Ledger -> Reconciliation).

### 1. Submit a Transaction
Send a transaction request. The target account `CASH` is pre-seeded by Flyway.
```bash
curl -X POST http://localhost:7654/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "trx-demo-001",
    "userId": 1001,
    "amount": 50000,
    "currency": "IDR",
    "targetAccount": "CASH"
  }'
```

### 2. Verify Reconciliation Results
Because the transaction is marked as SUCCESS, the engine automatically creates ledger entries and an external snapshot, then runs reconciliation. Check the result:
```bash
curl "http://localhost:7654/api/v1/recon/results?status=MATCHED"
```
You should see a JSON array containing the matched result for `trx-demo-001`.

### 3. Test Security Constraints (Blacklist & Rate Limiting)
Add a user to the blacklist:
```bash
curl -X POST http://localhost:7654/api/v1/blacklist/9999
```
Attempt a transaction with that user:
```bash
curl -X POST http://localhost:7654/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "trx-fraud-001",
    "userId": 9999,
    "amount": 100000,
    "currency": "IDR",
    "targetAccount": "CASH"
  }'
```
The transaction will be rejected silently by the background processor, audited in Elasticsearch, and will not generate ledger or reconciliation events.

## Running Automated Tests

The project includes integration tests that spin up temporary Docker containers using **Testcontainers**. Make sure Docker Desktop is running, then execute:

```bash
./mvnw test
```

## Project Structure Highlights

* `/service`: Contains the core business logic (`TransactionProcessor`, `RiskEvaluationService`, `RateLimitService`, `AuditService`).
* `/ledger`: An independent module handling double-entry accounting, listening to Kafka events to post journals.
* `/recon`: A module dedicated to comparing internal ledger records with external snapshots.
* `/config`: Configurations for Kafka, Risk Rules, and OpenAPI.
* `db/migration`: Flyway SQL scripts for schema versioning and data seeding.
