package com.shopmsa.partner.dto;

import com.shopmsa.partner.entity.Partner;

import jakarta.validation.constraints.Email;
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
public class PartnerRequest {
    @NotBlank(message = "Business number is required")
    @Size(min = 10, max = 12, message = "Business number must be 10-12 characters")
    private String businessNumber;
    
    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must be less than 200 characters")
    private String companyName;
    
    @NotBlank(message = "Representative name is required")
    @Size(max = 100, message = "Representative name must be less than 100 characters")
    private String representativeName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;
    
    @Size(max = 500, message = "Address must be less than 500 characters")
    private String address;
    
    @NotNull(message = "Partner type is required")
    private Partner.PartnerType partnerType;
}
