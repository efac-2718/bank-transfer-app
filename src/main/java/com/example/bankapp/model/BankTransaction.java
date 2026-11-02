package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row for every transfer, deposit or withdrawal, successful or failed. Stored in "bank_transactions". */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Task 12: TRANSFER, DEPOSIT or WITHDRAWAL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    // Empty for a deposit (the money comes from outside the bank)
    @Column(length = 6)
    private String fromAccount;

    // Empty for a withdrawal (the money leaves the bank)
    @Column(length = 6)
    private String toAccount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    // Task 12: transfer fee paid by the sender into account 999999 (0.00 for everything else)
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fee;

    // Stored as the text SUCCESS / FAILED instead of 0 / 1
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public BankTransaction(TransactionType type, String fromAccount, String toAccount, BigDecimal amount,
                           BigDecimal fee, TransactionStatus status, String description) {
        this.type = type;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.fee = fee;
        this.status = status;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
