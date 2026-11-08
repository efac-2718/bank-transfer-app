package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  
public class BankService {

    public static final String FEE_ACCOUNT = "999999";

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    @Value("${bank.transfer-fee-percent:1}")
    private BigDecimal feePercent;


    public List<Customer> getCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllOrdered();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public Optional<Customer> findCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber);
    }

    public long countCustomers() {
        return customerRepository.countCustomersNative();
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total == null ? BigDecimal.ZERO : total;        // SUM is null when there are no customers
    }

    public long countTransactions(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public List<BankTransaction> getLatestTransactions(TransactionStatus status) {
        if (status == null) {
            return transactionRepository.findAllLatest();
        }
        return transactionRepository.findByStatusLatest(status);
    }

    public List<BankTransaction> getHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        return transactionRepository.getTotalSent(accountNumber, TransactionStatus.SUCCESS, TransactionType.TRANSFER);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal min) {
        return transactionRepository.findLargeTransfers(TransactionType.TRANSFER, min);
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(feePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }


    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number " + customer.getAccountNumber() + " already exists");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email " + customer.getEmail() + " already exists");
        }
        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        // 1. Validate: accounts different and both exist
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        requireAccount(fromAccount);
        requireAccount(toAccount);

        BigDecimal fee = calculateFee(amount);

        if (customerRepository.debit(fromAccount, amount.add(fee)) == 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        customerRepository.credit(toAccount, amount);
        if (fee.signum() > 0 && customerRepository.credit(FEE_ACCOUNT, fee) == 0) {
            throw new AccountNotFoundException(FEE_ACCOUNT);
        }

        return transactionRepository.save(new BankTransaction(TransactionType.TRANSFER, fromAccount, toAccount,
                amount, fee, TransactionStatus.SUCCESS, "Transfer completed"));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        return transactionRepository.save(new BankTransaction(TransactionType.DEPOSIT, null, accountNumber,
                amount, BigDecimal.ZERO, TransactionStatus.SUCCESS, "Deposit completed"));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        requireAccount(accountNumber);
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException(accountNumber);
        }
        return transactionRepository.save(new BankTransaction(TransactionType.WITHDRAWAL, accountNumber, null,
                amount, BigDecimal.ZERO, TransactionStatus.SUCCESS, "Withdrawal completed"));
    }

    private void requireAccount(String accountNumber) {
        if (customerRepository.countAccounts(accountNumber) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
    }
}
