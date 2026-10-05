package com.freelancehub.service;

import com.freelancehub.model.Review;
import java.util.List;
import java.util.Optional;

public interface ReviewService {
    Review createReview(Long projectId, Long clientId, int rating, String comment);
    Optional<Review> findByProject(Long projectId);
    boolean existsForProject(Long projectId);
    List<Review> findByReviewee(Long userId);
}
