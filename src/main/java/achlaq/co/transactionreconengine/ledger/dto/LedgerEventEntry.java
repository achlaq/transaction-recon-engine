package achlaq.co.transactionreconengine.ledger.dto;

import achlaq.co.transactionreconengine.ledger.model.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LedgerEventEntry {
    @NotBlank
    private String accountCode;

    @NotNull
    private EntryType entryType;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String description;
}
