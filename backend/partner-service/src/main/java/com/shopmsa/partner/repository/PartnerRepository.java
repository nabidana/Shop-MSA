package com.shopmsa.partner.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopmsa.partner.entity.Partner;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long>{
    Optional<Partner> findByBusinessNumber(String businessNumber);
    
    Optional<Partner> findByEmail(String email);
    
    List<Partner> findByPartnerType(Partner.PartnerType partnerType);
    
    List<Partner> findByStatus(Partner.PartnerStatus status);
    
    boolean existsByBusinessNumber(String businessNumber);
    
    boolean existsByEmail(String email);
}
