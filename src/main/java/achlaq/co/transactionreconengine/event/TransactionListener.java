package achlaq.co.transactionreconengine.event;

import achlaq.co.transactionreconengine.core.dto.TransactionEvent;
import achlaq.co.transactionreconengine.core.service.TransactionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionListener {

    private final TransactionProcessor processor;

    @KafkaListener(
            topics = "${app.kafka.topics.transactions}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onTransaction(TransactionEvent event) {
        log.info("Received event: {}", event.getRequestId());
        processor.process(event);
    }
}
