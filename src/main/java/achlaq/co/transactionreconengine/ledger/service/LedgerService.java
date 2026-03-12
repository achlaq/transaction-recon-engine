package achlaq.co.transactionreconengine.ledger.service;

import achlaq.co.transactionreconengine.ledger.dto.JournalEntryRequest;
import achlaq.co.transactionreconengine.ledger.dto.JournalPostRequest;
import achlaq.co.transactionreconengine.ledger.dto.LedgerAccountRequest;
import achlaq.co.transactionreconengine.ledger.dto.LedgerEvent;
import achlaq.co.transactionreconengine.ledger.dto.LedgerEventEntry;
import achlaq.co.transactionreconengine.ledger.model.EntryType;
import achlaq.co.transactionreconengine.ledger.model.JournalHeader;
import achlaq.co.transactionreconengine.ledger.model.LedgerAccount;
import achlaq.co.transactionreconengine.ledger.model.LedgerEntry;
import achlaq.co.transactionreconengine.ledger.repository.JournalRepository;
import achlaq.co.transactionreconengine.ledger.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final JournalRepository journalRepository;

    @Transactional
    public LedgerAccount createAccount(LedgerAccountRequest request) {
        accountRepository.findByCode(request.getCode()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account code already exists");
        });

        LedgerAccount account = new LedgerAccount();
        account.setCode(request.getCode());
        account.setName(request.getName());
        account.setActive(true);
        return accountRepository.save(account);
    }

    @Transactional
    public JournalHeader postJournal(JournalPostRequest request) {
        journalRepository.findByJournalId(request.getJournalId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Journal ID already exists");
        });

        return createJournalFromRequest(request);
    }

    @Transactional
    public JournalHeader postJournalFromEvent(LedgerEvent event) {
        return journalRepository.findByJournalId(event.getJournalId())
                .orElseGet(() -> createJournalFromEvent(event));
    }

    private JournalHeader createJournalFromRequest(JournalPostRequest request) {
        if (request.getEntries().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least two entries required");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        List<LedgerEntry> entries = new ArrayList<>();

        for (JournalEntryRequest entryRequest : request.getEntries()) {
            LedgerAccount account = accountRepository.findById(entryRequest.getAccountId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Account not found: " + entryRequest.getAccountId()
                    ));

            LedgerEntry entry = new LedgerEntry();
            entry.setAccount(account);
            entry.setEntryType(entryRequest.getEntryType());
            entry.setAmount(entryRequest.getAmount());
            entry.setEntryDescription(entryRequest.getDescription());
            entries.add(entry);

            if (entryRequest.getEntryType() == EntryType.DEBIT) {
                debitTotal = debitTotal.add(entryRequest.getAmount());
            } else {
                creditTotal = creditTotal.add(entryRequest.getAmount());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Journal not balanced");
        }

        JournalHeader journal = new JournalHeader();
        journal.setJournalId(request.getJournalId());
        journal.setReferenceId(request.getReferenceId());
        journal.setDescription(request.getDescription());

        for (LedgerEntry entry : entries) {
            entry.setJournal(journal);
        }
        journal.setEntries(entries);

        return journalRepository.save(journal);
    }

    private JournalHeader createJournalFromEvent(LedgerEvent event) {
        if (event.getEntries() == null || event.getEntries().size() < 2) {
            throw new IllegalArgumentException("At least two entries required");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        List<LedgerEntry> entries = new ArrayList<>();

        for (LedgerEventEntry entryEvent : event.getEntries()) {
            LedgerAccount account = accountRepository.findByCode(entryEvent.getAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Account not found: " + entryEvent.getAccountCode()
                    ));

            LedgerEntry entry = new LedgerEntry();
            entry.setAccount(account);
            entry.setEntryType(entryEvent.getEntryType());
            entry.setAmount(entryEvent.getAmount());
            entry.setEntryDescription(entryEvent.getDescription());
            entries.add(entry);

            if (entryEvent.getEntryType() == EntryType.DEBIT) {
                debitTotal = debitTotal.add(entryEvent.getAmount());
            } else {
                creditTotal = creditTotal.add(entryEvent.getAmount());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException("Journal not balanced");
        }

        JournalHeader journal = new JournalHeader();
        journal.setJournalId(event.getJournalId());
        journal.setReferenceId(event.getReferenceId());
        journal.setDescription(event.getDescription());

        for (LedgerEntry entry : entries) {
            entry.setJournal(journal);
        }
        journal.setEntries(entries);

        return journalRepository.save(journal);
    }
}
