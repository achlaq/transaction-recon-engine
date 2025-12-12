package achlaq.co.transactionreconengine.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "trx_audit_logs")
public class AuditLog {
    @Id
    private String id;
    private String requestId;
    private String action;
    private String metadata;
    private String riskLevel;
}