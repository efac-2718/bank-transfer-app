package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Holds the data typed into the "Transfer Money" form. */
@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Choose the sender account")
    @Size(max = 6, message = "An account number has 6 digits")
    private String fromAccount;

    @NotBlank(message = "Choose the receiver account")
    @Size(max = 6, message = "An account number has 6 digits")
    private String toAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 12, fraction = 2, message = "Use at most 12 digits and 2 decimal places")
    private BigDecimal amount;

    // Experiment: throw an exception after the withdrawal to see the rollback
    private boolean simulateFailure;
}
