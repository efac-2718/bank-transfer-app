package com.example.bankapp.controller;

import com.example.bankapp.dto.CashRequest;
import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @InitBinder("customer")
    public void allowCustomerFields(WebDataBinder binder) {
        binder.setAllowedFields("accountNumber", "fullName", "email", "balance");
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) TransactionStatus status,
                            Model model) {
        loadDashboard(model, keyword, status);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer, BindingResult result,
                              Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDashboard(model);
        }
        try {
            bankService.createCustomer(customer);
        } catch (BankException e) {
            // e.g. "Account number 100004 already exists": show the form again with what was typed
            model.addAttribute("error", e.getMessage());
            return showDashboard(model);
        }
        redirect.addFlashAttribute("message",
                "Customer " + customer.getFullName() + " (" + customer.getAccountNumber() + ") added");
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request, BindingResult result,
                           Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDashboard(model);        // bean validation failed: nothing is saved
        }
        try {
            BankTransaction tx = bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            String message = "Transferred LKR " + lkr(tx.getAmount())
                    + " from " + tx.getFromAccount() + " to " + tx.getToAccount();
            if (tx.getFee().signum() > 0) {
                message += " (fee LKR " + lkr(tx.getFee()) + " paid to " + BankService.FEE_ACCOUNT + ")";
            }
            redirect.addFlashAttribute("message", message);
        } catch (RuntimeException e) {
            auditService.recordFailure(TransactionType.TRANSFER, request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), e.getMessage());
            redirect.addFlashAttribute("error",
                    "Transfer failed: " + e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute("depositRequest") CashRequest request, BindingResult result,
                          Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDashboard(model);
        }
        try {
            bankService.deposit(request.getAccountNumber(), request.getAmount());
            redirect.addFlashAttribute("message",
                    "Deposited LKR " + lkr(request.getAmount()) + " to " + request.getAccountNumber());
        } catch (RuntimeException e) {
            auditService.recordFailure(TransactionType.DEPOSIT, null, request.getAccountNumber(),
                    request.getAmount(), e.getMessage());
            redirect.addFlashAttribute("error",
                    "Deposit failed: " + e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(@Valid @ModelAttribute("withdrawRequest") CashRequest request, BindingResult result,
                           Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDashboard(model);
        }
        try {
            bankService.withdraw(request.getAccountNumber(), request.getAmount());
            redirect.addFlashAttribute("message",
                    "Withdrew LKR " + lkr(request.getAmount()) + " from " + request.getAccountNumber());
        } catch (RuntimeException e) {
            auditService.recordFailure(TransactionType.WITHDRAWAL, request.getAccountNumber(), null,
                    request.getAmount(), e.getMessage());
            redirect.addFlashAttribute("error",
                    "Withdrawal failed: " + e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model, RedirectAttributes redirect) {
        Optional<Customer> customer = bankService.findCustomer(accountNumber);
        if (customer.isEmpty()) {
            redirect.addFlashAttribute("error", "Account " + accountNumber + " was not found");
            return "redirect:/";
        }
        model.addAttribute("customer", customer.get());
        model.addAttribute("transactions", bankService.getHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    private String showDashboard(Model model) {
        loadDashboard(model, null, null);
        return "bank";
    }

    private void loadDashboard(Model model, String keyword, TransactionStatus status) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("statuses", TransactionStatus.values());
        model.addAttribute("customers", bankService.getCustomers(keyword));
        model.addAttribute("allCustomers", bankService.getCustomers(null));
        model.addAttribute("customerCount", bankService.countCustomers());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.countTransactions(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countTransactions(TransactionStatus.FAILED));
        model.addAttribute("transactions", bankService.getLatestTransactions(status));
        model.addAttribute("feePercent", bankService.getFeePercent());
        model.addAttribute("feeAccount", BankService.FEE_ACCOUNT);

        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        if (!model.containsAttribute("depositRequest")) {
            model.addAttribute("depositRequest", new CashRequest());
        }
        if (!model.containsAttribute("withdrawRequest")) {
            model.addAttribute("withdrawRequest", new CashRequest());
        }
    }

    private static String lkr(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
