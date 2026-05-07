package achlaq.co.transactionreconengine.risk.config;

import achlaq.co.transactionreconengine.risk.dto.RiskRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.risk")
public class RiskRulesConfig {
    
    /**
     * Risk rules definitions.
     * These rules are configured in application.yml under the prefix 'app.risk.rules'.
     * If not provided in the properties file, it will default to an empty list.
     */
    private List<RiskRule> rules = new ArrayList<>();
}