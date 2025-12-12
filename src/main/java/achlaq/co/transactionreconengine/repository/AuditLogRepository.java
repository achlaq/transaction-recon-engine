package achlaq.co.transactionreconengine.repository;

import achlaq.co.transactionreconengine.dto.AuditLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AuditLogRepository extends ElasticsearchRepository<AuditLog, String> {}
