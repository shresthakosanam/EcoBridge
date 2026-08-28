package com.ecobridge.controller;

import com.ecobridge.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AccountController {
    private final UserRepository users;
    public AccountController(UserRepository users) { this.users = users; }
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("message", "Not signed in"));
        return users.findByEmailIgnoreCase(principal.getEmail()).<ResponseEntity<?>>map(user -> {
            Map<String,Object> view = new LinkedHashMap<>();
            view.put("id", user.getId()); view.put("name", user.getName()); view.put("email", user.getEmail());
            view.put("avatarUrl", user.getProfileImageUrl()); view.put("role", user.getRole());
            return ResponseEntity.ok(view);
        }).orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Not signed in")));
    }
}
