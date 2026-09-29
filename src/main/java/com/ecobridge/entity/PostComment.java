package com.ecobridge.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="comments") public class PostComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="post_id",nullable=false) private Long postId; @Column(name="user_id",nullable=false) private Long userId; @Column(name="content",nullable=false,columnDefinition="TEXT") private String body; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; @Transient private String author;
 @PrePersist void create(){var n=Instant.now();createdAt=n;updatedAt=n;} @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public Long getPostId(){return postId;} public void setPostId(Long v){postId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getAuthor(){return author;} public void setAuthor(String v){author=v;} public String getBody(){return body;} public void setBody(String v){body=v;} public Instant getCreatedAt(){return createdAt;}
}
