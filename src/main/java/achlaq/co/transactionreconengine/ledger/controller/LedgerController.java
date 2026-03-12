package achlaq.co.transactionreconengine.ledger.controller;

import achlaq.co.transactionreconengine.ledger.dto.JournalPostRequest;
import achlaq.co.transactionreconengine.ledger.dto.LedgerAccountRequest;
import achlaq.co.transactionreconengine.ledger.model.JournalHeader;
import achlaq.co.transactionreconengine.ledger.model.LedgerAccount;
import achlaq.co.transactionreconengine.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/accounts")
    public ResponseEntity<LedgerAccount> createAccount(@Valid @RequestBody LedgerAccountRequest request) {
        return ResponseEntity.ok(ledgerService.createAccount(request));
    }

    @PostMapping("/journals")
    public ResponseEntity<JournalHeader> postJournal(@Valid @RequestBody JournalPostRequest request) {
        return ResponseEntity.ok(ledgerService.postJournal(request));
    }
}
