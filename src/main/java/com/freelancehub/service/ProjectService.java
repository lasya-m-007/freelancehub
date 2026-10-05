package com.freelancehub.service;

import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ProjectService {
    Project create(Long clientId, String title, String description, BigDecimal budget,
                   LocalDate deadline, Long categoryId);
    Project update(Long projectId, Long clientId, String title, String description, BigDecimal budget,
                   LocalDate deadline, Long categoryId);
    Project getById(Long id);
    /** Returns the project only if clientId owns it, otherwise throws ForbiddenException. */
    Project getOwned(Long projectId, Long clientId);
    List<Project> search(Long categoryId, ProjectStatus status, String keyword,
                         BigDecimal minBudget, BigDecimal maxBudget);
    List<Project> findByClient(Long clientId);
    Project close(Long projectId, Long clientId);
    Project markComplete(Long projectId, Long clientId);
    void delete(Long projectId, Long actorId, Role actorRole);
    long count();
    long countByStatus(ProjectStatus status);
}
