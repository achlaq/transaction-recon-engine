package achlaq.co.transactionreconengine.ledger.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ledger_journals")
@Data
public class JournalHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String journalId;

    private String referenceId;

    private String description;

    @Column(nullable = false)
    private LocalDateTime postedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> entries = new ArrayList<>();
}
