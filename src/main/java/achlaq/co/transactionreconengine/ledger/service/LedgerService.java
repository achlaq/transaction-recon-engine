package achlaq.co.transactionreconengine.ledger.service;

import achlaq.co.transactionreconengine.ledger.dto.JournalEntryRequest;
import achlaq.co.transactionreconengine.ledger.dto.JournalPostRequest;
import achlaq.co.transactionreconengine.ledger.dto.LedgerAccountRequest;
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
}
