package com.shopmsa.accounting.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopmsa.accounting.entity.AccountingEntry;

@Repository
public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long>{
    List<AccountingEntry> findByEntryDate(LocalDate entryDate);
    
    List<AccountingEntry> findByEntryDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<AccountingEntry> findByAccountCode(String accountCode);
    
    List<AccountingEntry> findByEntryType(AccountingEntry.EntryType entryType);
    
    List<AccountingEntry> findByStatus(AccountingEntry.EntryStatus status);
    
    List<AccountingEntry> findByReferenceNumber(String referenceNumber);
}
