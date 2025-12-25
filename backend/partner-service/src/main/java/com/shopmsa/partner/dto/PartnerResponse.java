package com.shopmsa.partner.dto;

import java.time.LocalDateTime;

import com.shopmsa.partner.entity.Partner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerResponse {
    private Long id;
    private String businessNumber;
    private String companyName;
    private String representativeName;
    private String email;
    private String phoneNumber;
    private String address;
    private Partner.PartnerType partnerType;
    private Partner.PartnerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static PartnerResponse from(Partner partner) {
        return PartnerResponse.builder()
                .id(partner.getId())
                .businessNumber(partner.getBusinessNumber())
                .companyName(partner.getCompanyName())
                .representativeName(partner.getRepresentativeName())
                .email(partner.getEmail())
                .phoneNumber(partner.getPhoneNumber())
                .address(partner.getAddress())
                .partnerType(partner.getPartnerType())
                .status(partner.getStatus())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}
