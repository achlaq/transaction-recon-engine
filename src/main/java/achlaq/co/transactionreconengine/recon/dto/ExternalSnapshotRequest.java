package achlaq.co.transactionreconengine.recon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExternalSnapshotRequest {
    @NotBlank
    private String sourceSystem;

    @NotBlank
    private String referenceId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private LocalDateTime eventTime;
}
