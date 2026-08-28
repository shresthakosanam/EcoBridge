package com.ecobridge.repository;

import com.ecobridge.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    long countByPostId(Long postId);
    Set<PostLike> findByUserId(Long userId);
    void deleteByPostId(Long postId);
}
