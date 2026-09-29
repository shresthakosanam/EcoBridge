package com.ecobridge.repository;

import com.ecobridge.entity.PickupRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PickupRepository extends JpaRepository<PickupRequest,Long>{
    List<PickupRequest> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<PickupRequest> findAllByCollectorIdOrderByCreatedAtDesc(Long collectorId);
    List<PickupRequest> findAllByCollectorIdIsNullAndStatusOrderByCreatedAtAsc(String status);
    Optional<PickupRequest> findByClientRequestIdAndUserId(String clientRequestId,Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PickupRequest p where p.id = :id")
    Optional<PickupRequest> findByIdForUpdate(@Param("id") Long id);
}
