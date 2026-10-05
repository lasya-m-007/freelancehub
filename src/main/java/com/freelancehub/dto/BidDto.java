package com.freelancehub.dto;

import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidDto(Long id, BigDecimal amount, String message, BidStatus status,
                     Long projectId, String projectTitle, Long freelancerId, String freelancerName,
                     LocalDateTime createdAt) {
    public static BidDto from(Bid b) {
        return new BidDto(b.getId(), b.getAmount(), b.getMessage(), b.getStatus(),
                b.getProject().getId(), b.getProject().getTitle(),
                b.getFreelancer().getId(), b.getFreelancer().getName(), b.getCreatedAt());
    }
}
