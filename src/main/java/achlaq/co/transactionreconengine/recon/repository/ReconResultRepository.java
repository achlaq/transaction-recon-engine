package achlaq.co.transactionreconengine.recon.repository;

import achlaq.co.transactionreconengine.recon.model.ReconResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {
    Optional<ReconResult> findBySourceSystemAndReferenceId(String sourceSystem, String referenceId);
    List<ReconResult> findByStatus(String status);
}
