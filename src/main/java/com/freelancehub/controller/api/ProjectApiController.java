package com.freelancehub.controller.api;

import com.freelancehub.dto.BidDto;
import com.freelancehub.dto.ProjectDto;
import com.freelancehub.dto.ProjectRequest;
import com.freelancehub.dto.ReviewDto;
import com.freelancehub.dto.ReviewRequest;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import com.freelancehub.service.ProjectService;
import com.freelancehub.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {

    private final ProjectService projectService;
    private final BidService bidService;
    private final ReviewService reviewService;
    private final AuthHelper auth;

    public ProjectApiController(ProjectService projectService, BidService bidService,
                                ReviewService reviewService, AuthHelper auth) {
        this.projectService = projectService;
        this.bidService = bidService;
        this.reviewService = reviewService;
        this.auth = auth;
    }

    /** e.g. GET /api/projects?category=1&status=OPEN&keyword=web&minBudget=1000&maxBudget=50000 */
    @GetMapping
    public List<ProjectDto> list(@RequestParam(required = false) Long category,
                                 @RequestParam(required = false) ProjectStatus status,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) BigDecimal minBudget,
                                 @RequestParam(required = false) BigDecimal maxBudget) {
        return projectService.search(category, status, keyword, minBudget, maxBudget).stream()
                .map(ProjectDto::from).toList();
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id) {
        return ProjectDto.from(projectService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectRequest req, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        ProjectDto dto = ProjectDto.from(projectService.create(user.getId(), req.title(), req.description(),
                req.budget(), req.deadline(), req.categoryId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req,
                             HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        return ProjectDto.from(projectService.update(id, user.getId(), req.title(), req.description(),
                req.budget(), req.deadline(), req.categoryId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT, Role.ADMIN);
        projectService.delete(id, user.getId(), user.getRole());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    public ProjectDto close(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        return ProjectDto.from(projectService.close(id, user.getId()));
    }

    @PostMapping("/{id}/complete")
    public ProjectDto complete(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        return ProjectDto.from(projectService.markComplete(id, user.getId()));
    }

    @GetMapping("/{id}/bids")
    public List<BidDto> bids(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT, Role.ADMIN);
        return bidService.findByProject(id, user.getId(), user.getRole()).stream().map(BidDto::from).toList();
    }

    @GetMapping("/{id}/review")
    public ReviewDto review(@PathVariable Long id) {
        return reviewService.findByProject(id).map(ReviewDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("No review for project " + id));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ReviewDto> createReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest req,
                                                  HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        ReviewDto dto = ReviewDto.from(reviewService.createReview(id, user.getId(), req.rating(), req.comment()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
