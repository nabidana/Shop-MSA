package com.shopmsa.partner.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmsa.partner.dto.PartnerRequest;
import com.shopmsa.partner.dto.PartnerResponse;
import com.shopmsa.partner.entity.Partner;
import com.shopmsa.partner.exception.PartnerNotFoundException;
import com.shopmsa.partner.repository.PartnerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PartnerService {
    private final PartnerRepository partnerRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerResponse createPartner(PartnerRequest request) {
        log.info("Creating partner: {}", request.getCompanyName());
        
        if (partnerRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new IllegalArgumentException("Business number already exists");
        }
        
        if (partnerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        Partner partner = Partner.builder()
                .businessNumber(request.getBusinessNumber())
                .companyName(request.getCompanyName())
                .representativeName(request.getRepresentativeName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .partnerType(request.getPartnerType())
                .status(Partner.PartnerStatus.ACTIVE)
                .build();
        
        Partner saved = partnerRepository.save(partner);
        
        kafkaTemplate.send("partner-events", "PARTNER_CREATED:" + saved.getId());
        
        return PartnerResponse.from(saved);
    }
    
    @Cacheable(value = "partners", key = "#id")
    public PartnerResponse getPartnerById(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + id));
        return PartnerResponse.from(partner);
    }
    
    @Cacheable(value = "partners", key = "#businessNumber")
    public PartnerResponse getPartnerByBusinessNumber(String businessNumber) {
        Partner partner = partnerRepository.findByBusinessNumber(businessNumber)
                .orElseThrow(() -> new PartnerNotFoundException(
                        "Partner not found with business number: " + businessNumber));
        return PartnerResponse.from(partner);
    }
    
    public List<PartnerResponse> getPartnersByType(Partner.PartnerType type) {
        return partnerRepository.findByPartnerType(type).stream()
                .map(PartnerResponse::from)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "partners")
    public List<PartnerResponse> getAllPartners() {
        return partnerRepository.findAll().stream()
                .map(PartnerResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerResponse updatePartner(Long id, PartnerRequest request) {
        log.info("Updating partner: {}", id);
        
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + id));
        
        // 사업자번호 중복 체크 (자신 제외)
        if (!partner.getBusinessNumber().equals(request.getBusinessNumber()) &&
            partnerRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new IllegalArgumentException("Business number already exists");
        }
        
        // 이메일 중복 체크 (자신 제외)
        if (!partner.getEmail().equals(request.getEmail()) &&
            partnerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        partner.setBusinessNumber(request.getBusinessNumber());
        partner.setCompanyName(request.getCompanyName());
        partner.setRepresentativeName(request.getRepresentativeName());
        partner.setEmail(request.getEmail());
        partner.setPhoneNumber(request.getPhoneNumber());
        partner.setAddress(request.getAddress());
        partner.setPartnerType(request.getPartnerType());
        
        Partner updated = partnerRepository.save(partner);
        
        kafkaTemplate.send("partner-events", "PARTNER_UPDATED:" + updated.getId());
        
        return PartnerResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerResponse activatePartner(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + id));
        
        partner.setStatus(Partner.PartnerStatus.ACTIVE);
        Partner updated = partnerRepository.save(partner);
        
        kafkaTemplate.send("partner-events", "PARTNER_ACTIVATED:" + updated.getId());
        
        return PartnerResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerResponse suspendPartner(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + id));
        
        partner.setStatus(Partner.PartnerStatus.SUSPENDED);
        Partner updated = partnerRepository.save(partner);
        
        kafkaTemplate.send("partner-events", "PARTNER_SUSPENDED:" + updated.getId());
        
        return PartnerResponse.from(updated);
    }
    
    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public void terminatePartner(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + id));
        
        partner.setStatus(Partner.PartnerStatus.TERMINATED);
        partnerRepository.save(partner);
        
        kafkaTemplate.send("partner-events", "PARTNER_TERMINATED:" + id);
    }
}
