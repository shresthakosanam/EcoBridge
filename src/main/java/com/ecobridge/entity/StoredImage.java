package com.ecobridge.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stored_images")
public class StoredImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Lob @Column(name = "image_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @PrePersist void created() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}
