package achlaq.co.transactionreconengine.ledger.repository;

import achlaq.co.transactionreconengine.ledger.model.JournalHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalRepository extends JpaRepository<JournalHeader, Long> {
    Optional<JournalHeader> findByJournalId(String journalId);
}
