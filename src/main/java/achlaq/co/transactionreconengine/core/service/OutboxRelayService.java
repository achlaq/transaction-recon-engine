package achlaq.co.transactionreconengine.core.service;

import achlaq.co.transactionreconengine.core.model.OutboxEventEntity;
import achlaq.co.transactionreconengine.core.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "1000") // Run every 1 second
    @Transactional
    public void relayEvents() {
        List<OutboxEventEntity> events = outboxEventRepository.findByProcessedFalse();
        
        for (OutboxEventEntity event : events) {
            try {
                // Send payload exactly as it is (it was pre-serialized to JSON in processor)
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());
                event.setProcessed(true);
                outboxEventRepository.save(event);
                log.info("Relayed outbox event ID {} to topic {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to relay outbox event ID {}", event.getId(), e);
                // Do not mark as processed if sending fails
            }
        }
    }
}
