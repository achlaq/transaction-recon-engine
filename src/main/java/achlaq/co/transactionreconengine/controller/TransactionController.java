package achlaq.co.transactionreconengine.controller;

import achlaq.co.transactionreconengine.dto.TransactionEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${app.kafka.topics.transactions}")
    private String transactionsTopic;

    @PostMapping
    public String createTransaction(@Valid @RequestBody TransactionEvent event) {
        kafkaTemplate.send(transactionsTopic, event.getRequestId(), event);
        return "Transaction Queued with ID: " + event.getRequestId();
    }
}
