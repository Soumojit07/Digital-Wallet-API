package com.example.wallet.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitUpdateRequest {
    private BigDecimal dailyLimit;
    private BigDecimal transactionLimit;
}
