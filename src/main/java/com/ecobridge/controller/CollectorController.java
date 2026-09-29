package com.ecobridge.controller;

import com.ecobridge.entity.*;
import com.ecobridge.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/collector")
public class CollectorController {
    private static final Map<String,String> NEXT=Map.of("ACCEPTED","ON_THE_WAY","COLLECTOR_ASSIGNED","ON_THE_WAY","ON_THE_WAY","COLLECTED","COLLECTED","COMPLETED");
    private final PickupRepository pickups; private final UserRepository users; private final CollectorRepository collectors;
    public CollectorController(PickupRepository pickups,UserRepository users,CollectorRepository collectors){this.pickups=pickups;this.users=users;this.collectors=collectors;}

    @GetMapping("/overview") public Map<String,Object> overview(HttpSession session){
        User user=requireCollectorUser(session); Collector collector=collectors.findByUserId(user.getId()).orElseThrow(); Map<String,Object> result=new LinkedHashMap<>();
        result.put("collector",Map.of("id",collector.getId(),"name",user.getName(),"email",user.getEmail(),"role",user.getRole()));
        result.put("available",pickups.findAllByCollectorIdIsNullAndStatusOrderByCreatedAtAsc("REQUESTED"));
        result.put("assigned",pickups.findAllByCollectorIdOrderByCreatedAtDesc(collector.getId())); return result;
    }
    @Transactional @PostMapping("/pickups/{id}/accept") public PickupRequest accept(@PathVariable Long id,HttpSession session){
        User user=requireCollectorUser(session); Collector collector=collectors.findByUserId(user.getId()).orElseThrow(); PickupRequest pickup=locked(id);
        if(pickup.getCollectorId()!=null||!"REQUESTED".equals(pickup.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Another collector already accepted this request");
        if(Objects.equals(pickup.getUserId(),user.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"You cannot collect your own pickup request");
        pickup.setCollectorId(collector.getId());pickup.setAcceptedAt(Instant.now());pickup.setStatus("ACCEPTED");return pickups.save(pickup);
    }
    @Transactional @PatchMapping("/pickups/{id}/status") public PickupRequest update(@PathVariable Long id,@Valid @RequestBody StatusInput input,HttpSession session){
        User user=requireCollectorUser(session); Collector collector=collectors.findByUserId(user.getId()).orElseThrow(); PickupRequest pickup=locked(id);
        if(!Objects.equals(pickup.getCollectorId(),collector.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This pickup is assigned to another collector");
        String expected=NEXT.get(pickup.getStatus()); if(expected==null||!expected.equals(input.status()))throw new ResponseStatusException(HttpStatus.CONFLICT,expected==null?"This pickup is already completed":"Next allowed status is "+expected);
        pickup.setStatus(input.status()); if("COLLECTED".equals(input.status()))pickup.setCollectedAt(Instant.now()); if("COMPLETED".equals(input.status())){pickup.setCompletedAt(Instant.now());collector.setTotalCompletedPickups(collector.getTotalCompletedPickups()+1);collectors.save(collector);} return pickups.save(pickup);
    }
    private PickupRequest locked(Long id){return pickups.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pickup request not found"));}
    private User requireCollectorUser(HttpSession session){Long id=(Long)session.getAttribute("userId");if(id==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in first");User user=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found"));if(!"ROLE_COLLECTOR".equals(user.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Approved collector access is required");return user;}
    public record StatusInput(@NotBlank String status){}
}
