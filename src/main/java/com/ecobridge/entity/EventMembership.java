package com.ecobridge.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "event_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
public class EventMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private Instant joinedAt = Instant.now();
    @Column(nullable = false)
    private String status = "JOINED";

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Instant getJoinedAt() { return joinedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
