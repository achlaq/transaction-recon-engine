# Transaction Recon Engine

Transaction Recon Engine adalah layanan Spring Boot untuk menerima transaksi via REST, memproses event lewat Kafka, menyimpan transaksi ke Postgres, serta audit log ke Elasticsearch. Redis digunakan untuk blacklist dan rate limit.

## Prasyarat
- Java 17
- Docker + Docker Compose

## Menjalankan Dependencies
```bash
docker-compose up -d
```

## Menjalankan Aplikasi
```bash
./mvnw spring-boot:run
```

Server berjalan di `http://localhost:7654`.

## Endpoint Utama
- `POST /api/v1/transactions` ingest transaksi ke Kafka.
- `GET /api/v1/audit-logs` pagination audit log.
- `GET /api/v1/audit-logs/user/{userId}`
- `GET /api/v1/audit-logs/risk-level/{riskLevel}`
- `GET /api/v1/audit-logs/search?userId={userId}`
- `GET /api/v1/analytics/high-value-users`
- `POST /api/v1/blacklist/{userId}`
- `DELETE /api/v1/blacklist/{userId}`
- `GET /api/v1/blacklist/{userId}`
- `POST /api/v1/ledger/accounts`
- `POST /api/v1/ledger/journals`
- `POST /api/v1/recon/snapshots`
- `POST /api/v1/recon/run/{sourceSystem}/{referenceId}`
- `GET /api/v1/recon/results?status={status}`

## Contoh Request
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

## Contoh Journal Posting
```bash
curl -X POST http://localhost:7654/api/v1/ledger/accounts \
  -H "Content-Type: application/json" \
  -d '{"code":"CASH","name":"Cash Account"}'

curl -X POST http://localhost:7654/api/v1/ledger/accounts \
  -H "Content-Type: application/json" \
  -d '{"code":"REV","name":"Revenue Account"}'

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

## Contoh Rekonsiliasi
```bash
curl -X POST http://localhost:7654/api/v1/recon/snapshots \
  -H "Content-Type: application/json" \
  -d '{
    "sourceSystem": "switch",
    "referenceId": "jrn-001",
    "amount": 100000,
    "currency": "IDR",
    "eventTime": "2025-01-01T10:00:00"
  }'

curl "http://localhost:7654/api/v1/recon/results?status=MATCHED"
```

## Kafka Topics
- `trx-events` untuk transaksi.
- `trx-events-dlq` untuk dead-letter (pesan gagal setelah retry).
- `ledger-events` untuk posting journal.
- `ledger-events-dlq` untuk dead-letter ledger.

Jika auto topic creation dimatikan, buat topic ini manual terlebih dulu.

## Observability
Actuator endpoint:
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Konfigurasi
Lihat `src/main/resources/application.properties` untuk konfigurasi DB, Kafka, Redis, dan Elasticsearch.
