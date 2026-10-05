package com.freelancehub.service.impl;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import com.freelancehub.model.Category;
import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.CategoryRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.service.ProjectService;
import com.freelancehub.spec.ProjectSpecs;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BidRepository bidRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository,
                              CategoryRepository categoryRepository, BidRepository bidRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bidRepository = bidRepository;
    }

    @Override
    @Transactional
    public Project create(Long clientId, String title, String description, BigDecimal budget,
                          LocalDate deadline, Long categoryId) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + clientId + " not found"));
        if (client.getRole() != Role.CLIENT) {
            throw new ForbiddenException("Only clients can post projects");
        }
        validate(title, description, budget, deadline);
        Project project = new Project();
        project.setClient(client);
        project.setCategory(resolveCategory(categoryId));
        project.setTitle(title.trim());
        project.setDescription(description.trim());
        project.setBudget(budget);
        project.setDeadline(deadline);
        project.setStatus(ProjectStatus.OPEN);
        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public Project update(Long projectId, Long clientId, String title, String description, BigDecimal budget,
                          LocalDate deadline, Long categoryId) {
        Project project = getOwned(projectId, clientId);
        if (!project.isOpen()) {
            throw new BusinessRuleException("Only open projects can be edited");
        }
        validate(title, description, budget, deadline);
        project.setCategory(resolveCategory(categoryId));
        project.setTitle(title.trim());
        project.setDescription(description.trim());
        project.setBudget(budget);
        project.setDeadline(deadline);
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Project getOwned(Long projectId, Long clientId) {
        Project project = getById(projectId);
        if (!project.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("This project belongs to another client");
        }
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> search(Long categoryId, ProjectStatus status, String keyword,
                                BigDecimal minBudget, BigDecimal maxBudget) {
        return projectRepository.findAll(
                ProjectSpecs.filter(categoryId, status, keyword, minBudget, maxBudget),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByClient(Long clientId) {
        return projectRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    @Transactional
    public Project close(Long projectId, Long clientId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
        if (!project.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("This project belongs to another client");
        }
        if (!project.isOpen()) {
            throw new BusinessRuleException("Only open projects can be closed");
        }
        project.setStatus(ProjectStatus.CLOSED);
        List<Bid> bids = bidRepository.findByProjectId(projectId);
        bids.stream().filter(Bid::isPending).forEach(b -> b.setStatus(BidStatus.REJECTED));
        bidRepository.saveAll(bids);
        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public Project markComplete(Long projectId, Long clientId) {
        Project project = getOwned(projectId, clientId);
        if (!project.isInProgress()) {
            throw new BusinessRuleException("Only projects in progress can be marked complete");
        }
        project.setStatus(ProjectStatus.COMPLETED);
        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long actorId, Role actorRole) {
        Project project = getById(projectId);
        boolean owner = project.getClient().getId().equals(actorId);
        if (!owner && actorRole != Role.ADMIN) {
            throw new ForbiddenException("You cannot delete this project");
        }
        if (!(project.isOpen() || project.isClosed())) {
            throw new BusinessRuleException("Only open or closed projects can be deleted");
        }
        projectRepository.delete(project); // bids are removed through cascade
    }

    @Override
    @Transactional(readOnly = true)
    public long count() { return projectRepository.count(); }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ProjectStatus status) { return projectRepository.countByStatus(status); }

    // ---- helpers ----
    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) throw new BusinessRuleException("Please choose a category");
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
    }

    private void validate(String title, String description, BigDecimal budget, LocalDate deadline) {
        if (title == null || title.isBlank()) throw new BusinessRuleException("Title is required");
        if (title.trim().length() > 200) throw new BusinessRuleException("Title must be 200 characters or fewer");
        if (description == null || description.isBlank()) throw new BusinessRuleException("Description is required");
        if (description.trim().length() > 4000) {
            throw new BusinessRuleException("Description must be 4000 characters or fewer");
        }
        if (budget == null || budget.signum() <= 0) throw new BusinessRuleException("Budget must be greater than zero");
        if (budget.compareTo(new BigDecimal("9999999999")) > 0) throw new BusinessRuleException("Budget is too large");
        if (deadline == null) throw new BusinessRuleException("Deadline is required");
        if (deadline.isBefore(LocalDate.now())) throw new BusinessRuleException("Deadline cannot be in the past");
    }
}
