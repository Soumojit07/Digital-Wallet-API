package com.example.wallet.repository;

import com.example.wallet.model.Wallet;
import com.example.wallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);
    Optional<Wallet> findByUserUsername(String username);
    Optional<Wallet> findByUserPhoneNumber(String phoneNumber);
    Optional<Wallet> findByUserUpiId(String upiId);
}
