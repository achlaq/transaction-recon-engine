package achlaq.co.transactionreconengine.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionEvent {
    private String requestId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String targetAccount;
}
