package achlaq.co.transactionreconengine.recon.service;

import achlaq.co.transactionreconengine.ledger.model.JournalHeader;
import achlaq.co.transactionreconengine.ledger.model.LedgerEntry;
import achlaq.co.transactionreconengine.ledger.model.EntryType;
import achlaq.co.transactionreconengine.ledger.repository.JournalRepository;
import achlaq.co.transactionreconengine.recon.dto.ExternalSnapshotRequest;
import achlaq.co.transactionreconengine.recon.model.ExternalTransactionSnapshot;
import achlaq.co.transactionreconengine.recon.model.ReconResult;
import achlaq.co.transactionreconengine.recon.model.ReconStatus;
import achlaq.co.transactionreconengine.recon.repository.ExternalTransactionSnapshotRepository;
import achlaq.co.transactionreconengine.recon.repository.ReconResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ExternalTransactionSnapshotRepository snapshotRepository;
    private final ReconResultRepository reconResultRepository;
    private final JournalRepository journalRepository;

    @Transactional
    public ReconResult ingestSnapshot(ExternalSnapshotRequest request) {
        snapshotRepository.findBySourceSystemAndReferenceId(
                request.getSourceSystem(), request.getReferenceId()
        ).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Snapshot already exists");
        });

        ExternalTransactionSnapshot snapshot = new ExternalTransactionSnapshot();
        snapshot.setSourceSystem(request.getSourceSystem());
        snapshot.setReferenceId(request.getReferenceId());
        snapshot.setAmount(request.getAmount());
        snapshot.setCurrency(request.getCurrency());
        snapshot.setEventTime(request.getEventTime());
        snapshotRepository.save(snapshot);

        return reconcileSingle(request.getSourceSystem(), request.getReferenceId());
    }

    @Transactional
    public ReconResult reconcileSingle(String sourceSystem, String referenceId) {
        ReconResult result = reconResultRepository
                .findBySourceSystemAndReferenceId(sourceSystem, referenceId)
                .orElseGet(ReconResult::new);

        result.setSourceSystem(sourceSystem);
        result.setReferenceId(referenceId);

        ExternalTransactionSnapshot snapshot = snapshotRepository
                .findBySourceSystemAndReferenceId(sourceSystem, referenceId)
                .orElse(null);

        JournalHeader journal = journalRepository
                .findByJournalId(referenceId)
                .orElse(null);

        if (snapshot == null && journal == null) {
            result.setStatus(ReconStatus.MISSING_EXTERNAL);
            result.setMatchedAt(LocalDateTime.now());
            return reconResultRepository.save(result);
        }

        if (snapshot == null) {
            result.setStatus(ReconStatus.MISSING_EXTERNAL);
            result.setLedgerAmount(totalJournalAmount(journal));
            result.setMatchedAt(LocalDateTime.now());
            return reconResultRepository.save(result);
        }

        if (journal == null) {
            result.setStatus(ReconStatus.MISSING_LEDGER);
            result.setExternalAmount(snapshot.getAmount());
            result.setMatchedAt(LocalDateTime.now());
            return reconResultRepository.save(result);
        }

        BigDecimal ledgerAmount = totalJournalAmount(journal);
        result.setLedgerAmount(ledgerAmount);
        result.setExternalAmount(snapshot.getAmount());

        if (ledgerAmount.compareTo(snapshot.getAmount()) == 0) {
            result.setStatus(ReconStatus.MATCHED);
        } else {
            result.setStatus(ReconStatus.AMOUNT_MISMATCH);
        }

        result.setMatchedAt(LocalDateTime.now());
        return reconResultRepository.save(result);
    }

    public List<ReconResult> findByStatus(ReconStatus status) {
        return reconResultRepository.findByStatus(status.name());
    }

    private BigDecimal totalJournalAmount(JournalHeader journal) {
        return journal.getEntries().stream()
                .filter(entry -> entry.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
