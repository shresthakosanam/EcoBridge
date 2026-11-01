package com.ecobridge.controller;

import com.ecobridge.repository.UserRepository;
import com.ecobridge.repository.CollectorRepository;
import com.ecobridge.entity.User;
import com.ecobridge.entity.Collector;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AccountController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final CollectorRepository collectors;
    public AccountController(UserRepository users, PasswordEncoder passwords, CollectorRepository collectors) {
        this.users = users;
        this.passwords = passwords;
        this.collectors = collectors;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody Signup input, HttpSession session) {
        String email = input.email().trim().toLowerCase();
        User existing = users.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            String message = existing.getPasswordHash() == null
                    ? "This email belongs to an older Google-only account. Please use a different email to create a password account."
                    : "An account already exists for this email. Please log in instead.";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
        }
        User user = new User();
        user.setName(input.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(input.password()));
        user = users.save(user);
        signIn(user, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(view(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login input, HttpSession session) {
        User user = users.findByEmailIgnoreCase(input.email().trim()).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwords.matches(input.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password."));
        }
        signIn(user, session);
        return ResponseEntity.ok(view(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) return users.findById(userId).<ResponseEntity<?>>map(user -> ResponseEntity.ok(view(user)))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Not signed in")));
        return ResponseEntity.status(401).body(Map.of("message", "Not signed in"));
    }

    @PostMapping("/collector/enroll")
    public ResponseEntity<?> enrollCollector(@Valid @RequestBody CollectorEnrollment input, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Please sign in first"));
        User user = users.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(404).body(Map.of("message", "Account not found"));
        Collector collector = collectors.findByUserId(userId).orElse(null);
        if (collector != null && !"REJECTED".equals(collector.getVerificationStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "A collector application already exists."));
        }
        if (collector == null) { collector = new Collector(); collector.setUserId(userId); }
        collector.setVerificationStatus("PENDING"); collector.setPhone(input.phone().trim());
        collector.setServiceArea(input.serviceArea().trim()); collector.setVehicleType(input.vehicleType()); collector.setVehicleNumber(input.vehicleNumber());
        collectors.save(collector);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("verificationStatus", "PENDING", "message", "Collector application submitted for approval."));
    }

    @GetMapping("/collector/application")
    public ResponseEntity<?> collectorApplication(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Please sign in first"));
        return ResponseEntity.ok(collectors.findByUserId(userId)
                .<Map<String, Object>>map(collector -> Map.of("status", collector.getVerificationStatus()))
                .orElseGet(() -> Map.of("status", "NOT_APPLIED")));
    }

    private void signIn(User user, HttpSession session) {
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null,
                java.util.List.of(new SimpleGrantedAuthority(user.getRole())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private Map<String,Object> view(User user) {
        Map<String,Object> view = new LinkedHashMap<>();
        view.put("id", user.getId()); view.put("name", user.getName()); view.put("email", user.getEmail());
        view.put("avatarUrl", user.getProfileImageUrl()); view.put("role", user.getRole());
        return view;
    }

    public record Signup(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=255) String email,
                         @NotBlank @Size(min=6,max=72) String password) {}
    public record Login(@NotBlank @Email String email, @NotBlank String password) {}
    public record CollectorEnrollment(@NotBlank @Size(max=20) String phone, @NotBlank @Size(max=255) String serviceArea,
                                      @Size(max=100) String vehicleType, @Size(max=50) String vehicleNumber) {}
}
