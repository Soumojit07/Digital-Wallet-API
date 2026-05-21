package com.example.wallet.repository;

import com.example.wallet.model.Transaction;
import com.example.wallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);
    List<Transaction> findBySenderUsernameOrReceiverUsernameOrderByTimestampDesc(String senderUsername, String receiverUsername);
    List<Transaction> findByReceiverAndTypeOrderByTimestampDesc(User receiver, String type);
}
