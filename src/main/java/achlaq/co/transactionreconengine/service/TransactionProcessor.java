package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.dto.RiskRule;
import achlaq.co.transactionreconengine.dto.TransactionEvent;
import achlaq.co.transactionreconengine.model.TransactionEntity;
import achlaq.co.transactionreconengine.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final TransactionRepository transactionRepo;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;
    private final RiskEvaluationService riskEvaluationService;
    private final AuditService auditService;

    @Transactional
    public void process(TransactionEvent event) {
        String requestId = event.getRequestId();
        Long userId = event.getUserId();

        String lockKey = "LOCK::" + requestId;
        Boolean isLocked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("Duplicate Transaction: {}", requestId);
            return;
        }

        try {
            if (rateLimitService.isUserBlacklisted(userId)) {
                auditService.saveAuditLogToElastic(event, "REJECTED_BLACKLIST", "HIGH", "User is in blacklist");
                return;
            }

            if (rateLimitService.isRateLimited(userId)) {
                auditService.saveAuditLogToElastic(event, "REJECTED_RATE_LIMIT", "HIGH", "Velocity limit exceeded");
                return;
            }

            RiskRule matchedRule = riskEvaluationService.evaluateRisk(event);

            saveTransactionAndAudit(event, matchedRule);

        } finally {
            try {
                redisTemplate.delete(lockKey);
            } catch (Exception e) {
                log.error("Failed to release Redis lock for key: {}", lockKey, e);
            }
        }
    }

    private void saveTransactionAndAudit(TransactionEvent event, RiskRule rule) {
        TransactionEntity entity = new TransactionEntity();
        entity.setRequestId(event.getRequestId());
        entity.setUserId(event.getUserId());
        entity.setAmount(event.getAmount());
        entity.setTimestamp(LocalDateTime.now());
        entity.setStatus(rule.getStatus());

        transactionRepo.save(entity);

        auditService.saveAuditLogToElastic(event, rule.getStatus(), rule.getRiskLevel(), rule.getReason());

        log.info("Tx Processed | ID: {} | Status: {}", event.getRequestId(), rule.getStatus());
    }
}