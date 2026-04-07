package achlaq.co.transactionreconengine.recon.repository;

import achlaq.co.transactionreconengine.recon.model.ReconResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {
    Optional<ReconResult> findBySourceSystemAndReferenceId(String sourceSystem, String referenceId);
    
    List<ReconResult> findByStatus(String status);
    
    @Query("SELECT r FROM ReconResult r WHERE r.matchedAt >= :startOfDay AND r.matchedAt < :endOfDay AND r.status = :status")
    List<ReconResult> findByMatchedAtBetweenAndStatus(LocalDateTime startOfDay, LocalDateTime endOfDay, String status);
}