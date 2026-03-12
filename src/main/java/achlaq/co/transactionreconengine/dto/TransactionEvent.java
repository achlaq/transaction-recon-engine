package achlaq.co.transactionreconengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionEvent {
    @NotBlank
    private String requestId;
    @NotNull
    private Long userId;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;
    @NotBlank
    private String targetAccount;
}
