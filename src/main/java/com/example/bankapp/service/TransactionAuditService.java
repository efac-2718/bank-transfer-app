package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Saves a FAILED record after a transfer (or deposit / withdrawal) was rolled back.
 * REQUIRES_NEW = always start a brand-new, independent transaction,
 * so this record is committed even though the transfer itself was rolled back.
 * It must be a separate class: calling a @Transactional method of the same class bypasses the Spring proxy.
 */
@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private static final int MAX_DESCRIPTION = 255;

    private final BankTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(TransactionType type, String fromAccount, String toAccount,
                              BigDecimal amount, String reason) {
        transactionRepository.save(new BankTransaction(type, fromAccount, toAccount, amount,
                BigDecimal.ZERO, TransactionStatus.FAILED, shorten(reason)));
    }

    // The description column holds at most 255 characters
    private static String shorten(String reason) {
        if (reason == null) {
            return "Unknown error";
        }
        return reason.length() <= MAX_DESCRIPTION ? reason : reason.substring(0, MAX_DESCRIPTION);
    }
}
