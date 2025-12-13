package achlaq.co.transactionreconengine.repository;

import achlaq.co.transactionreconengine.model.TransactionEntity;
import achlaq.co.transactionreconengine.repository.projection.HighValueUserProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    @Query(value = """
        WITH UserTotals AS (
            SELECT user_id, SUM(amount) as total_spent
            FROM transactions
            WHERE status = 'SUCCESS'
            GROUP BY user_id
        ),
        GlobalStats AS (
            SELECT AVG(total_spent) as global_avg
            FROM UserTotals
        )
        SELECT u.user_id, u.total_spent
        FROM UserTotals u, GlobalStats g
        WHERE u.total_spent > (g.global_avg * 1.5)
        ORDER BY u.total_spent DESC
    """, nativeQuery = true)
    List<HighValueUserProjection> findHighValueUsersAboveAverage();

}