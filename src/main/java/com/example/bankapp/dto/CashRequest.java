package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Task 12: holds the data typed into the "Deposit" or "Withdraw" form. */
@Getter
@Setter
public class CashRequest {

    @NotBlank(message = "Choose an account")
    @Size(max = 6, message = "An account number has 6 digits")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 12, fraction = 2, message = "Use at most 12 digits and 2 decimal places")
    private BigDecimal amount;
}
