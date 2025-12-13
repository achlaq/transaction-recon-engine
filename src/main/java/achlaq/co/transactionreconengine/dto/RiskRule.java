package achlaq.co.transactionreconengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskRule {
    private BigDecimal limit;
    private String riskLevel;
    private String status;
    private String reason;
}