package com.shopmsa.user.controller;

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

import com.shopmsa.user.dto.UserRequest;
import com.shopmsa.user.dto.UserResponse;
import com.shopmsa.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    
    /**
     * 사용자 생성
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        log.info("POST /api/users - Creating user: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 사용자 ID로 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("GET /api/users/{} - Getting user by id", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자명으로 조회
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        log.info("GET /api/users/username/{} - Getting user by username", username);
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 사용자 조회
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/users - Getting all users");
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        log.info("PUT /api/users/{} - Updating user", id);
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 삭제 (Soft Delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/users/{} - Deleting user", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is healthy");
    }
}
