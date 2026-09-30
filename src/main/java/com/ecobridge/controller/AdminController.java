package com.ecobridge.controller;

import com.ecobridge.entity.Collector;
import com.ecobridge.entity.User;
import com.ecobridge.repository.CollectorRepository;
import com.ecobridge.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final CollectorRepository collectors;
    private final UserRepository users;

    public AdminController(CollectorRepository collectors, UserRepository users) {
        this.collectors = collectors;
        this.users = users;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @GetMapping("/collectors")
    public List<CollectorApplication> applications() {
        return collectors.findAllByOrderByCreatedAtDesc().stream().map(collector -> {
            User user = users.findById(collector.getUserId()).orElseThrow();
            return new CollectorApplication(collector.getId(), user.getName(), user.getEmail(),
                    collector.getPhone(), collector.getServiceArea(), collector.getVehicleType(),
                    collector.getVehicleNumber(), collector.getVerificationStatus(), collector.getCreatedAt());
        }).toList();
    }

    @Transactional
    @PostMapping("/collectors/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id) {
        Collector collector = pending(id);
        User user = users.findById(collector.getUserId()).orElseThrow();
        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An admin account cannot become a collector");
        }
        collector.setVerificationStatus("APPROVED");
        user.setRole("ROLE_COLLECTOR");
        collectors.save(collector);
        users.save(user);
        return Map.of("verificationStatus", "APPROVED");
    }

    @Transactional
    @PostMapping("/collectors/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id) {
        Collector collector = pending(id);
        collector.setVerificationStatus("REJECTED");
        collectors.save(collector);
        return Map.of("verificationStatus", "REJECTED");
    }

    private Collector pending(Long id) {
        Collector collector = collectors.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Collector application not found"));
        if (!"PENDING".equals(collector.getVerificationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application has already been reviewed");
        }
        return collector;
    }

    public record CollectorApplication(Long id, String applicantName, String email, String phone,
                                       String serviceArea, String vehicleType, String vehicleNumber,
                                       String status, Instant submittedAt) {}
}
