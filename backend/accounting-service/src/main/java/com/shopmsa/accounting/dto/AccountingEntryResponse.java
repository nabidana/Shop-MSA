package com.shopmsa.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.shopmsa.accounting.entity.AccountingEntry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingEntryResponse {
    private Long id;
    private LocalDate entryDate;
    private String accountCode;
    private String accountName;
    private AccountingEntry.EntryType entryType;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String description;
    private String referenceNumber;
    private AccountingEntry.EntryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static AccountingEntryResponse from(AccountingEntry entry) {
        return AccountingEntryResponse.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .accountCode(entry.getAccountCode())
                .accountName(entry.getAccountName())
                .entryType(entry.getEntryType())
                .debitAmount(entry.getDebitAmount())
                .creditAmount(entry.getCreditAmount())
                .description(entry.getDescription())
                .referenceNumber(entry.getReferenceNumber())
                .status(entry.getStatus())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
