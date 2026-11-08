package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Inserts three sample customers the first time the application starts (empty database). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        // Total money = LKR 85,000.00
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    new Customer("100001", "Kamal Silva", "kamal@example.com", new BigDecimal("50000.00")),
                    new Customer("100002", "Nimali Perera", "nimali@example.com", new BigDecimal("25000.00")),
                    new Customer("100003", "Saman Kumara", "saman@example.com", new BigDecimal("10000.00"))));
        }

        // Task 12: the bank's own account that collects the transfer fees.
        // It starts at 0.00, so the total money in the bank is still 85,000.00.
        if (customerRepository.countAccounts(BankService.FEE_ACCOUNT) == 0) {
            customerRepository.save(new Customer(BankService.FEE_ACCOUNT, "Bank Fee Account",
                    "fees@bank.example.com", new BigDecimal("0.00")));
        }
    }
}
