package achlaq.co.transactionreconengine.config;

import achlaq.co.transactionreconengine.dto.RiskRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.risk")
public class RiskRulesConfig {
    private List<RiskRule> rules = List.of(
            new RiskRule(new BigDecimal("100000000"), "HIGH", "FRAUD_DETECTED", "High Value Transaction Exceeded"),
            new RiskRule(new BigDecimal("999999"), "MEDIUM", "REVIEW_NEEDED", "Suspicious Medium Value")
    );
}