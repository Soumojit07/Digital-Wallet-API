package com.example.wallet.service;

import com.example.wallet.dto.LimitUpdateRequest;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    public Wallet getWalletByUsername(String username) {
        return walletRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + username));
    }

    public Wallet updateLimits(String username, LimitUpdateRequest request) {
        Wallet wallet = getWalletByUsername(username);
        
        if (request.getDailyLimit() != null) {
            wallet.setDailyLimit(request.getDailyLimit());
        }
        if (request.getTransactionLimit() != null) {
            wallet.setTransactionLimit(request.getTransactionLimit());
        }
        
        return walletRepository.save(wallet);
    }
}
