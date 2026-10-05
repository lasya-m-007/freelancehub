package com.freelancehub.repository;

import com.freelancehub.model.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByProjectId(Long projectId);

    Optional<Review> findByProjectId(Long projectId);

    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    long countByRevieweeId(Long revieweeId);

    @Query("select avg(r.rating) from Review r where r.reviewee.id = :id")
    Double averageRatingByRevieweeId(@Param("id") Long id);
}
