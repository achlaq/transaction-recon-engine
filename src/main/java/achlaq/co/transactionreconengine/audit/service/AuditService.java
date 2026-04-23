package achlaq.co.transactionreconengine.audit.service;

import achlaq.co.transactionreconengine.audit.document.AuditLogDocument;
import achlaq.co.transactionreconengine.core.dto.TransactionEvent;
import achlaq.co.transactionreconengine.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.audit-dlq:audit-events-dlq}")
    private String auditDlqTopic;

    public void saveAuditLogToElastic(TransactionEvent event, String action, String riskLevel, String metadata) {
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
            log.error("Failed to save Audit Log to Elastic for ReqID: {}. Sending to DLQ.", event.getRequestId(), e);
            sendToAuditDlq(event, action, riskLevel, metadata, e.getMessage());
        }
    }

    private void sendToAuditDlq(TransactionEvent event, String action, String riskLevel, String metadata, String errorMsg) {
        try {
             kafkaTemplate.send(auditDlqTopic, event.getRequestId(), event);
             log.info("Successfully sent Audit Log to DLQ for ReqID: {}", event.getRequestId());
        } catch (Exception ex) {
             log.error("CRITICAL: Failed to send Audit Log to DLQ for ReqID: {}", event.getRequestId(), ex);
        }
    }
}