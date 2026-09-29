package com.ecobridge.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="posts") public class FeedPost {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="user_id",nullable=false) private Long userId; @Column(name="content",columnDefinition="TEXT") private String caption; @Column(name="activity_type",length=80) private String activity; @Column(name="image_url",columnDefinition="TEXT") private String imageUrl; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; @Transient private String author;
 @PrePersist void create(){var n=Instant.now();createdAt=n;updatedAt=n;} @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getAuthor(){return author;} public void setAuthor(String v){author=v;} public String getCaption(){return caption;} public void setCaption(String v){caption=v;} public String getActivity(){return activity;} public void setActivity(String v){activity=v;} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public void setLikes(int v){} public Instant getCreatedAt(){return createdAt;}
}
