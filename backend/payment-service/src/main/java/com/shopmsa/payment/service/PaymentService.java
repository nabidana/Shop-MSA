package com.shopmsa.payment.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmsa.payment.dto.PaymentRequest;
import com.shopmsa.payment.dto.PaymentResponse;
import com.shopmsa.payment.entity.Payment;
import com.shopmsa.payment.exception.PaymentNotFoundException;
import com.shopmsa.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment for order: {}", request.getOrderId());
        
        // 트랜잭션 ID 생성
        String transactionId = UUID.randomUUID().toString();
        
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .transactionId(transactionId)
                .description(request.getDescription())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("payment-events", 
                "PAYMENT_CREATED:" + savedPayment.getId());
        
        log.info("Payment created: {} with transaction ID: {}", 
                savedPayment.getId(), transactionId);
        
        return PaymentResponse.from(savedPayment);
    }
    
    @Cacheable(value = "payments", key = "#id")
    public PaymentResponse getPaymentById(Long id) {
        log.info("Getting payment by id: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        return PaymentResponse.from(payment);
    }
    
    @Cacheable(value = "payments", key = "#transactionId")
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        log.info("Getting payment by transaction ID: {}", transactionId);
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with transaction ID: " + transactionId));
        return PaymentResponse.from(payment);
    }
    
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        log.info("Getting payments for user: {}", userId);
        return paymentRepository.findByUserId(userId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        log.info("Getting payments for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "payments")
    public List<PaymentResponse> getAllPayments() {
        log.info("Getting all payments");
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentResponse completePayment(Long id) {
        log.info("Completing payment: {}", id);
        
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment can only be completed from PENDING status");
        }
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        Payment updatedPayment = paymentRepository.save(payment);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("payment-events", 
                "PAYMENT_COMPLETED:" + updatedPayment.getId());
        
        log.info("Payment completed: {}", updatedPayment.getId());
        return PaymentResponse.from(updatedPayment);
    }
    
    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentResponse failPayment(Long id, String reason) {
        log.info("Failing payment: {} with reason: {}", id, reason);
        
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setDescription(
                (payment.getDescription() != null ? payment.getDescription() + " | " : "") 
                + "Failed: " + reason);
        
        Payment updatedPayment = paymentRepository.save(payment);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("payment-events", 
                "PAYMENT_FAILED:" + updatedPayment.getId() + ":" + reason);
        
        log.info("Payment failed: {}", updatedPayment.getId());
        return PaymentResponse.from(updatedPayment);
    }
    
    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentResponse refundPayment(Long id) {
        log.info("Refunding payment: {}", id);
        
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only completed payments can be refunded");
        }
        
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment updatedPayment = paymentRepository.save(payment);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("payment-events", 
                "PAYMENT_REFUNDED:" + updatedPayment.getId());
        
        log.info("Payment refunded: {}", updatedPayment.getId());
        return PaymentResponse.from(updatedPayment);
    }
    
    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public void cancelPayment(Long id) {
        log.info("Cancelling payment: {}", id);
        
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be cancelled");
        }
        
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("payment-events", 
                "PAYMENT_CANCELLED:" + id);
        
        log.info("Payment cancelled: {}", id);
    }
}
