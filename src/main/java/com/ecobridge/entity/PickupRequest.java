package com.ecobridge.entity;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="pickups") public class PickupRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; private Long userId;
 @Column(nullable=false) private String wasteType; private double quantity; private String imageUrl;
 @Column(nullable=false) private String address; private LocalDate preferredDate; private String preferredTime;
 @Column(length=2000) private String notes; private String status="Requested"; private Instant createdAt=Instant.now();
 public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getWasteType(){return wasteType;} public void setWasteType(String v){wasteType=v;} public double getQuantity(){return quantity;} public void setQuantity(double v){quantity=v;} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;} public LocalDate getPreferredDate(){return preferredDate;} public void setPreferredDate(LocalDate v){preferredDate=v;} public String getPreferredTime(){return preferredTime;} public void setPreferredTime(String v){preferredTime=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Instant getCreatedAt(){return createdAt;}
}
