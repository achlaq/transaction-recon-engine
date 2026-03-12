package achlaq.co.transactionreconengine.recon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recon_results")
@Data
public class ReconResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private String sourceSystem;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReconStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal ledgerAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal externalAmount;

    private LocalDateTime matchedAt;
}
