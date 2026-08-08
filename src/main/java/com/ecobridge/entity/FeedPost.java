package com.ecobridge.entity;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="feed_posts") public class FeedPost {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; private Long userId;
 @Column(nullable=false) private String author; @Column(nullable=false,length=4000) private String caption;
 private String activity; private String imageUrl; private int likes; private int comments; private Instant createdAt=Instant.now();
 public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getAuthor(){return author;} public void setAuthor(String v){author=v;} public String getCaption(){return caption;} public void setCaption(String v){caption=v;} public String getActivity(){return activity;} public void setActivity(String v){activity=v;} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public int getLikes(){return likes;} public void setLikes(int v){likes=v;} public int getComments(){return comments;} public void setComments(int v){comments=v;} public Instant getCreatedAt(){return createdAt;}
}
