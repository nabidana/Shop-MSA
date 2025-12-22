package com.shopmsa.payment.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmsa.payment.dto.PaymentRequest;
import com.shopmsa.payment.dto.PaymentResponse;
import com.shopmsa.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    
    /**
     * 결제 생성
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        log.info("POST /api/payments - Creating payment for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 결제 ID로 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        log.info("GET /api/payments/{} - Getting payment", id);
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 트랜잭션 ID로 조회
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @PathVariable String transactionId) {
        log.info("GET /api/payments/transaction/{} - Getting payment", transactionId);
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자별 결제 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUserId(
            @PathVariable Long userId) {
        log.info("GET /api/payments/user/{} - Getting payments", userId);
        List<PaymentResponse> response = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 주문별 결제 조회
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @PathVariable Long orderId) {
        log.info("GET /api/payments/order/{} - Getting payments", orderId);
        List<PaymentResponse> response = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 전체 결제 조회
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        log.info("GET /api/payments - Getting all payments");
        List<PaymentResponse> response = paymentService.getAllPayments();
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 완료 처리
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<PaymentResponse> completePayment(@PathVariable Long id) {
        log.info("POST /api/payments/{}/complete - Completing payment", id);
        PaymentResponse response = paymentService.completePayment(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 실패 처리
     */
    @PostMapping("/{id}/fail")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Unknown");
        log.info("POST /api/payments/{}/fail - Failing payment: {}", id, reason);
        PaymentResponse response = paymentService.failPayment(id, reason);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 환불 처리
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id) {
        log.info("POST /api/payments/{}/refund - Refunding payment", id);
        PaymentResponse response = paymentService.refundPayment(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 취소
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPayment(@PathVariable Long id) {
        log.info("DELETE /api/payments/{} - Cancelling payment", id);
        paymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is healthy");
    }
}
