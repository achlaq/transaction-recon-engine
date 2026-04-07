package achlaq.co.transactionreconengine.recon.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailySettlementReport {
    private LocalDate settlementDate;
    private int totalMatchedTransactions;
    private BigDecimal totalSettledAmount;
    private int totalMismatchedTransactions;
    private BigDecimal totalMismatchAmount;
}