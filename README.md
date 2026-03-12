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

## Kafka Topics
- `trx-events` untuk transaksi.
- `trx-events-dlq` untuk dead-letter (pesan gagal setelah retry).

Jika auto topic creation dimatikan, buat topic ini manual terlebih dulu.

## Observability
Actuator endpoint:
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Konfigurasi
Lihat `src/main/resources/application.properties` untuk konfigurasi DB, Kafka, Redis, dan Elasticsearch.
