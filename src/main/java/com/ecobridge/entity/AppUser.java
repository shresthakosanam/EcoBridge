package com.ecobridge.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="users",uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name; @Column(nullable=false) private String email;
 private String passwordHash; private String provider="LOCAL"; private String firebaseUid; private String avatarUrl;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;} public String getProvider(){return provider;} public void setProvider(String v){provider=v;} public String getFirebaseUid(){return firebaseUid;} public void setFirebaseUid(String v){firebaseUid=v;} public String getAvatarUrl(){return avatarUrl;} public void setAvatarUrl(String v){avatarUrl=v;} public Instant getCreatedAt(){return createdAt;}
}
