package achlaq.co.transactionreconengine.controller;

import achlaq.co.transactionreconengine.document.AuditLogDocument;
import achlaq.co.transactionreconengine.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepo;

    @GetMapping
    public Iterable<AuditLogDocument> getAllLogs() {
        return auditLogRepo.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<AuditLogDocument> getLogsByUser(@PathVariable Long userId) {
        return auditLogRepo.findByUserId(userId);
    }

    @GetMapping("/risk/{level}")
    public List<AuditLogDocument> getLogsByRisk(@PathVariable String level) {
        return auditLogRepo.findByRiskLevel(level);
    }

    @GetMapping("/search")
    public List<AuditLogDocument> searchSuspiciousUser(@RequestParam Long userId) {
        return auditLogRepo.findByUserIdAndRiskLevel(userId, "HIGH");
    }
}
