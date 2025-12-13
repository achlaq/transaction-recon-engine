package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.document.AuditLogDocument;
import achlaq.co.transactionreconengine.dto.RiskRule;
import achlaq.co.transactionreconengine.dto.TransactionEvent;
import achlaq.co.transactionreconengine.model.TransactionEntity;
import achlaq.co.transactionreconengine.repository.AuditLogRepository;
import achlaq.co.transactionreconengine.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final TransactionRepository transactionRepo;
    private final AuditLogRepository auditLogRepo;
    private final StringRedisTemplate redisTemplate;

    private final List<RiskRule> RISK_RULES = List.of(
            new RiskRule(new BigDecimal("100000000"), "HIGH", "FRAUD_DETECTED", "High Value Transaction Exceeded"),
            new RiskRule(new BigDecimal("999999"), "MEDIUM", "REVIEW_NEEDED", "Suspicious Medium Value")
    );
    private static final int MAX_TX_PER_MINUTE = 5;

    public void process(TransactionEvent event) {
        String requestId = event.getRequestId();
        Long userId = event.getUserId();
        BigDecimal amount = event.getAmount();

        String lockKey = "LOCK::" + requestId;
        Boolean isLocked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("Duplicate Transaction: {}", requestId);
            return;
        }

        if (isUserBlacklisted(userId)) {
            saveAuditLogToElastic(event, "REJECTED_BLACKLIST", "HIGH", "User is in blacklist");
            return;
        }

        if (isRateLimited(userId)) {
            saveAuditLogToElastic(event, "REJECTED_RATE_LIMIT", "HIGH", "Velocity limit exceeded");
            return;
        }

        RiskRule matchedRule = RISK_RULES.stream()
                .filter(rule -> amount.compareTo(rule.getLimit()) >= 0)
                .max(Comparator.comparing(RiskRule::getLimit))
                .orElse(new RiskRule(BigDecimal.ZERO, "LOW", "SUCCESS", "Normal Transaction"));

        saveTransactionAndAudit(event, matchedRule);
    }

    private void saveTransactionAndAudit(TransactionEvent event, RiskRule rule) {
        TransactionEntity entity = new TransactionEntity();
        entity.setRequestId(event.getRequestId());
        entity.setUserId(event.getUserId());
        entity.setAmount(event.getAmount());
        entity.setTimestamp(LocalDateTime.now());
        entity.setStatus(rule.getStatus());

        transactionRepo.save(entity);

        saveAuditLogToElastic(event, rule.getStatus(), rule.getRiskLevel(), rule.getReason());

        log.info("Tx Processed | ID: {} | Status: {}", event.getRequestId(), rule.getStatus());
    }

    private void saveAuditLogToElastic(TransactionEvent event, String action, String riskLevel, String metadata) {
        try {
            AuditLogDocument doc = AuditLogDocument.builder()
                    .requestId(event.getRequestId())
                    .userId(event.getUserId())
                    .action(action)
                    .riskLevel(riskLevel)
                    .metadata(metadata)
                    .amountSnapshot(event.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepo.save(doc);

        } catch (Exception e) {
            log.error("Gagal menyimpan Audit Log ke Elastic untuk ReqID: {}", event.getRequestId(), e);
        }
    }

    private boolean isRateLimited(Long userId) {
        String key = "VELOCITY::" + String.valueOf(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        return count != null && count > MAX_TX_PER_MINUTE;
    }

    private boolean isUserBlacklisted(Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember("BLACKLIST_USERS", String.valueOf(userId))
        );
    }
}