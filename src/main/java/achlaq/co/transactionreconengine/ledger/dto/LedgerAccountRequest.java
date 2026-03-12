package achlaq.co.transactionreconengine.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LedgerAccountRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String name;
}
