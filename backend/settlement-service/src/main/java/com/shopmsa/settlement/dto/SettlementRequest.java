package com.shopmsa.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRequest {
    
    @NotNull(message = "Partner ID is required")
    private Long partnerId;
    
    @NotNull(message = "Settlement date is required")
    private LocalDate settlementDate;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", message = "Total amount must be positive")
    private BigDecimal totalAmount;
    
    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.0", message = "Fee amount must be positive")
    private BigDecimal feeAmount;
    
    private String description;
}
