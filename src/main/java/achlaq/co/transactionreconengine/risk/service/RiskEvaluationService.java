package achlaq.co.transactionreconengine.risk.service;

import achlaq.co.transactionreconengine.risk.config.RiskRulesConfig;
import achlaq.co.transactionreconengine.risk.dto.RiskRule;
import achlaq.co.transactionreconengine.core.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class RiskEvaluationService {

    private final RiskRulesConfig riskRulesConfig;
    
    @Value("${app.risk.base-currency}")
    private String baseCurrency;

    public RiskRule evaluateRisk(TransactionEvent event) {
        BigDecimal amount = event.getAmount();
        String currency = event.getCurrency();

        // 1. Evaluate based on amount limits from configuration
        // If the rules are empty (not configured), it will default to LOW risk
        RiskRule amountBasedRule = riskRulesConfig.getRules().stream()
                .filter(rule -> amount.compareTo(rule.getLimit()) >= 0)
                .max(Comparator.comparing(RiskRule::getLimit))
                .orElse(new RiskRule(BigDecimal.ZERO, "LOW", "SUCCESS", "Normal Transaction"));

        // 2. Additional Reality Checks (e.g., Cross-border / Currency Risk)
        // If it's a very high amount AND not in base currency, elevate risk
        if ("HIGH".equals(amountBasedRule.getRiskLevel()) && !baseCurrency.equalsIgnoreCase(currency)) {
             return new RiskRule(
                     amountBasedRule.getLimit(),
                     "CRITICAL",
                     "FROZEN",
                     "Critical: High value cross-border transaction detected"
             );
        }

        // If it's a medium amount but in a potentially volatile/foreign currency, elevate to HIGH
        if ("MEDIUM".equals(amountBasedRule.getRiskLevel()) && !baseCurrency.equalsIgnoreCase(currency)) {
             return new RiskRule(
                     amountBasedRule.getLimit(),
                     "HIGH",
                     "FRAUD_DETECTED", // Or require manual review
                     "High: Medium value transaction in foreign currency"
             );
        }
        
        // If it is a normal transaction but not in base currency, elevate to MEDIUM for review
        if ("LOW".equals(amountBasedRule.getRiskLevel()) && !baseCurrency.equalsIgnoreCase(currency)) {
             return new RiskRule(
                     new BigDecimal("0"),
                     "MEDIUM",
                     "REVIEW_NEEDED",
                     "Review: Normal value but foreign currency"
             );
        }

        return amountBasedRule;
    }
}
