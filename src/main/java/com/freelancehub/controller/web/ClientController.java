package com.freelancehub.controller.web;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.model.Bid;
import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import com.freelancehub.service.CategoryService;
import com.freelancehub.service.ProjectService;
import com.freelancehub.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/client")
public class ClientController {

    private final ProjectService projectService;
    private final BidService bidService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final AuthHelper auth;

    public ClientController(ProjectService projectService, BidService bidService,
                            CategoryService categoryService, ReviewService reviewService, AuthHelper auth) {
        this.projectService = projectService;
        this.bidService = bidService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
        this.auth = auth;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        List<Project> projects = projectService.findByClient(user.getId());
        Map<ProjectStatus, Long> byStatus = projects.stream()
                .collect(Collectors.groupingBy(Project::getStatus, Collectors.counting()));
        List<Project> recent = projects.stream().limit(4).toList();
        long totalBids = bidService.countByProjects(projects).values().stream().mapToLong(Long::longValue).sum();

        model.addAttribute("totalProjects", projects.size());
        model.addAttribute("openCount", byStatus.getOrDefault(ProjectStatus.OPEN, 0L));
        model.addAttribute("inProgressCount", byStatus.getOrDefault(ProjectStatus.IN_PROGRESS, 0L));
        model.addAttribute("completedCount", byStatus.getOrDefault(ProjectStatus.COMPLETED, 0L));
        model.addAttribute("totalBids", totalBids);
        model.addAttribute("recent", recent);
        model.addAttribute("bidCounts", bidService.countByProjects(recent));
        return "client/dashboard";
    }

    @GetMapping("/projects")
    public String myProjects(HttpServletRequest request, Model model) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        List<Project> projects = projectService.findByClient(user.getId());
        model.addAttribute("projects", projects);
        model.addAttribute("bidCounts", bidService.countByProjects(projects));
        return "client/projects";
    }

    @GetMapping("/projects/new")
    public String newProjectForm(HttpServletRequest request, Model model) {
        auth.requireRole(request, Role.CLIENT);
        model.addAttribute("categories", categoryService.findAll());
        return "client/project-form";
    }

    @PostMapping("/projects")
    public String createProject(@RequestParam(defaultValue = "") String title,
                                @RequestParam(defaultValue = "") String description,
                                @RequestParam(required = false) BigDecimal budget,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                                @RequestParam(required = false) Long categoryId,
                                HttpServletRequest request, Model model, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        try {
            projectService.create(user.getId(), title, description, budget, deadline, categoryId);
            ra.addFlashAttribute("msgSuccess", "Project posted.");
            return "redirect:/client/projects";
        } catch (BusinessRuleException ex) {
            Map<String, Object> form = new HashMap<>();
            form.put("title", title);
            form.put("description", description);
            form.put("budget", budget);
            form.put("deadline", deadline);
            form.put("categoryId", categoryId);
            model.addAttribute("form", form);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("msgError", ex.getMessage());
            return "client/project-form";
        }
    }

    @GetMapping("/projects/{id}/bids")
    public String viewBids(@PathVariable Long id, HttpServletRequest request, Model model) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        Project project = projectService.getOwned(id, user.getId());
        model.addAttribute("project", project);
        model.addAttribute("bids", bidService.findByProject(id, user.getId(), user.getRole()));
        model.addAttribute("review", reviewService.findByProject(id).orElse(null));
        return "client/bids";
    }

    @PostMapping("/bids/{bidId}/accept")
    public String acceptBid(@PathVariable Long bidId, HttpServletRequest request, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        Long projectId = bidService.findById(bidId).getProject().getId();
        try {
            Bid bid = bidService.acceptBid(bidId, user.getId());
            ra.addFlashAttribute("msgSuccess", "Bid from " + bid.getFreelancer().getName()
                    + " accepted. All other bids were rejected.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/client/projects/" + projectId + "/bids";
    }

    @PostMapping("/projects/{id}/close")
    public String close(@PathVariable Long id, HttpServletRequest request, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        try {
            projectService.close(id, user.getId());
            ra.addFlashAttribute("msgSuccess", "Project closed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/client/projects";
    }

    @PostMapping("/projects/{id}/delete")
    public String delete(@PathVariable Long id, HttpServletRequest request, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        try {
            projectService.delete(id, user.getId(), user.getRole());
            ra.addFlashAttribute("msgSuccess", "Project deleted.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/client/projects";
    }

    @PostMapping("/projects/{id}/complete")
    public String complete(@PathVariable Long id, HttpServletRequest request, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        try {
            projectService.markComplete(id, user.getId());
            ra.addFlashAttribute("msgSuccess", "Project marked complete. Please leave a review.");
            return "redirect:/client/projects/" + id + "/review";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
            return "redirect:/client/projects/" + id + "/bids";
        }
    }

    @GetMapping("/projects/{id}/review")
    public String reviewForm(@PathVariable Long id, HttpServletRequest request, Model model, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        Project project = projectService.getOwned(id, user.getId());
        if (!project.isCompleted()) {
            ra.addFlashAttribute("msgError", "Only completed projects can be reviewed.");
            return "redirect:/client/projects/" + id + "/bids";
        }
        if (reviewService.existsForProject(id)) {
            ra.addFlashAttribute("msgError", "You have already reviewed this project.");
            return "redirect:/client/projects/" + id + "/bids";
        }
        Bid accepted = bidService.findByProject(id, user.getId(), user.getRole()).stream()
                .filter(Bid::isAccepted).findFirst().orElse(null);
        model.addAttribute("project", project);
        model.addAttribute("freelancer", accepted == null ? null : accepted.getFreelancer());
        return "client/review-form";
    }

    @PostMapping("/projects/{id}/review")
    public String submitReview(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") int rating,
                               @RequestParam(defaultValue = "") String comment,
                               HttpServletRequest request, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        try {
            reviewService.createReview(id, user.getId(), rating, comment);
            ra.addFlashAttribute("msgSuccess", "Thanks! Your review was saved and the freelancer's rating updated.");
            return "redirect:/client/projects";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
            return "redirect:/client/projects/" + id + "/review";
        }
    }
}
