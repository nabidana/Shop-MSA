package com.shopmsa.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmsa.user.dto.UserRequest;
import com.shopmsa.user.dto.UserResponse;
import com.shopmsa.user.entity.User;
import com.shopmsa.user.exception.UserNotFoundException;
import com.shopmsa.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user: {}", request.getUsername());
        
        // 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // 사용자 생성
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword()) // TODO: 암호화 필요
                .phoneNumber(request.getPhoneNumber())
                .status(User.UserStatus.ACTIVE)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("user-events", 
                "USER_CREATED:" + savedUser.getId());
        
        log.info("User created successfully: {}", savedUser.getId());
        return UserResponse.from(savedUser);
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }

    @Cacheable(value = "users", key = "#username")
    public UserResponse getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return UserResponse.from(user);
    }
    
    @Cacheable(value = "users")
    public List<UserResponse> getAllUsers() {
        log.info("Getting all users");
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        
        User updatedUser = userRepository.save(user);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("user-events", 
                "USER_UPDATED:" + updatedUser.getId());
        
        log.info("User updated successfully: {}", updatedUser.getId());
        return UserResponse.from(updatedUser);
    }
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
        
        // Kafka 이벤트 발행
        kafkaTemplate.send("user-events", 
                "USER_DELETED:" + id);
        
        log.info("User deleted successfully: {}", id);
    }
}
