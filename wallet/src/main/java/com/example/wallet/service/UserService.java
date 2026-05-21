package com.example.wallet.service;

import com.example.wallet.dto.SignupRequest;
import com.example.wallet.model.User;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())) {
            throw new RuntimeException("Error: Phone number is already registered!");
        }

        String upiId = signupRequest.getUsername().toLowerCase() + "@payfast";
        if (signupRequest.getRole().equalsIgnoreCase("ROLE_MERCHANT")) {
            upiId = signupRequest.getUsername().toLowerCase() + "@merchant";
        }

        if (userRepository.existsByUpiId(upiId)) {
            throw new RuntimeException("Error: UPI ID is already registered!");
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getFullName())
                .email(signupRequest.getEmail())
                .phoneNumber(signupRequest.getPhoneNumber())
                .upiId(upiId)
                .role(signupRequest.getRole() != null ? signupRequest.getRole() : "ROLE_USER")
                .build();

        User savedUser = userRepository.save(user);

        // Provision wallet with ₹1000 signup bonus to help testing
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(new BigDecimal("1000.00"))
                .dailyLimit(new BigDecimal("50000.00"))
                .transactionLimit(new BigDecimal("10000.00"))
                .build();

        walletRepository.save(wallet);

        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Authenticate a user by username and raw password.
     * Throws RuntimeException if authentication fails.
     */
    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        return user;
    }
}
