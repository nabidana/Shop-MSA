package com.shopmsa.settlement.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmsa.settlement.dto.SettlementRequest;
import com.shopmsa.settlement.dto.SettlementResponse;
import com.shopmsa.settlement.service.SettlementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settlement", description = "정산 관리 API")
public class SettlementController {
    private final SettlementService settlementService;
    
    @Operation(summary = "정산 생성")
    @PostMapping
    public ResponseEntity<SettlementResponse> createSettlement(@Valid @RequestBody SettlementRequest request) {
        SettlementResponse response = settlementService.createSettlement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "정산 조회")
    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> getSettlementById(@PathVariable Long id) {
        SettlementResponse response = settlementService.getSettlementById(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파트너별 정산 조회")
    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<List<SettlementResponse>> getSettlementsByPartnerId(@PathVariable Long partnerId) {
        List<SettlementResponse> response = settlementService.getSettlementsByPartnerId(partnerId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "날짜별 정산 조회")
    @GetMapping("/date/{date}")
    public ResponseEntity<List<SettlementResponse>> getSettlementsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SettlementResponse> response = settlementService.getSettlementsByDate(date);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "전체 정산 조회")
    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getAllSettlements() {
        List<SettlementResponse> response = settlementService.getAllSettlements();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "정산 처리")
    @PostMapping("/{id}/process")
    public ResponseEntity<SettlementResponse> processSettlement(@PathVariable Long id) {
        SettlementResponse response = settlementService.processSettlement(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "정산 완료")
    @PostMapping("/{id}/complete")
    public ResponseEntity<SettlementResponse> completeSettlement(@PathVariable Long id) {
        SettlementResponse response = settlementService.completeSettlement(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "정산 실패")
    @PostMapping("/{id}/fail")
    public ResponseEntity<SettlementResponse> failSettlement(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Unknown");
        SettlementResponse response = settlementService.failSettlement(id, reason);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "정산 취소")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelSettlement(@PathVariable Long id) {
        settlementService.cancelSettlement(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Settlement Service is healthy");
    }
}
