package com.ecobridge.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="event_registrations",uniqueConstraints=@UniqueConstraint(columnNames={"event_id","user_id"})) public class EventMembership {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="user_id",nullable=false) private Long userId; @Column(name="event_id",nullable=false) private Long eventId; @Column(name="registered_at",nullable=false) private Instant joinedAt=Instant.now(); @Column(nullable=false,length=30) private String status="REGISTERED"; @Column(name="cancelled_at") private Instant cancelledAt;
 public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public Long getEventId(){return eventId;} public void setEventId(Long v){eventId=v;} public Instant getJoinedAt(){return joinedAt;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
