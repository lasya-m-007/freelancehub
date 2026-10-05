package com.freelancehub.service;

import com.freelancehub.model.Bid;
import com.freelancehub.model.Project;
import com.freelancehub.model.Role;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BidService {
    Bid placeBid(Long freelancerId, Long projectId, BigDecimal amount, String message);
    Bid updateBid(Long bidId, Long freelancerId, BigDecimal amount, String message);
    Bid findById(Long id);
    /** Bids on a project, cheapest first. Only the owning client or an admin may call this. */
    List<Bid> findByProject(Long projectId, Long actorId, Role actorRole);
    /** Admin: all bids, Client: bids on own projects, Freelancer: own bids. */
    List<Bid> findForUser(Long userId, Role role);
    List<Bid> findByFreelancer(Long freelancerId);
    Bid acceptBid(Long bidId, Long clientId);
    void delete(Long bidId, Long actorId, Role actorRole);
    long countByProject(Long projectId);
    Map<Long, Long> countByProjects(Collection<Project> projects);
    boolean hasBid(Long projectId, Long freelancerId);
    long count();
}
