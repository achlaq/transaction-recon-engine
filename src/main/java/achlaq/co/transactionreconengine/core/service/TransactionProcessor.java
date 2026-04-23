package achlaq.co.transactionreconengine.core.service;

import achlaq.co.transactionreconengine.risk.dto.RiskRule;
import achlaq.co.transactionreconengine.core.dto.TransactionEvent;
import achlaq.co.transactionreconengine.ledger.dto.LedgerEvent;
import achlaq.co.transactionreconengine.ledger.dto.LedgerEventEntry;
import achlaq.co.transactionreconengine.ledger.model.EntryType;
import achlaq.co.transactionreconengine.core.model.OutboxEventEntity;
import achlaq.co.transactionreconengine.core.model.TransactionEntity;
import achlaq.co.transactionreconengine.recon.dto.ExternalSnapshotRequest;
import achlaq.co.transactionreconengine.recon.service.ReconciliationService;
import achlaq.co.transactionreconengine.core.repository.OutboxEventRepository;
import achlaq.co.transactionreconengine.core.repository.TransactionRepository;
import achlaq.co.transactionreconengine.audit.service.AuditService;
import achlaq.co.transactionreconengine.risk.service.RiskEvaluationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final TransactionRepository transactionRepo;
    private final OutboxEventRepository outboxEventRepo;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;
    private final RiskEvaluationService riskEvaluationService;
    private final AuditService auditService;
    private final ReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.ledger}")
    private String ledgerTopic;

    @Transactional
    public void process(TransactionEvent event) {
        String requestId = event.getRequestId();
        Long userId = event.getUserId();

        String lockKey = "LOCK::" + requestId;
        Boolean isLocked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("Duplicate Transaction processing detected via Redis Lock: {}", requestId);
            return;
        }

        try {
            if (rateLimitService.isUserBlacklisted(userId)) {
                auditService.saveAuditLogToElastic(event, "REJECTED_BLACKLIST", "HIGH", "User is in blacklist");
                redisTemplate.opsForValue().set(lockKey, "PROCESSED", Duration.ofHours(24));
                return;
            }

            if (rateLimitService.isRateLimited(userId)) {
                auditService.saveAuditLogToElastic(event, "REJECTED_RATE_LIMIT", "HIGH", "Velocity limit exceeded");
                redisTemplate.opsForValue().set(lockKey, "PROCESSED", Duration.ofHours(24));
                return;
            }

            RiskRule matchedRule = riskEvaluationService.evaluateRisk(event);

            try {
                saveTransactionAndAudit(event, matchedRule);
            } catch (DataIntegrityViolationException e) {
                log.warn("Duplicate Transaction saved to database: {}", requestId);
                redisTemplate.opsForValue().set(lockKey, "PROCESSED", Duration.ofHours(24));
                return;
            }

            // Integrate with Ledger & Recon ONLY if the transaction is successful (LOW risk)
            if ("SUCCESS".equals(matchedRule.getStatus())) {
                 saveLedgerEventToOutbox(event);
                 createReconSnapshot(event);
            }

            redisTemplate.opsForValue().set(lockKey, "PROCESSED", Duration.ofHours(24));

        } catch (Exception e) {
            redisTemplate.delete(lockKey); // Delete lock only on error so it can be retried
            throw e;
        }
    }

    private void saveTransactionAndAudit(TransactionEvent event, RiskRule rule) {
        TransactionEntity entity = new TransactionEntity();
        entity.setRequestId(event.getRequestId());
        entity.setUserId(event.getUserId());
        entity.setAmount(event.getAmount());
        entity.setTimestamp(LocalDateTime.now());
        entity.setStatus(rule.getStatus());

        transactionRepo.saveAndFlush(entity);

        auditService.saveAuditLogToElastic(event, rule.getStatus(), rule.getRiskLevel(), rule.getReason());

        log.info("Tx Processed | ID: {} | Status: {}", event.getRequestId(), rule.getStatus());
    }

    private void saveLedgerEventToOutbox(TransactionEvent event) {
        LedgerEvent ledgerEvent = new LedgerEvent();
        ledgerEvent.setJournalId(event.getRequestId()); 
        ledgerEvent.setReferenceId(event.getRequestId());
        ledgerEvent.setDescription("Auto-generated journal for Tx: " + event.getRequestId());

        LedgerEventEntry debitEntry = new LedgerEventEntry();
        debitEntry.setAccountCode("CLEARING"); 
        debitEntry.setEntryType(EntryType.DEBIT);
        debitEntry.setAmount(event.getAmount());
        debitEntry.setDescription("Money in from clearing");

        LedgerEventEntry creditEntry = new LedgerEventEntry();
        creditEntry.setAccountCode(event.getTargetAccount()); 
        creditEntry.setEntryType(EntryType.CREDIT);
        creditEntry.setAmount(event.getAmount());
        creditEntry.setDescription("Money out to target");

        ledgerEvent.setEntries(List.of(debitEntry, creditEntry));

        try {
            OutboxEventEntity outboxEvent = new OutboxEventEntity();
            outboxEvent.setAggregateType("Transaction");
            outboxEvent.setAggregateId(event.getRequestId());
            outboxEvent.setEventType(ledgerTopic);
            outboxEvent.setPayload(objectMapper.writeValueAsString(ledgerEvent));
            
            outboxEventRepo.save(outboxEvent);
            log.info("Saved Ledger Event to Outbox for Tx: {}", event.getRequestId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ledger event for outbox", e);
            throw new RuntimeException("Failed to serialize ledger event", e);
        }
    }

    private void createReconSnapshot(TransactionEvent event) {
         try {
             ExternalSnapshotRequest snapshot = new ExternalSnapshotRequest();
             snapshot.setSourceSystem("INTERNAL_SWITCH"); // Simulating the source system
             snapshot.setReferenceId(event.getRequestId()); // Must match the Journal ID for recon to work
             snapshot.setAmount(event.getAmount());
             snapshot.setCurrency(event.getCurrency());
             snapshot.setEventTime(LocalDateTime.now());
             
             reconciliationService.ingestSnapshot(snapshot);
             log.info("Created Auto-Recon Snapshot for Tx: {}", event.getRequestId());
         } catch (Exception e) {
             log.error("Failed to auto-create recon snapshot for Tx: {}", event.getRequestId(), e);
         }
    }
}