package com.ecobridge.repository;

import com.ecobridge.entity.EventMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EventMembershipRepository extends JpaRepository<EventMembership, Long> {
    Optional<EventMembership> findByUserIdAndEventId(Long userId, Long eventId);
    long countByEventIdAndStatus(Long eventId, String status);
    List<EventMembership> findByUserIdAndStatus(Long userId, String status);
    void deleteByEventId(Long eventId);
}
