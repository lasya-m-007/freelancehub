package com.freelancehub.dto;

import com.freelancehub.model.Review;
import java.time.LocalDateTime;

public record ReviewDto(Long id, int rating, String comment, Long projectId,
                        Long reviewerId, String reviewerName, Long revieweeId, String revieweeName,
                        LocalDateTime createdAt) {
    public static ReviewDto from(Review r) {
        return new ReviewDto(r.getId(), r.getRating(), r.getComment(), r.getProject().getId(),
                r.getReviewer().getId(), r.getReviewer().getName(),
                r.getReviewee().getId(), r.getReviewee().getName(), r.getCreatedAt());
    }
}
