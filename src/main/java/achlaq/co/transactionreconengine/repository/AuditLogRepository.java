package achlaq.co.transactionreconengine.repository;

import achlaq.co.transactionreconengine.document.AuditLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends ElasticsearchRepository<AuditLogDocument, String> {
    List<AuditLogDocument> findByUserId(Long userId);
    List<AuditLogDocument> findByRiskLevel(String riskLevel);
    List<AuditLogDocument> findByUserIdAndRiskLevel(Long userId, String riskLevel);
}
