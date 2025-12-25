package com.shopmsa.partner.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmsa.partner.dto.PartnerRequest;
import com.shopmsa.partner.dto.PartnerResponse;
import com.shopmsa.partner.entity.Partner;
import com.shopmsa.partner.service.PartnerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Partner", description = "파트너 관리 API")
public class PartnerController {
    private final PartnerService partnerService;
    
    @Operation(summary = "파트너 생성")
    @PostMapping
    public ResponseEntity<PartnerResponse> createPartner(@Valid @RequestBody PartnerRequest request) {
        log.info("POST /api/partners - Creating partner: {}", request.getCompanyName());
        PartnerResponse response = partnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "파트너 ID로 조회")
    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> getPartnerById(@PathVariable Long id) {
        log.info("GET /api/partners/{} - Getting partner", id);
        PartnerResponse response = partnerService.getPartnerById(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "사업자번호로 조회")
    @GetMapping("/business/{businessNumber}")
    public ResponseEntity<PartnerResponse> getPartnerByBusinessNumber(@PathVariable String businessNumber) {
        log.info("GET /api/partners/business/{} - Getting partner", businessNumber);
        PartnerResponse response = partnerService.getPartnerByBusinessNumber(businessNumber);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파트너 타입별 조회")
    @GetMapping("/type/{type}")
    public ResponseEntity<List<PartnerResponse>> getPartnersByType(@PathVariable Partner.PartnerType type) {
        log.info("GET /api/partners/type/{} - Getting partners", type);
        List<PartnerResponse> response = partnerService.getPartnersByType(type);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "전체 파트너 조회")
    @GetMapping
    public ResponseEntity<List<PartnerResponse>> getAllPartners() {
        log.info("GET /api/partners - Getting all partners");
        List<PartnerResponse> response = partnerService.getAllPartners();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파트너 수정")
    @PutMapping("/{id}")
    public ResponseEntity<PartnerResponse> updatePartner(
            @PathVariable Long id,
            @Valid @RequestBody PartnerRequest request) {
        log.info("PUT /api/partners/{} - Updating partner", id);
        PartnerResponse response = partnerService.updatePartner(id, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파트너 활성화")
    @PostMapping("/{id}/activate")
    public ResponseEntity<PartnerResponse> activatePartner(@PathVariable Long id) {
        log.info("POST /api/partners/{}/activate", id);
        PartnerResponse response = partnerService.activatePartner(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파트너 정지")
    @PostMapping("/{id}/suspend")
    public ResponseEntity<PartnerResponse> suspendPartner(@PathVariable Long id) {
        log.info("POST /api/partners/{}/suspend", id);
        PartnerResponse response = partnerService.suspendPartner(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파트너 해지")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> terminatePartner(@PathVariable Long id) {
        log.info("DELETE /api/partners/{} - Terminating partner", id);
        partnerService.terminatePartner(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Partner Service is healthy");
    }
}
