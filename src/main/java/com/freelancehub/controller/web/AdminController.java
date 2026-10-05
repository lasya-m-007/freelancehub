package com.freelancehub.controller.web;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import com.freelancehub.service.CategoryService;
import com.freelancehub.service.ProjectService;
import com.freelancehub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final ProjectService projectService;
    private final BidService bidService;
    private final AuthHelper auth;

    public AdminController(UserService userService, CategoryService categoryService,
                           ProjectService projectService, BidService bidService, AuthHelper auth) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.projectService = projectService;
        this.bidService = bidService;
        this.auth = auth;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        auth.requireRole(request, Role.ADMIN);
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("clientCount", userService.countByRole(Role.CLIENT));
        model.addAttribute("freelancerCount", userService.countByRole(Role.FREELANCER));
        model.addAttribute("totalProjects", projectService.count());
        model.addAttribute("openProjects", projectService.countByStatus(ProjectStatus.OPEN));
        model.addAttribute("inProgressProjects", projectService.countByStatus(ProjectStatus.IN_PROGRESS));
        model.addAttribute("completedProjects", projectService.countByStatus(ProjectStatus.COMPLETED));
        model.addAttribute("totalBids", bidService.count());
        model.addAttribute("totalCategories", categoryService.count());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(HttpServletRequest request, Model model) {
        auth.requireRole(request, Role.ADMIN);
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpServletRequest request, RedirectAttributes ra) {
        SessionUser admin = auth.requireRole(request, Role.ADMIN);
        try {
            if (admin.getId().equals(id)) {
                throw new BusinessRuleException("You cannot delete your own account");
            }
            userService.delete(id);
            ra.addFlashAttribute("msgSuccess", "User deleted.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("msgError", "This user still has projects, bids or reviews and cannot be deleted.");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/categories")
    public String categories(HttpServletRequest request, Model model) {
        auth.requireRole(request, Role.ADMIN);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam(defaultValue = "") String name, HttpServletRequest request,
                              RedirectAttributes ra) {
        auth.requireRole(request, Role.ADMIN);
        try {
            categoryService.create(name);
            ra.addFlashAttribute("msgSuccess", "Category added.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, HttpServletRequest request, RedirectAttributes ra) {
        auth.requireRole(request, Role.ADMIN);
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("msgSuccess", "Category deleted.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
