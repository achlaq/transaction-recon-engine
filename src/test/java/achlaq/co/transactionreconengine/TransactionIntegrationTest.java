package achlaq.co.transactionreconengine;

import achlaq.co.transactionreconengine.dto.TransactionEvent;
import achlaq.co.transactionreconengine.model.TransactionEntity;
import achlaq.co.transactionreconengine.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Testcontainers
class TransactionIntegrationTest {

    // 1. Setup PostgreSQL Container
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    // 2. Setup Kafka Container
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    // 3. Setup Redis Container
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:alpine"))
            .withExposedPorts(6379);

    // 4. Setup Elasticsearch Container
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    // 5. Override Spring properties dynamically based on containers' ports
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldProcessTransactionSuccessfully() {
        // Arrange
        String requestId = UUID.randomUUID().toString();
        TransactionEvent event = new TransactionEvent();
        event.setRequestId(requestId);
        event.setUserId(1001L);
        event.setAmount(new BigDecimal("50000")); // Amount is below threshold, so it should be LOW risk
        event.setCurrency("IDR"); // Base currency
        event.setTargetAccount("acct-991");

        // Act: Send message to Kafka topic
        kafkaTemplate.send("trx-events", event.getRequestId(), event);

        // Assert: Wait until message is consumed and saved into PostgreSQL
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<TransactionEntity> transactions = transactionRepository.findAll();
            
            // Verifikasi bahwa transaksi berhasil masuk ke database
            assertThat(transactions).isNotEmpty();
            
            // Verifikasi detail transaksi
            TransactionEntity savedTx = transactions.stream()
                    .filter(tx -> tx.getRequestId().equals(requestId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Transaction not found!"));

            assertThat(savedTx.getStatus()).isEqualTo("SUCCESS");
            assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        });
    }
}