package com.shopmsa.accounting.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmsa.accounting.dto.AccountingEntryRequest;
import com.shopmsa.accounting.dto.AccountingEntryResponse;
import com.shopmsa.accounting.service.AccountingEntryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounting", description = "회계 관리 API")
public class AccountingEntryController {
    private final AccountingEntryService entryService;
    
    @Operation(summary = "회계 항목 생성")
    @PostMapping
    public ResponseEntity<AccountingEntryResponse> createEntry(
            @Valid @RequestBody AccountingEntryRequest request) {
        log.info("POST /api/accounting - Creating entry");
        AccountingEntryResponse response = entryService.createEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "회계 항목 조회")
    @GetMapping("/{id}")
    public ResponseEntity<AccountingEntryResponse> getEntryById(@PathVariable Long id) {
        log.info("GET /api/accounting/{} - Getting entry", id);
        AccountingEntryResponse response = entryService.getEntryById(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "날짜별 회계 항목 조회")
    @GetMapping("/date/{date}")
    public ResponseEntity<List<AccountingEntryResponse>> getEntriesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/accounting/date/{} - Getting entries", date);
        List<AccountingEntryResponse> response = entryService.getEntriesByDate(date);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "기간별 회계 항목 조회")
    @GetMapping("/range")
    public ResponseEntity<List<AccountingEntryResponse>> getEntriesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/accounting/range - Getting entries from {} to {}", 
                startDate, endDate);
        List<AccountingEntryResponse> response = 
                entryService.getEntriesByDateRange(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "계정코드별 조회")
    @GetMapping("/account/{accountCode}")
    public ResponseEntity<List<AccountingEntryResponse>> getEntriesByAccountCode(
            @PathVariable String accountCode) {
        log.info("GET /api/accounting/account/{} - Getting entries", accountCode);
        List<AccountingEntryResponse> response = 
                entryService.getEntriesByAccountCode(accountCode);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "전체 회계 항목 조회")
    @GetMapping
    public ResponseEntity<List<AccountingEntryResponse>> getAllEntries() {
        log.info("GET /api/accounting - Getting all entries");
        List<AccountingEntryResponse> response = entryService.getAllEntries();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "회계 항목 수정")
    @PutMapping("/{id}")
    public ResponseEntity<AccountingEntryResponse> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody AccountingEntryRequest request) {
        log.info("PUT /api/accounting/{} - Updating entry", id);
        AccountingEntryResponse response = entryService.updateEntry(id, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "회계 항목 전기")
    @PostMapping("/{id}/post")
    public ResponseEntity<AccountingEntryResponse> postEntry(@PathVariable Long id) {
        log.info("POST /api/accounting/{}/post", id);
        AccountingEntryResponse response = entryService.postEntry(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "회계 항목 승인")
    @PostMapping("/{id}/approve")
    public ResponseEntity<AccountingEntryResponse> approveEntry(@PathVariable Long id) {
        log.info("POST /api/accounting/{}/approve", id);
        AccountingEntryResponse response = entryService.approveEntry(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "회계 항목 거부")
    @PostMapping("/{id}/reject")
    public ResponseEntity<AccountingEntryResponse> rejectEntry(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Unknown");
        log.info("POST /api/accounting/{}/reject - {}", id, reason);
        AccountingEntryResponse response = entryService.rejectEntry(id, reason);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "회계 항목 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        log.info("DELETE /api/accounting/{}", id);
        entryService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Accounting Service is healthy");
    }
}
