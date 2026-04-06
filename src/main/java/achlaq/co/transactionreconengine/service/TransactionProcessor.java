package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.dto.RiskRule;
import achlaq.co.transactionreconengine.dto.TransactionEvent;
import achlaq.co.transactionreconengine.ledger.dto.LedgerEvent;
import achlaq.co.transactionreconengine.ledger.dto.LedgerEventEntry;
import achlaq.co.transactionreconengine.ledger.model.EntryType;
import achlaq.co.transactionreconengine.model.TransactionEntity;
import achlaq.co.transactionreconengine.recon.dto.ExternalSnapshotRequest;
import achlaq.co.transactionreconengine.recon.service.ReconciliationService;
import achlaq.co.transactionreconengine.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;
    private final RiskEvaluationService riskEvaluationService;
    private final AuditService auditService;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReconciliationService reconciliationService;

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

            // Integrate with Ledger & Recon ONLY if the transaction is successful (LOW risk)
            if ("SUCCESS".equals(matchedRule.getStatus())) {
                 publishLedgerEvent(event);
                 createReconSnapshot(event);
            }

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

    private void publishLedgerEvent(TransactionEvent event) {
        LedgerEvent ledgerEvent = new LedgerEvent();
        // Use the transaction requestId as the journalId to link them 1-to-1
        ledgerEvent.setJournalId(event.getRequestId()); 
        ledgerEvent.setReferenceId(event.getRequestId());
        ledgerEvent.setDescription("Auto-generated journal for Tx: " + event.getRequestId());

        LedgerEventEntry debitEntry = new LedgerEventEntry();
        // Assuming we have a standard CLEARING account setup in the database
        debitEntry.setAccountCode("CLEARING"); 
        debitEntry.setEntryType(EntryType.DEBIT);
        debitEntry.setAmount(event.getAmount());
        debitEntry.setDescription("Money in from clearing");

        LedgerEventEntry creditEntry = new LedgerEventEntry();
        // The target account from the transaction
        creditEntry.setAccountCode(event.getTargetAccount()); 
        creditEntry.setEntryType(EntryType.CREDIT);
        creditEntry.setAmount(event.getAmount());
        creditEntry.setDescription("Money out to target");

        ledgerEvent.setEntries(List.of(debitEntry, creditEntry));

        // Send to Kafka so the Ledger Module can pick it up asynchronously
        kafkaTemplate.send(ledgerTopic, ledgerEvent.getJournalId(), ledgerEvent);
        log.info("Published Ledger Event for Tx: {}", event.getRequestId());
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