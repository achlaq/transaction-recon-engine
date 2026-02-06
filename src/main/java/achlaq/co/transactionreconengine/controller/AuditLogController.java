package achlaq.co.transactionreconengine.controller;

import achlaq.co.transactionreconengine.service.AuditLogService;
import achlaq.co.transactionreconengine.document.AuditLogDocument;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDocument>> getAllLogs(
            @PageableDefault(
                    size = 20,
                    page = 0,
                    sort = "timestamp",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) throws Exception {
        if (pageable.getOffset() > 10000) {
            throw new BadRequestException("Halaman terlalu jauh. Gunakan filter tanggal untuk mempersempit pencarian.");
        }

        return ResponseEntity.ok(auditLogService.findAll(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogDocument>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.findByUserId(userId));
    }

    @GetMapping("/risk-level/{riskLevel}")
    public ResponseEntity<List<AuditLogDocument>> getLogsByRiskLevel(@PathVariable String riskLevel) {
        return ResponseEntity.ok(auditLogService.findByRiskLevel(riskLevel));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AuditLogDocument>> searchSuspiciousUser(@RequestParam Long userId) {
        return ResponseEntity.ok(auditLogService.findSuspiciousLogs(userId));
    }

}
