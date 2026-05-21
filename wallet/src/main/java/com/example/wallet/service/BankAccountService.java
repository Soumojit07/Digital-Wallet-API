package com.example.wallet.service;

import com.example.wallet.dto.BankAccountDTO;
import com.example.wallet.model.BankAccount;
import com.example.wallet.model.User;
import com.example.wallet.repository.BankAccountRepository;
import com.example.wallet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankAccountService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    public BankAccountDTO linkBankAccount(String username, BankAccountDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bankAccountRepository.existsByAccountNumber(dto.getAccountNumber())) {
            throw new RuntimeException("Bank account number already linked!");
        }

        BankAccount bankAccount = BankAccount.builder()
                .user(user)
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber())
                .ifscCode(dto.getIfscCode())
                .balance(new BigDecimal("100000.00")) // Provision simulated external bank balance
                .build();

        BankAccount saved = bankAccountRepository.save(bankAccount);
        return convertToDTO(saved);
    }

    public List<BankAccountDTO> getLinkedAccounts(String username) {
        return bankAccountRepository.findByUserUsername(username).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BankAccountDTO convertToDTO(BankAccount account) {
        return BankAccountDTO.builder()
                .id(account.getId())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .ifscCode(account.getIfscCode())
                .balance(account.getBalance())
                .build();
    }
}
