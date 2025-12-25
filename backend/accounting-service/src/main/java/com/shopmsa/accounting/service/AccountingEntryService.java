package com.shopmsa.accounting.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmsa.accounting.dto.AccountingEntryRequest;
import com.shopmsa.accounting.dto.AccountingEntryResponse;
import com.shopmsa.accounting.entity.AccountingEntry;
import com.shopmsa.accounting.exception.AccountingEntryNotFoundException;
import com.shopmsa.accounting.repository.AccountingEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountingEntryService {
    private final AccountingEntryRepository entryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    @CacheEvict(value = "accounting-entries", allEntries = true)
    public AccountingEntryResponse createEntry(AccountingEntryRequest request) {
        log.info("Creating accounting entry: {}", request.getAccountCode());
        
        // 차변과 대변의 합이 같은지 검증
        if (request.getDebitAmount().compareTo(request.getCreditAmount()) != 0) {
            log.warn("Debit and credit amounts do not match: {} vs {}", 
                    request.getDebitAmount(), request.getCreditAmount());
        }
        
        AccountingEntry entry = AccountingEntry.builder()
                .entryDate(request.getEntryDate())
                .accountCode(request.getAccountCode())
                .accountName(request.getAccountName())
                .entryType(request.getEntryType())
                .debitAmount(request.getDebitAmount())
                .creditAmount(request.getCreditAmount())
                .description(request.getDescription())
                .referenceNumber(request.getReferenceNumber())
                .status(AccountingEntry.EntryStatus.DRAFT)
                .build();
        
        AccountingEntry saved = entryRepository.save(entry);
        
        kafkaTemplate.send("accounting-events", "ENTRY_CREATED:" + saved.getId());
        
        return AccountingEntryResponse.from(saved);
    }
    
    @Cacheable(value = "accounting-entries", key = "#id")
    public AccountingEntryResponse getEntryById(Long id) {
        AccountingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new AccountingEntryNotFoundException(
                        "Accounting entry not found: " + id));
        return AccountingEntryResponse.from(entry);
    }
    
    public List<AccountingEntryResponse> getEntriesByDate(LocalDate date) {
        return entryRepository.findByEntryDate(date).stream()
                .map(AccountingEntryResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<AccountingEntryResponse> getEntriesByDateRange(
            LocalDate startDate, LocalDate endDate) {
        return entryRepository.findByEntryDateBetween(startDate, endDate).stream()
                .map(AccountingEntryResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<AccountingEntryResponse> getEntriesByAccountCode(String accountCode) {
        return entryRepository.findByAccountCode(accountCode).stream()
                .map(AccountingEntryResponse::from)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "accounting-entries")
    public List<AccountingEntryResponse> getAllEntries() {
        return entryRepository.findAll().stream()
                .map(AccountingEntryResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "accounting-entries", allEntries = true)
    public AccountingEntryResponse updateEntry(Long id, AccountingEntryRequest request) {
        log.info("Updating accounting entry: {}", id);
        
        AccountingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new AccountingEntryNotFoundException(
                        "Accounting entry not found: " + id));
        
        if (entry.getStatus() != AccountingEntry.EntryStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft entries can be updated");
        }
        
        entry.setEntryDate(request.getEntryDate());
        entry.setAccountCode(request.getAccountCode());
        entry.setAccountName(request.getAccountName());
        entry.setEntryType(request.getEntryType());
        entry.setDebitAmount(request.getDebitAmount());
        entry.setCreditAmount(request.getCreditAmount());
        entry.setDescription(request.getDescription());
        entry.setReferenceNumber(request.getReferenceNumber());
        
        AccountingEntry updated = entryRepository.save(entry);
        
        kafkaTemplate.send("accounting-events", "ENTRY_UPDATED:" + updated.getId());
        
        return AccountingEntryResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "accounting-entries", allEntries = true)
    public AccountingEntryResponse postEntry(Long id) {
        AccountingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new AccountingEntryNotFoundException(
                        "Accounting entry not found: " + id));
        
        if (entry.getStatus() != AccountingEntry.EntryStatus.DRAFT) {
            throw new IllegalStateException("Only draft entries can be posted");
        }
        
        entry.setStatus(AccountingEntry.EntryStatus.POSTED);
        AccountingEntry updated = entryRepository.save(entry);
        
        kafkaTemplate.send("accounting-events", "ENTRY_POSTED:" + updated.getId());
        
        return AccountingEntryResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "accounting-entries", allEntries = true)
    public AccountingEntryResponse approveEntry(Long id) {
        AccountingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new AccountingEntryNotFoundException(
                        "Accounting entry not found: " + id));
        
        if (entry.getStatus() != AccountingEntry.EntryStatus.REVIEWED) {
            throw new IllegalStateException("Only reviewed entries can be approved");
        }
        
        entry.setStatus(AccountingEntry.EntryStatus.APPROVED);
        AccountingEntry updated = entryRepository.save(entry);
        
        kafkaTemplate.send("accounting-events", "ENTRY_APPROVED:" + updated.getId());
        
        return AccountingEntryResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "accounting-entries", allEntries = true)
    public AccountingEntryResponse rejectEntry(Long id, String reason) {
        AccountingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new AccountingEntryNotFoundException(
                        "Accounting entry not found: " + id));
        
        entry.setStatus(AccountingEntry.EntryStatus.REJECTED);
        entry.setDescription(
                (entry.getDescription() != null ? entry.getDescription() + " | " : "")
                + "Rejected: " + reason);
        
        AccountingEntry updated = entryRepository.save(entry);
        
        kafkaTemplate.send("accounting-events", "ENTRY_REJECTED:" + updated.getId());
        
        return AccountingEntryResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "accounting-entries", allEntries = true)
    public void deleteEntry(Long id) {
        AccountingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new AccountingEntryNotFoundException(
                        "Accounting entry not found: " + id));
        
        if (entry.getStatus() != AccountingEntry.EntryStatus.DRAFT) {
            throw new IllegalStateException("Only draft entries can be deleted");
        }
        
        entryRepository.delete(entry);
        
        kafkaTemplate.send("accounting-events", "ENTRY_DELETED:" + id);
    }
}
