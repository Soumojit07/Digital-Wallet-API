package com.example.wallet.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferBankRequest {
    private Long bankAccountId;
    private BigDecimal amount;
}
