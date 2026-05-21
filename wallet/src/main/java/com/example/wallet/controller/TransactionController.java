package com.example.wallet.controller;
import java.util.Map;
import com.example.wallet.dto.SendMoneyRequest;
import com.example.wallet.dto.TransactionDTO;
import com.example.wallet.model.Transaction;
import com.example.wallet.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMoney(Authentication authentication, @RequestBody SendMoneyRequest request) {
        String username = authentication.getName();
        try {
            Transaction transaction = transactionService.sendMoney(username, request);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(Authentication authentication) {
        String username = authentication.getName();
        List<TransactionDTO> transactions = transactionService.getTransactionHistory(username);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/add-funds")
    public ResponseEntity<?> addFunds(Authentication authentication, @RequestParam Long bankAccountId, @RequestParam BigDecimal amount) {
        String username = authentication.getName();
        try {
            Transaction transaction = transactionService.addFunds(username, bankAccountId, amount);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/withdraw-funds")
    public ResponseEntity<?> withdrawFunds(Authentication authentication, @RequestParam Long bankAccountId, @RequestParam BigDecimal amount) {
        String username = authentication.getName();
        try {
            Transaction transaction = transactionService.withdrawFunds(username, bankAccountId, amount);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
