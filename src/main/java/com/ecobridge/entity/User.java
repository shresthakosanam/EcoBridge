package com.ecobridge.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "google_id", unique = true)
    private String googleId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;
    @Column(length = 50)
    private String role = "ROLE_USER";
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column
    private Instant updatedAt = Instant.now();

    @PrePersist void createTimestamps() { if (createdAt == null) createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public Long getId(){return id;} public String getGoogleId(){return googleId;} public void setGoogleId(String v){googleId=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getProfileImageUrl(){return profileImageUrl;} public void setProfileImageUrl(String v){profileImageUrl=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
