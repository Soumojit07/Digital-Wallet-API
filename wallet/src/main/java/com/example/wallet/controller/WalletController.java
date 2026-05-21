package com.example.wallet.controller;

import com.example.wallet.dto.LimitUpdateRequest;
import com.example.wallet.model.Wallet;
import com.example.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<?> getWalletBalance(Authentication authentication) {
        String username = authentication.getName();
        Wallet wallet = walletService.getWalletByUsername(username);
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/limits")
    public ResponseEntity<?> updateLimits(Authentication authentication, @RequestBody LimitUpdateRequest request) {
        String username = authentication.getName();
        Wallet wallet = walletService.updateLimits(username, request);
        return ResponseEntity.ok(wallet);
    }
}
