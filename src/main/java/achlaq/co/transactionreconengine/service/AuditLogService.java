package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.document.AuditLogDocument;
import achlaq.co.transactionreconengine.repository.AuditLogRepository;
import achlaq.co.transactionreconengine.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final TransactionRepository transactionRepo;
    private final AuditLogRepository auditLogRepo;

    public Page<AuditLogDocument> findAll(Pageable pageable) {
        return auditLogRepo.findAll(pageable);
    }

    public List<AuditLogDocument> findByUserId (Long userId) {
        return auditLogRepo.findByUserId(userId);
    }

    public List<AuditLogDocument> findByRiskLevel(String riskLevel) {
        return auditLogRepo.findByRiskLevel(riskLevel);
    }

    public List<AuditLogDocument> findSuspiciousLogs(Long userId) {
        return auditLogRepo.findByUserIdAndRiskLevel(userId, "HIGH");
    }

}
