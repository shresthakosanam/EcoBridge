package com.ecobridge.controller;

import com.ecobridge.entity.*;
import com.ecobridge.repository.*;
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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final CollectorRepository collectors;
    private final UserRepository users;
    private final PickupRepository pickups;
    private final EventRepository events;
    private final FeedPostRepository posts;

    public AdminController(CollectorRepository collectors, UserRepository users, PickupRepository pickups,
                           EventRepository events, FeedPostRepository posts) {
        this.collectors = collectors;
        this.users = users;
        this.pickups = pickups;
        this.events = events;
        this.posts = posts;
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

    @GetMapping("/overview")
    public AdminOverview overview() {
        List<User> allUsers = users.findAll();
        Map<Long, User> userById = allUsers.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Collector> allCollectors = collectors.findAllByOrderByCreatedAtDesc();
        Map<Long, String> collectorNames = allCollectors.stream().collect(Collectors.toMap(
                Collector::getId,
                collector -> name(userById, collector.getUserId()),
                (first, ignored) -> first));
        List<PickupRequest> allPickups = pickups.findAllByOrderByCreatedAtDesc();

        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("users", (long) allUsers.size());
        metrics.put("collectors", allCollectors.stream().filter(c -> "APPROVED".equals(c.getVerificationStatus())).count());
        metrics.put("pendingCollectors", allCollectors.stream().filter(c -> "PENDING".equals(c.getVerificationStatus())).count());
        metrics.put("pickups", (long) allPickups.size());
        metrics.put("openPickups", allPickups.stream().filter(p -> !List.of("COMPLETED", "CANCELLED", "REJECTED").contains(p.getStatus())).count());
        metrics.put("events", events.count());
        metrics.put("posts", posts.count());

        List<UserSummary> userSummaries = allUsers.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                        user.getAccountStatus(), user.getCreatedAt()))
                .toList();
        List<PickupSummary> pickupSummaries = allPickups.stream()
                .map(pickup -> new PickupSummary(pickup.getId(), name(userById, pickup.getUserId()),
                        pickup.getCollectorId() == null ? null : collectorNames.getOrDefault(pickup.getCollectorId(), "Unknown collector"),
                        pickup.getWasteType(), pickup.getQuantity(), pickup.getAddress(), pickup.getPreferredDate(),
                        pickup.getPreferredTime(), pickup.getStatus(), pickup.getCreatedAt()))
                .toList();
        List<EventSummary> eventSummaries = events.findAllByOrderByDateAsc().stream()
                .map(event -> new EventSummary(event.getId(), event.getName(), name(userById, event.getOrganizerId()),
                        event.getDate(), event.getTime(), event.getLocation()))
                .toList();
        List<PostSummary> postSummaries = posts.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> new PostSummary(post.getId(), name(userById, post.getUserId()), post.getCaption(),
                        post.getActivity(), post.getCreatedAt()))
                .toList();
        return new AdminOverview(metrics, userSummaries, pickupSummaries, eventSummaries, postSummaries);
    }

    private String name(Map<Long, User> users, Long id) {
        User user = users.get(id);
        return user == null ? "Unknown account" : user.getName();
    }

    @Transactional
    @PostMapping("/collectors/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id) {
        Collector collector = pending(id);
        User user = users.findById(collector.getUserId()).orElseThrow();
        collector.setVerificationStatus("APPROVED");
        // Admins retain their administrative authority. Their approved collector
        // record grants collector access without replacing ROLE_ADMIN.
        if (!"ROLE_ADMIN".equals(user.getRole())) user.setRole("ROLE_COLLECTOR");
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
    public record AdminOverview(Map<String, Long> metrics, List<UserSummary> users,
                                List<PickupSummary> pickups, List<EventSummary> events,
                                List<PostSummary> posts) {}
    public record UserSummary(Long id, String name, String email, String role, String status, Instant joinedAt) {}
    public record PickupSummary(Long id, String requester, String collector, String wasteType, double quantity,
                                String address, LocalDate preferredDate, String preferredTime,
                                String status, Instant createdAt) {}
    public record EventSummary(Long id, String name, String organizer, LocalDate date, String time, String location) {}
    public record PostSummary(Long id, String author, String caption, String activity, Instant createdAt) {}
}
