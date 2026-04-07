package achlaq.co.transactionreconengine.recon.service;

import achlaq.co.transactionreconengine.recon.model.ReconResult;
import achlaq.co.transactionreconengine.recon.repository.ReconResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class EodSettlementJobService {

    private final ReconResultRepository reconResultRepository;

    /**
     * Executes automatically every day at 23:59.
     * Can also be triggered manually for testing purposes.
     */
    @Scheduled(cron = "0 59 23 * * ?")
    @Transactional(readOnly = true)
    public void generateEodSettlementReport() {
        LocalDate today = LocalDate.now();
        log.info("Starting EOD Settlement Job for date: {}", today);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<ReconResult> matchedResults = reconResultRepository
                .findByMatchedAtBetweenAndStatus(startOfDay, endOfDay, "MATCHED");
                
        List<ReconResult> mismatchedResults = reconResultRepository
                .findByMatchedAtBetweenAndStatus(startOfDay, endOfDay, "AMOUNT_MISMATCH");

        int totalMatched = matchedResults.size();
        BigDecimal totalSettledAmount = matchedResults.stream()
                .map(ReconResult::getLedgerAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalMismatched = mismatchedResults.size();
        BigDecimal totalMismatchAmount = mismatchedResults.stream()
                .map(result -> result.getLedgerAmount() != null ? result.getLedgerAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("EOD Settlement Summary for {}: Matched={}, SettledAmount={}, Mismatched={}, MismatchAmount={}",
                today, totalMatched, totalSettledAmount, totalMismatched, totalMismatchAmount);

        generateCsvReport(today, matchedResults, totalMatched, totalSettledAmount, totalMismatched, totalMismatchAmount);
    }

    private void generateCsvReport(LocalDate date, List<ReconResult> matchedResults, 
                                   int totalMatched, BigDecimal totalSettledAmount,
                                   int totalMismatched, BigDecimal totalMismatchAmount) {
                                       
        String filename = "eod_settlement_report_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write Summary Header
            writer.println("--- EOD SETTLEMENT SUMMARY ---");
            writer.println("Date," + date);
            writer.println("Total Matched," + totalMatched);
            writer.println("Total Settled Amount," + totalSettledAmount);
            writer.println("Total Mismatched," + totalMismatched);
            writer.println("Total Mismatch Amount," + totalMismatchAmount);
            writer.println("");
            
            // Write Detailed Transactions
            writer.println("--- MATCHED TRANSACTIONS ---");
            writer.println("ID,SourceSystem,ReferenceId,Amount,MatchedTime");
            
            for (ReconResult result : matchedResults) {
                writer.printf("%d,%s,%s,%s,%s%n",
                        result.getId(),
                        result.getSourceSystem(),
                        result.getReferenceId(),
                        result.getLedgerAmount(),
                        result.getMatchedAt()
                );
            }
            
            log.info("Successfully generated EOD Settlement Report CSV: {}", filename);
            
        } catch (IOException e) {
            log.error("Failed to write EOD Settlement Report to CSV", e);
        }
    }
}