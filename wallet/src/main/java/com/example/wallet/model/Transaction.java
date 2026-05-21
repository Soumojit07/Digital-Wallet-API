package com.example.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id")
    private User sender; // Can be null if it's a deposit from bank to wallet

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id")
    private User receiver; // Can be null if it's a withdrawal from wallet to bank

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(nullable = false)
    private String type; // WALLET_TO_WALLET, WALLET_TO_BANK, BANK_TO_WALLET, MERCHANT_PAYMENT

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal cashbackAmount;
}
