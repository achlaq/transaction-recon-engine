package achlaq.co.transactionreconengine.ledger.repository;

import achlaq.co.transactionreconengine.ledger.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByCode(String code);
}
