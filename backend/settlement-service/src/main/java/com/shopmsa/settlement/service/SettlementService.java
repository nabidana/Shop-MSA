package com.shopmsa.settlement.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmsa.settlement.dto.SettlementRequest;
import com.shopmsa.settlement.dto.SettlementResponse;
import com.shopmsa.settlement.entity.Settlement;
import com.shopmsa.settlement.exception.SettlementNotFoundException;
import com.shopmsa.settlement.repository.SettlementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    @CacheEvict(value = "settlements", allEntries = true)
    public SettlementResponse createSettlement(SettlementRequest request) {
        log.info("Creating settlement for partner: {}", request.getPartnerId());
        
        // Net amount 계산
        var netAmount = request.getTotalAmount().subtract(request.getFeeAmount());
        
        Settlement settlement = Settlement.builder()
                .partnerId(request.getPartnerId())
                .settlementDate(request.getSettlementDate())
                .totalAmount(request.getTotalAmount())
                .feeAmount(request.getFeeAmount())
                .netAmount(netAmount)
                .status(Settlement.SettlementStatus.PENDING)
                .description(request.getDescription())
                .build();
        
        Settlement saved = settlementRepository.save(settlement);
        
        kafkaTemplate.send("settlement-events", "SETTLEMENT_CREATED:" + saved.getId());
        
        return SettlementResponse.from(saved);
    }
    
    @Cacheable(value = "settlements", key = "#id")
    public SettlementResponse getSettlementById(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));
        return SettlementResponse.from(settlement);
    }
    
    public List<SettlementResponse> getSettlementsByPartnerId(Long partnerId) {
        return settlementRepository.findByPartnerId(partnerId).stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<SettlementResponse> getSettlementsByDate(LocalDate date) {
        return settlementRepository.findBySettlementDate(date).stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "settlements")
    public List<SettlementResponse> getAllSettlements() {
        return settlementRepository.findAll().stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "settlements", allEntries = true)
    public SettlementResponse processSettlement(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));
        
        if (settlement.getStatus() != Settlement.SettlementStatus.PENDING) {
            throw new IllegalStateException("Only pending settlements can be processed");
        }
        
        settlement.setStatus(Settlement.SettlementStatus.PROCESSING);
        Settlement updated = settlementRepository.save(settlement);
        
        kafkaTemplate.send("settlement-events", "SETTLEMENT_PROCESSING:" + updated.getId());
        
        return SettlementResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "settlements", allEntries = true)
    public SettlementResponse completeSettlement(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));
        
        settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
        Settlement updated = settlementRepository.save(settlement);
        
        kafkaTemplate.send("settlement-events", "SETTLEMENT_COMPLETED:" + updated.getId());
        
        return SettlementResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "settlements", allEntries = true)
    public SettlementResponse failSettlement(Long id, String reason) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));
        
        settlement.setStatus(Settlement.SettlementStatus.FAILED);
        settlement.setDescription(
                (settlement.getDescription() != null ? settlement.getDescription() + " | " : "")
                + "Failed: " + reason);
        
        Settlement updated = settlementRepository.save(settlement);
        
        kafkaTemplate.send("settlement-events", "SETTLEMENT_FAILED:" + updated.getId());
        
        return SettlementResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "settlements", allEntries = true)
    public void cancelSettlement(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));
        
        settlement.setStatus(Settlement.SettlementStatus.CANCELLED);
        settlementRepository.save(settlement);
        
        kafkaTemplate.send("settlement-events", "SETTLEMENT_CANCELLED:" + id);
    }
}
