package com.shopmsa.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.shopmsa.accounting.entity.AccountingEntry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingEntryRequest {
    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;
    
    @NotBlank(message = "Account code is required")
    @Size(max = 100, message = "Account code must be less than 100 characters")
    private String accountCode;
    
    @NotBlank(message = "Account name is required")
    @Size(max = 200, message = "Account name must be less than 200 characters")
    private String accountName;
    
    @NotNull(message = "Entry type is required")
    private AccountingEntry.EntryType entryType;
    
    @NotNull(message = "Debit amount is required")
    @DecimalMin(value = "0.0", message = "Debit amount must be non-negative")
    private BigDecimal debitAmount;
    
    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.0", message = "Credit amount must be non-negative")
    private BigDecimal creditAmount;
    
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
    
    @Size(max = 100, message = "Reference number must be less than 100 characters")
    private String referenceNumber;
}
