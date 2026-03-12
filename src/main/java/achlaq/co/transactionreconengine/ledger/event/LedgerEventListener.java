package achlaq.co.transactionreconengine.ledger.event;

import achlaq.co.transactionreconengine.ledger.dto.LedgerEvent;
import achlaq.co.transactionreconengine.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LedgerEventListener {

    private final LedgerService ledgerService;

    @KafkaListener(
            topics = "${app.kafka.topics.ledger}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onLedgerEvent(LedgerEvent event) {
        log.info("Received ledger event: {}", event.getJournalId());
        ledgerService.postJournalFromEvent(event);
    }
}
