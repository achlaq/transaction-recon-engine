package achlaq.co.transactionreconengine.recon.repository;

import achlaq.co.transactionreconengine.recon.model.ExternalTransactionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalTransactionSnapshotRepository extends JpaRepository<ExternalTransactionSnapshot, Long> {
    Optional<ExternalTransactionSnapshot> findBySourceSystemAndReferenceId(String sourceSystem, String referenceId);
    List<ExternalTransactionSnapshot> findBySourceSystem(String sourceSystem);
}
