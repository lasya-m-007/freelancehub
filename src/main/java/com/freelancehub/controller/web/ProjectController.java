package com.freelancehub.controller.web;

import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import com.freelancehub.service.CategoryService;
import com.freelancehub.service.ProjectService;
import com.freelancehub.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Public browsing: project list with combined filters + project details. */
@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final CategoryService categoryService;
    private final BidService bidService;
    private final ReviewService reviewService;
    private final AuthHelper auth;

    public ProjectController(ProjectService projectService, CategoryService categoryService,
                             BidService bidService, ReviewService reviewService, AuthHelper auth) {
        this.projectService = projectService;
        this.categoryService = categoryService;
        this.bidService = bidService;
        this.reviewService = reviewService;
        this.auth = auth;
    }

    @GetMapping("/projects")
    public String browse(@RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) BigDecimal minBudget,
                         @RequestParam(required = false) BigDecimal maxBudget,
                         @RequestParam(required = false) String status,
                         Model model) {
        String statusParam = status == null || status.isBlank() ? "OPEN" : status.trim().toUpperCase();
        ProjectStatus statusFilter;
        if ("ALL".equals(statusParam)) {
            statusFilter = null;
        } else {
            try {
                statusFilter = ProjectStatus.valueOf(statusParam);
            } catch (IllegalArgumentException ex) {
                statusFilter = ProjectStatus.OPEN;
                statusParam = "OPEN";
            }
        }
        List<Project> projects = projectService.search(categoryId, statusFilter, keyword, minBudget, maxBudget);

        Map<String, Object> filters = new HashMap<>();
        filters.put("categoryId", categoryId);
        filters.put("keyword", keyword);
        filters.put("minBudget", minBudget);
        filters.put("maxBudget", maxBudget);
        filters.put("status", statusParam);

        model.addAttribute("projects", projects);
        model.addAttribute("bidCounts", bidService.countByProjects(projects));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("f", filters);
        return "projects";
    }

    @GetMapping("/projects/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        Project project = projectService.getById(id);
        SessionUser user = auth.current(request);
        boolean owner = user != null && user.getId().equals(project.getClient().getId());
        boolean alreadyBid = user != null && user.isFreelancer() && bidService.hasBid(id, user.getId());
        boolean canBid = user != null && user.isFreelancer() && project.isOpen() && !alreadyBid;

        model.addAttribute("project", project);
        model.addAttribute("owner", owner);
        model.addAttribute("canBid", canBid);
        model.addAttribute("alreadyBid", alreadyBid);
        model.addAttribute("bidCount", bidService.countByProject(id));
        model.addAttribute("review", reviewService.findByProject(id).orElse(null));
        return "project-detail";
    }
}
