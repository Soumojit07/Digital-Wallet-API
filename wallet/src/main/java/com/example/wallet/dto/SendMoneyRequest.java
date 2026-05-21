package com.example.wallet.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMoneyRequest {
    private String targetIdentifier; // UPI ID, phone number, or username
    private String description;
    private BigDecimal amount;
}
