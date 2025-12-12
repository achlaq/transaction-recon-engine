package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.dto.AuditLog;
import achlaq.co.transactionreconengine.dto.TransactionEvent;
import achlaq.co.transactionreconengine.model.*;
import achlaq.co.transactionreconengine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final TransactionRepository transactionRepo;
    private final AuditLogRepository auditLogRepo;
    private final StringRedisTemplate redisTemplate;

    private final List<BigDecimal> SUSPICIOUS_AMOUNTS = List.of(
            new BigDecimal("999999"),
            new BigDecimal("100000000")
    );

    public void process(TransactionEvent event) {
        String lockKey = "LOCK::" + event.getRequestId();
        Boolean isLocked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("Duplicate Transaction Detected: {}", event.getRequestId());
            return;
        }

        boolean isFraud = SUSPICIOUS_AMOUNTS.stream()
                .anyMatch(limit -> event.getAmount().compareTo(limit) >= 0);

        TransactionEntity entity = new TransactionEntity();
        entity.setRequestId(event.getRequestId());
        entity.setUserId(event.getUserId());
        entity.setAmount(event.getAmount());
        entity.setTimestamp(LocalDateTime.now());
        entity.setStatus(isFraud ? "FRAUD_DETECTED" : "SUCCESS");

        transactionRepo.save(entity);
        log.info("Transaction processed. Status: {}", entity.getStatus());

        AuditLog audit = new AuditLog();
        audit.setRequestId(event.getRequestId());
        audit.setAction("TRANSACTION_PROCESSED");
        audit.setRiskLevel(isFraud ? "HIGH" : "LOW");
        audit.setMetadata("User " + event.getUserId() + " sent " + event.getAmount());

        auditLogRepo.save(audit);
    }
}
