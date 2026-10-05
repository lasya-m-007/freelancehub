package com.freelancehub.dto;

import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectDto(Long id, String title, String description, BigDecimal budget, LocalDate deadline,
                         ProjectStatus status, Long clientId, String clientName,
                         Long categoryId, String categoryName, LocalDateTime createdAt) {
    public static ProjectDto from(Project p) {
        return new ProjectDto(p.getId(), p.getTitle(), p.getDescription(), p.getBudget(), p.getDeadline(),
                p.getStatus(), p.getClient().getId(), p.getClient().getName(),
                p.getCategory().getId(), p.getCategory().getName(), p.getCreatedAt());
    }
}
