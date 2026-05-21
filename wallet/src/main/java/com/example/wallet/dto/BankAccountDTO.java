package com.example.wallet.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountDTO {
    private Long id;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private BigDecimal balance;
}
