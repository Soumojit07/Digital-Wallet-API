package com.example.wallet.controller;

import com.example.wallet.dto.BankAccountDTO;
import com.example.wallet.service.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
public class BankAccountController {

    @Autowired
    private BankAccountService bankAccountService;

    @PostMapping("/link")
    public ResponseEntity<?> linkBankAccount(Authentication authentication, @RequestBody BankAccountDTO dto) {
        String username = authentication.getName();
        try {
            BankAccountDTO account = bankAccountService.linkBankAccount(username, dto);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getLinkedAccounts(Authentication authentication) {
        String username = authentication.getName();
        List<BankAccountDTO> accounts = bankAccountService.getLinkedAccounts(username);
        return ResponseEntity.ok(accounts);
    }
}
