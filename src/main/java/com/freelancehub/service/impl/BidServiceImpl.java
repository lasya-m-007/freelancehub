package com.freelancehub.service.impl;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.InvalidBidException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.service.BidService;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public BidServiceImpl(BidRepository bidRepository, ProjectRepository projectRepository,
                          UserRepository userRepository) {
        this.bidRepository = bidRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Bid placeBid(Long freelancerId, Long projectId, BigDecimal amount, String message) {
        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + freelancerId + " not found"));
        if (freelancer.getRole() != Role.FREELANCER) {
            throw new ForbiddenException("Only freelancers can place bids");
        }
        if (projectId == null) throw new InvalidBidException("Project is required");
        // lock the project row so a bid cannot slip in while the client is accepting another one
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new InvalidBidException("This project is not open for bids");
        }
        validateAmountAndMessage(amount, message, project);
        if (bidRepository.existsByProjectIdAndFreelancerId(projectId, freelancerId)) {
            throw new InvalidBidException("You have already placed a bid on this project");
        }
        Bid bid = new Bid();
        bid.setProject(project);
        bid.setFreelancer(freelancer);
        bid.setAmount(amount);
        bid.setMessage(message.trim());
        bid.setStatus(BidStatus.PENDING);
        return bidRepository.save(bid);
    }

    @Override
    @Transactional
    public Bid updateBid(Long bidId, Long freelancerId, BigDecimal amount, String message) {
        Bid bid = findById(bidId);
        if (!bid.getFreelancer().getId().equals(freelancerId)) {
            throw new ForbiddenException("This is not your bid");
        }
        if (!bid.isPending()) {
            throw new InvalidBidException("Only pending bids can be edited");
        }
        if (!bid.getProject().isOpen()) {
            throw new InvalidBidException("This project is no longer open");
        }
        validateAmountAndMessage(amount, message, bid.getProject());
        bid.setAmount(amount);
        bid.setMessage(message.trim());
        return bidRepository.save(bid);
    }

    @Override
    @Transactional(readOnly = true)
    public Bid findById(Long id) {
        return bidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bid " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bid> findByProject(Long projectId, Long actorId, Role actorRole) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
        if (actorRole != Role.ADMIN && !project.getClient().getId().equals(actorId)) {
            throw new ForbiddenException("Only the project owner can view its bids");
        }
        return bidRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing(Bid::getAmount))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bid> findForUser(Long userId, Role role) {
        return switch (role) {
            case ADMIN -> bidRepository.findAll();
            case CLIENT -> bidRepository.findByProjectClientIdOrderByCreatedAtDesc(userId);
            case FREELANCER -> bidRepository.findByFreelancerIdOrderByCreatedAtDesc(userId);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bid> findByFreelancer(Long freelancerId) {
        return bidRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId);
    }

    /** Accept one bid, reject every other bid, move project to IN_PROGRESS - all in ONE transaction. */
    @Override
    @Transactional
    public Bid acceptBid(Long bidId, Long clientId) {
        Bid bid = findById(bidId);
        Project project = projectRepository.findByIdForUpdate(bid.getProject().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!project.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the project owner can accept bids");
        }
        if (!project.isOpen()) {
            throw new InvalidBidException("This project is no longer open");
        }
        if (!bid.isPending()) {
            throw new InvalidBidException("Only pending bids can be accepted");
        }
        List<Bid> allBids = bidRepository.findByProjectId(project.getId());
        allBids.forEach(b -> b.setStatus(b.getId().equals(bidId) ? BidStatus.ACCEPTED : BidStatus.REJECTED));
        project.setStatus(ProjectStatus.IN_PROGRESS);
        bidRepository.saveAll(allBids);
        projectRepository.save(project);
        return bid;
    }

    @Override
    @Transactional
    public void delete(Long bidId, Long actorId, Role actorRole) {
        Bid bid = findById(bidId);
        boolean owner = bid.getFreelancer().getId().equals(actorId);
        if (!owner && actorRole != Role.ADMIN) {
            throw new ForbiddenException("You cannot delete this bid");
        }
        if (!bid.isPending()) {
            throw new BusinessRuleException("Only pending bids can be withdrawn");
        }
        bidRepository.delete(bid);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByProject(Long projectId) { return bidRepository.countByProjectId(projectId); }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countByProjects(Collection<Project> projects) {
        if (projects == null || projects.isEmpty()) return Map.of();
        List<Long> ids = projects.stream().map(Project::getId).toList();
        return bidRepository.countByProjectIds(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBid(Long projectId, Long freelancerId) {
        return bidRepository.existsByProjectIdAndFreelancerId(projectId, freelancerId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() { return bidRepository.count(); }

    private void validateAmountAndMessage(BigDecimal amount, String message, Project project) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidBidException("Bid amount must be greater than zero");
        }
        if (amount.compareTo(project.getBudget()) > 0) {
            throw new InvalidBidException("Bid amount cannot exceed the project budget");
        }
        if (message == null || message.isBlank()) {
            throw new InvalidBidException("Please include a message with your bid");
        }
        if (message.trim().length() > 2000) {
            throw new InvalidBidException("Message must be 2000 characters or fewer");
        }
    }
}
