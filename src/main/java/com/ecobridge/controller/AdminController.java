package com.ecobridge.controller;
import com.ecobridge.entity.*; import com.ecobridge.repository.*; import jakarta.transaction.Transactional; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.util.Map;
@RestController @RequestMapping("/api/admin") public class AdminController {
 private final CollectorRepository collectors; private final UserRepository users;
 public AdminController(CollectorRepository collectors,UserRepository users){this.collectors=collectors;this.users=users;}
 @Transactional @PostMapping("/collectors/{id}/approve") public Map<String,Object> approve(@PathVariable Long id){Collector c=collectors.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Collector application not found"));User u=users.findById(c.getUserId()).orElseThrow();c.setVerificationStatus("APPROVED");u.setRole("ROLE_COLLECTOR");collectors.save(c);users.save(u);return Map.of("collectorId",c.getId(),"userId",u.getId(),"role",u.getRole(),"verificationStatus",c.getVerificationStatus());}
}
