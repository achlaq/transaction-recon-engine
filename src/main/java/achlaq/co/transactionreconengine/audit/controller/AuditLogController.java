package achlaq.co.transactionreconengine.audit.controller;

import achlaq.co.transactionreconengine.audit.document.AuditLogDocument;
import achlaq.co.transactionreconengine.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Page<AuditLogDocument> getAllLogs(Pageable pageable) {
        return auditLogService.findAll(pageable);
    }

    @GetMapping("/user/{userId}")
    public List<AuditLogDocument> getLogsByUser(@PathVariable Long userId) {
        return auditLogService.findByUserId(userId);
    }

    @GetMapping("/risk-level/{riskLevel}")
    public List<AuditLogDocument> getLogsByRiskLevel(@PathVariable String riskLevel) {
        return auditLogService.findByRiskLevel(riskLevel);
    }

    @GetMapping("/search")
    public List<AuditLogDocument> searchSuspiciousLogs(@RequestParam Long userId) {
        return auditLogService.findSuspiciousLogs(userId);
    }
}