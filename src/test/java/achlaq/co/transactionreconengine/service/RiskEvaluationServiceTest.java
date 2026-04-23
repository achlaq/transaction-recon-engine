package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.risk.config.RiskRulesConfig;
import achlaq.co.transactionreconengine.risk.dto.RiskRule;
import achlaq.co.transactionreconengine.core.dto.TransactionEvent;
import achlaq.co.transactionreconengine.risk.service.RiskEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskEvaluationServiceTest {

    private RiskEvaluationService riskEvaluationService;

    @BeforeEach
    void setUp() {
        RiskRulesConfig config = new RiskRulesConfig();
        // Setup mock rules same as application.properties defaults
        config.setRules(List.of(
                new RiskRule(new BigDecimal("100000000"), "HIGH", "FRAUD_DETECTED", "High Value Transaction Exceeded"),
                new RiskRule(new BigDecimal("999999"), "MEDIUM", "REVIEW_NEEDED", "Suspicious Medium Value")
        ));
        riskEvaluationService = new RiskEvaluationService(config);
    }

    @Test
    void evaluateRisk_NormalTransaction_ReturnsLowRisk() {
        TransactionEvent event = new TransactionEvent();
        event.setAmount(new BigDecimal("50000"));
        event.setCurrency("IDR");

        RiskRule result = riskEvaluationService.evaluateRisk(event);

        assertEquals("LOW", result.getRiskLevel());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void evaluateRisk_MediumAmount_ReturnsMediumRisk() {
        TransactionEvent event = new TransactionEvent();
        event.setAmount(new BigDecimal("1000000")); // 1 million
        event.setCurrency("IDR");

        RiskRule result = riskEvaluationService.evaluateRisk(event);

        assertEquals("MEDIUM", result.getRiskLevel());
        assertEquals("REVIEW_NEEDED", result.getStatus());
    }

    @Test
    void evaluateRisk_HighAmount_ReturnsHighRisk() {
        TransactionEvent event = new TransactionEvent();
        event.setAmount(new BigDecimal("150000000")); // 150 million
        event.setCurrency("IDR");

        RiskRule result = riskEvaluationService.evaluateRisk(event);

        assertEquals("HIGH", result.getRiskLevel());
        assertEquals("FRAUD_DETECTED", result.getStatus());
    }

    @Test
    void evaluateRisk_NormalAmountForeignCurrency_ReturnsMediumRisk() {
        TransactionEvent event = new TransactionEvent();
        event.setAmount(new BigDecimal("50000"));
        event.setCurrency("USD");

        RiskRule result = riskEvaluationService.evaluateRisk(event);

        assertEquals("MEDIUM", result.getRiskLevel());
        assertEquals("REVIEW_NEEDED", result.getStatus());
    }

    @Test
    void evaluateRisk_MediumAmountForeignCurrency_ReturnsHighRisk() {
        TransactionEvent event = new TransactionEvent();
        event.setAmount(new BigDecimal("1000000"));
        event.setCurrency("SGD");

        RiskRule result = riskEvaluationService.evaluateRisk(event);

        assertEquals("HIGH", result.getRiskLevel());
        assertEquals("FRAUD_DETECTED", result.getStatus());
    }

    @Test
    void evaluateRisk_HighAmountForeignCurrency_ReturnsCriticalRisk() {
        TransactionEvent event = new TransactionEvent();
        event.setAmount(new BigDecimal("150000000"));
        event.setCurrency("EUR");

        RiskRule result = riskEvaluationService.evaluateRisk(event);

        assertEquals("CRITICAL", result.getRiskLevel());
        assertEquals("FROZEN", result.getStatus());
    }
}