package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 1. All transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findAllLatest();

    // 2. OR condition: the account is the sender or the receiver
    @Query("SELECT t FROM BankTransaction t " +
           "WHERE t.fromAccount = :account OR t.toAccount = :account " +
           "ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findHistory(@Param("account") String account);

    // 3. Filter by status: an enum can be passed as a parameter
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    // 4. Number of SUCCESS or FAILED transactions for the dashboard
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    // Task 12: transfers of at least :min, largest first
    @Query("SELECT t FROM BankTransaction t " +
           "WHERE t.type = :type AND t.amount >= :min " +
           "ORDER BY t.amount DESC, t.createdAt DESC")
    List<BankTransaction> findLargeTransfers(@Param("type") TransactionType type, @Param("min") BigDecimal min);

    // Task 12: total amount sent by an account = SUM of its SUCCESS transfers (0 when there are none)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t " +
           "WHERE t.fromAccount = :account AND t.status = :status AND t.type = :type")
    BigDecimal getTotalSent(@Param("account") String account,
                            @Param("status") TransactionStatus status,
                            @Param("type") TransactionType type);
}
