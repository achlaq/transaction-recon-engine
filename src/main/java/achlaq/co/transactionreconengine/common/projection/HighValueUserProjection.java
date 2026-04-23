package achlaq.co.transactionreconengine.common.projection;

import java.math.BigDecimal;

public interface HighValueUserProjection {
    Long getUserId();
    BigDecimal getTotalSpent();
}