package achlaq.co.transactionreconengine.repository.projection;

import java.math.BigDecimal;

public interface HighValueUserProjection {
    Long getUserId();
    BigDecimal getTotalSpent();
}