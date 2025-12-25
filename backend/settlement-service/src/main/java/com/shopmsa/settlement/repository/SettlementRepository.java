package com.shopmsa.settlement.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopmsa.settlement.entity.Settlement;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long>{
    List<Settlement> findByPartnerId(Long partnerId);
    
    List<Settlement> findBySettlementDate(LocalDate settlementDate);
    
    List<Settlement> findByStatus(Settlement.SettlementStatus status);
    
    List<Settlement> findByPartnerIdAndSettlementDateBetween(Long partnerId, LocalDate startDate, LocalDate endDate);
}
