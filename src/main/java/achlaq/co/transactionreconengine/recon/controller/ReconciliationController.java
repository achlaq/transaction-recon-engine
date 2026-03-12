package achlaq.co.transactionreconengine.recon.controller;

import achlaq.co.transactionreconengine.recon.dto.ExternalSnapshotRequest;
import achlaq.co.transactionreconengine.recon.model.ReconResult;
import achlaq.co.transactionreconengine.recon.model.ReconStatus;
import achlaq.co.transactionreconengine.recon.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recon")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/snapshots")
    public ResponseEntity<ReconResult> ingestSnapshot(@Valid @RequestBody ExternalSnapshotRequest request) {
        return ResponseEntity.ok(reconciliationService.ingestSnapshot(request));
    }

    @PostMapping("/run/{sourceSystem}/{referenceId}")
    public ResponseEntity<ReconResult> reconcileSingle(
            @PathVariable String sourceSystem,
            @PathVariable String referenceId
    ) {
        return ResponseEntity.ok(reconciliationService.reconcileSingle(sourceSystem, referenceId));
    }

    @GetMapping("/results")
    public ResponseEntity<List<ReconResult>> findByStatus(@RequestParam ReconStatus status) {
        return ResponseEntity.ok(reconciliationService.findByStatus(status));
    }
}
