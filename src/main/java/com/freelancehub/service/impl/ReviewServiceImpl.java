package com.freelancehub.service.impl;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.DuplicateResourceException;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import com.freelancehub.model.Project;
import com.freelancehub.model.Review;
import com.freelancehub.model.User;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.ReviewRepository;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.service.ReviewService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProjectRepository projectRepository,
                             BidRepository bidRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.projectRepository = projectRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Review createReview(Long projectId, Long clientId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new BusinessRuleException("Rating must be between 1 and 5");
        if (comment != null && comment.length() > 1000) {
            throw new BusinessRuleException("Comment must be 1000 characters or fewer");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
        if (!project.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the project owner can review it");
        }
        if (!project.isCompleted()) {
            throw new BusinessRuleException("Only completed projects can be reviewed");
        }
        if (reviewRepository.existsByProjectId(projectId)) {
            throw new DuplicateResourceException("This project has already been reviewed");
        }
        Bid accepted = bidRepository.findByProjectIdAndStatus(projectId, BidStatus.ACCEPTED).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("This project has no accepted bid"));
        User freelancer = accepted.getFreelancer();

        Review review = new Review();
        review.setProject(project);
        review.setReviewer(project.getClient());
        review.setReviewee(freelancer);
        review.setRating(rating);
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        reviewRepository.saveAndFlush(review);

        // recompute from the database (not from a stale in-memory list)
        Double avg = reviewRepository.averageRatingByRevieweeId(freelancer.getId());
        long count = reviewRepository.countByRevieweeId(freelancer.getId());
        freelancer.setAverageRating(avg == null ? 0.0 : Math.round(avg * 100.0) / 100.0);
        freelancer.setRatingCount((int) count);
        userRepository.save(freelancer);
        return review;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findByProject(Long projectId) { return reviewRepository.findByProjectId(projectId); }

    @Override
    @Transactional(readOnly = true)
    public boolean existsForProject(Long projectId) { return reviewRepository.existsByProjectId(projectId); }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByReviewee(Long userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);
    }
}
