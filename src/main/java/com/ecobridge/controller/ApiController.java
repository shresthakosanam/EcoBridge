package com.ecobridge.controller;

import com.ecobridge.entity.*;
import com.ecobridge.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final PickupRepository pickups;
    private final EventRepository events;
    private final EventMembershipRepository memberships;
    private final FeedPostRepository posts;
    private final PostLikeRepository likes;
    private final PostCommentRepository comments;
    private final UserRepository users;
    private final CommunityRepository communities;
    private final CommunityMemberRepository communityMembers;

    public ApiController(PickupRepository pickups, EventRepository events,
                         EventMembershipRepository memberships, FeedPostRepository posts,
                         PostLikeRepository likes, PostCommentRepository comments, UserRepository users,
                         CommunityRepository communities, CommunityMemberRepository communityMembers) {
        this.pickups = pickups;
        this.events = events;
        this.memberships = memberships;
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
        this.users = users;
        this.communities = communities;
        this.communityMembers = communityMembers;
    }

    @GetMapping("/pickups")
    public List<PickupRequest> pickupList(HttpSession session) {
        return pickups.findAllByUserIdOrderByCreatedAtDesc(requireUser(session));
    }

    @PostMapping("/pickups")
    @ResponseStatus(HttpStatus.CREATED)
    public PickupRequest createPickup(@RequestHeader("Idempotency-Key") @Size(max=64) String key,@Valid @RequestBody PickupInput input, HttpSession session) {
        Long userId=requireUser(session); Optional<PickupRequest> prior=pickups.findByClientRequestIdAndUserId(key,userId); if(prior.isPresent())return prior.get();
        PickupRequest pickup = new PickupRequest();
        pickup.setClientRequestId(key); pickup.setUserId(userId);
        pickup.setWasteType(input.wasteType());
        pickup.setQuantity(input.quantity());
        pickup.setImageUrl(input.imageUrl());
        pickup.setAddress(input.address());
        pickup.setPreferredDate(input.preferredDate());
        pickup.setPreferredTime(input.preferredTime());
        pickup.setNotes(input.notes());
        pickup.setStatus("REQUESTED");
        return pickups.save(pickup);
    }

    @GetMapping("/events")
    public List<Map<String, Object>> eventList(HttpSession session) {
        Long userId = userId(session);
        return events.findAllByOrderByDateAsc().stream().map(event -> eventView(event, userId)).toList();
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createEvent(@Valid @RequestBody EventInput input, HttpSession session) {
        Long userId = requireUser(session);
        EcoEventEntity event = new EcoEventEntity();
        event.setName(input.name());
        event.setDescription(input.description());
        event.setDate(input.date());
        event.setTime(input.time());
        event.setLocation(input.location());
        event.setCapacity(input.capacity());
        event.setImageUrl(input.imageUrl());
        event.setOrganizerId(userId);
        event.setCommunityId(defaultCommunity(userId));
        event.setOrganizer(userName(session));
        event.setRegistered(0);
        return eventView(events.save(event), userId);
    }

    @Transactional
    @PostMapping("/events/{id}/join")
    public Map<String, Object> join(@PathVariable Long id, HttpSession session) {
        Long userId = requireUser(session);
        EcoEventEntity event = events.findByIdForUpdate(id).orElseThrow(() -> notFound("Event"));
        if (memberships.findByUserIdAndEventId(userId, id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already joined this event");
        }
        if (memberships.countByEventIdAndStatus(id, "REGISTERED") >= event.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This event is full");
        }
        EventMembership membership = new EventMembership();
        membership.setUserId(userId);
        membership.setEventId(id);
        memberships.save(membership);
        return eventView(event, userId);
    }

    @Transactional
    @DeleteMapping("/events/{id}/join")
    public Map<String, Object> leave(@PathVariable Long id, HttpSession session) {
        Long userId = requireUser(session);
        EcoEventEntity event = events.findById(id).orElseThrow(() -> notFound("Event"));
        EventMembership membership = memberships.findByUserIdAndEventId(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You have not joined this event"));
        memberships.delete(membership);
        memberships.flush();
        return eventView(event, userId);
    }

    @GetMapping("/posts")
    public List<Map<String, Object>> postList(HttpSession session) {
        Long userId = userId(session);
        return posts.findAllByOrderByCreatedAtDesc().stream().map(post -> postView(post, userId)).toList();
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPost(@Valid @RequestBody PostInput input, HttpSession session) {
        if ((input.caption() == null || input.caption().isBlank())
                && (input.activity() == null || input.activity().isBlank())
                && (input.imageUrl() == null || input.imageUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add text, an activity or a photo before sharing");
        }
        FeedPost post = new FeedPost();
        post.setUserId(requireUser(session));
        post.setAuthor(userName(session));
        post.setCaption(input.caption());
        post.setActivity(input.activity());
        post.setImageUrl(input.imageUrl());
        post.setLikes(0);
        return postView(posts.save(post), userId(session));
    }

    @Transactional
    @PostMapping("/posts/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Long id, HttpSession session) {
        Long userId = requireUser(session);
        posts.findById(id).orElseThrow(() -> notFound("Post"));
        Optional<PostLike> existing = likes.findByUserIdAndPostId(userId, id);
        boolean liked;
        if (existing.isPresent()) {
            likes.delete(existing.get());
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(id);
            likes.save(like);
            liked = true;
        }
        likes.flush();
        return Map.of("liked", liked, "likes", likes.countByPostId(id));
    }

    @GetMapping("/posts/{id}/comments")
    public List<Map<String, Object>> comments(@PathVariable Long id) {
        posts.findById(id).orElseThrow(() -> notFound("Post"));
        return comments.findByPostIdOrderByCreatedAtAsc(id).stream().map(this::commentView).toList();
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> comment(@PathVariable Long id, @Valid @RequestBody CommentInput input,
                                       HttpSession session) {
        posts.findById(id).orElseThrow(() -> notFound("Post"));
        PostComment comment = new PostComment();
        comment.setPostId(id);
        comment.setUserId(requireUser(session));
        comment.setAuthor(userName(session));
        comment.setBody(input.body().trim());
        return commentView(comments.save(comment));
    }

    @Transactional
    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id, HttpSession session) {
        FeedPost post = posts.findById(id).orElseThrow(() -> notFound("Post"));
        Long currentUser = requireUser(session);
        if (!Objects.equals(post.getUserId(), currentUser) && !isAdmin(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own post");
        }
        likes.deleteByPostId(id);
        comments.deleteByPostId(id);
        posts.delete(post);
    }

    @Transactional @DeleteMapping("/comments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id,HttpSession session){Long uid=requireUser(session);PostComment c=comments.findById(id).orElseThrow(()->notFound("Comment"));if(!Objects.equals(c.getUserId(),uid)&&!isAdmin(uid))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only delete your own comment");comments.delete(c);}

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpSession session) {
        Long id = requireUser(session);
        User user = users.findById(id).orElseThrow(() -> notFound("User"));
        List<PickupRequest> mine = pickups.findAllByUserIdOrderByCreatedAtDesc(id);
        List<PickupRequest> completed = mine.stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus())).toList();
        double diverted = completed.stream().mapToDouble(PickupRequest::getQuantity).sum();
        long postsShared = posts.countByUserId(id);
        List<Long> joinedIds = memberships.findByUserIdAndStatus(id, "REGISTERED").stream()
                .map(EventMembership::getEventId).toList();
        List<Map<String, Object>> upcoming = events.findAllByOrderByDateAsc().stream()
                .filter(e -> !e.getDate().isBefore(LocalDate.now()))
                .filter(e -> joinedIds.contains(e.getId()) || Objects.equals(e.getOrganizerId(), id))
                .limit(3).map(e -> eventView(e, id)).toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pickupCount", mine.size());
        stats.put("wasteDivertedKg", Math.round(diverted * 10.0) / 10.0);
        stats.put("eventsJoined", joinedIds.size());
        stats.put("postsShared", postsShared);
        stats.put("ecoPoints", completed.size() * 100 + joinedIds.size() * 50 + postsShared * 25);

        List<Map<String, Object>> activity = new ArrayList<>();
        mine.stream().limit(3).forEach(p -> activity.add(Map.of(
                "type", "pickup", "text", p.getWasteType() + " pickup " + p.getStatus().toLowerCase(),
                "at", p.getCreatedAt())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", userView(user));
        result.put("stats", stats);
        result.put("latestPickup", mine.isEmpty() ? null : mine.get(0));
        result.put("upcomingEvents", upcoming);
        result.put("recentActivity", activity);
        return result;
    }

    private Map<String, Object> eventView(EcoEventEntity event, Long userId) {
        long count = memberships.countByEventIdAndStatus(event.getId(), "REGISTERED");
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", event.getId()); view.put("name", event.getName());
        view.put("description", event.getDescription()); view.put("date", event.getDate());
        view.put("time", event.getTime()); view.put("location", event.getLocation());
        view.put("organizer", event.getOrganizer()); view.put("organizerId", event.getOrganizerId());
        view.put("capacity", event.getCapacity()); view.put("registeredCount", count);
        view.put("imageUrl", event.getImageUrl());
        view.put("joined", userId != null && memberships.findByUserIdAndEventId(userId, event.getId()).isPresent());
        view.put("createdByCurrentUser", userId != null && Objects.equals(userId, event.getOrganizerId()));
        return view;
    }

    private Long defaultCommunity(Long userId) {
        return communities.findByCreatedBy(userId).stream().findFirst().map(Community::getId).orElseGet(() -> {
            Community community = new Community(); community.setName(userNameFromId(userId) + "'s Community");
            community.setDescription("Community created for EcoBridge events"); community.setCreatedBy(userId);
            community = communities.save(community); CommunityMember member = new CommunityMember();
            member.setCommunityId(community.getId()); member.setUserId(userId); member.setRole("OWNER"); communityMembers.save(member);
            return community.getId();
        });
    }
    private String userNameFromId(Long id) { return users.findById(id).map(User::getName).orElse("EcoBridge"); }

    private Map<String, Object> postView(FeedPost post, Long userId) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", post.getId()); view.put("userId", post.getUserId());
        view.put("author", users.findById(post.getUserId()).map(User::getName).orElse("EcoBridge Member")); view.put("caption", post.getCaption());
        view.put("activity", post.getActivity()); view.put("imageUrl", post.getImageUrl());
        view.put("likes", likes.countByPostId(post.getId())); view.put("comments", comments.countByPostId(post.getId()));
        view.put("createdAt", post.getCreatedAt());
        view.put("liked", userId != null && likes.findByUserIdAndPostId(userId, post.getId()).isPresent());
        return view;
    }

    private Map<String, Object> commentView(PostComment comment) {
        return Map.of("id", comment.getId(), "postId", comment.getPostId(), "userId", comment.getUserId(),
                "author", users.findById(comment.getUserId()).map(User::getName).orElse("EcoBridge Member"), "body", comment.getBody(), "createdAt", comment.getCreatedAt());
    }

    private Map<String, Object> userView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId()); view.put("name", user.getName()); view.put("email", user.getEmail());
        view.put("avatarUrl", user.getProfileImageUrl()); return view;
    }

    private Long userId(HttpSession session) { return (Long) session.getAttribute("userId"); }
    private Long requireUser(HttpSession session) {
        Long id = userId(session);
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        return id;
    }
    private String userName(HttpSession session) {
        return Optional.ofNullable((String) session.getAttribute("userName")).orElse("EcoBridge Member");
    }
    private boolean isAdmin(Long id){return users.findById(id).map(u->"ROLE_ADMIN".equals(u.getRole())).orElse(false);}
    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    public record PickupInput(@NotBlank String wasteType, @Positive double quantity, String imageUrl,
                              @NotBlank String address, @NotNull @FutureOrPresent LocalDate preferredDate,
                              @NotBlank String preferredTime, @Size(max = 1000) String notes) {}
    public record EventInput(@NotBlank String name, @NotBlank String description, @NotNull @FutureOrPresent LocalDate date,
                             @NotBlank String time, @NotBlank String location, @Positive int capacity, String imageUrl) {}
    public record PostInput(@Size(max = 4000) String caption, @Size(max = 80) String activity, String imageUrl) {}
    public record CommentInput(@NotBlank @Size(max = 1000) String body) {}
}
