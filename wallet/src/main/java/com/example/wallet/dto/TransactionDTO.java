package com.example.wallet.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private String transactionId;
    private String senderUsername;
    private String senderFullName;
    private String senderUpiId;
    private String receiverUsername;
    private String receiverFullName;
    private String receiverUpiId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String status;
    private String type;
    private String description;
    private BigDecimal cashbackAmount;
}
