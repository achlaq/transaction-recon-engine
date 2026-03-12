package achlaq.co.transactionreconengine.ledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LedgerEvent {
    @NotBlank
    private String journalId;

    private String referenceId;

    @NotBlank
    private String description;

    @NotEmpty
    private List<@Valid LedgerEventEntry> entries;
}
