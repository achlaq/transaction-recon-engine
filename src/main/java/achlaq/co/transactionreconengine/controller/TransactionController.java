package achlaq.co.transactionreconengine.controller;

import achlaq.co.transactionreconengine.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping
    public String createTransaction(@RequestBody TransactionEvent event) {
        kafkaTemplate.send("trx-events", event.getRequestId(), event);
        return "Transaction Queued with ID: " + event.getRequestId();
    }
}
