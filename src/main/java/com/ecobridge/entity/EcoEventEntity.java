package com.ecobridge.entity;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="eco_events") public class EcoEventEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; private Long organizerId;
 @Column(nullable=false) private String name; @Column(length=3000) private String description; private LocalDate date;
 private String time; private String location; private int capacity; private String imageUrl; private String organizer; private int registered;
 public Long getId(){return id;} public Long getOrganizerId(){return organizerId;} public void setOrganizerId(Long v){organizerId=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public String getTime(){return time;} public void setTime(String v){time=v;} public String getLocation(){return location;} public void setLocation(String v){location=v;} public int getCapacity(){return capacity;} public void setCapacity(int v){capacity=v;} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public String getOrganizer(){return organizer;} public void setOrganizer(String v){organizer=v;} public int getRegistered(){return registered;} public void setRegistered(int v){registered=v;}
}
