package com.freelancehub.repository;

import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByProjectId(Long projectId);

    List<Bid> findByProjectIdAndStatus(Long projectId, BidStatus status);

    List<Bid> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    List<Bid> findByProjectClientIdOrderByCreatedAtDesc(Long clientId);

    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);

    long countByProjectId(Long projectId);

    @Query("select b.project.id, count(b) from Bid b where b.project.id in :ids group by b.project.id")
    List<Object[]> countByProjectIds(@Param("ids") Collection<Long> ids);
}
