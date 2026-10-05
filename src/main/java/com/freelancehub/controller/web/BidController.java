package com.freelancehub.controller.web;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.model.Project;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import com.freelancehub.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Freelancer bid actions: submit and withdraw. */
@Controller
public class BidController {

    private final BidService bidService;
    private final ProjectService projectService;
    private final AuthHelper auth;

    public BidController(BidService bidService, ProjectService projectService, AuthHelper auth) {
        this.bidService = bidService;
        this.projectService = projectService;
        this.auth = auth;
    }

    @GetMapping("/projects/{id}/bid")
    public String form(@PathVariable Long id, HttpServletRequest request, Model model, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER);
        Project project = projectService.getById(id);
        if (!project.isOpen()) {
            ra.addFlashAttribute("msgError", "This project is not open for bids.");
            return "redirect:/projects/" + id;
        }
        if (bidService.hasBid(id, user.getId())) {
            ra.addFlashAttribute("msgError", "You have already placed a bid on this project.");
            return "redirect:/projects/" + id;
        }
        model.addAttribute("project", project);
        return "bid-form";
    }

    @PostMapping("/projects/{id}/bid")
    public String submit(@PathVariable Long id,
                         @RequestParam(required = false) BigDecimal amount,
                         @RequestParam(defaultValue = "") String message,
                         HttpServletRequest request, Model model, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER);
        try {
            bidService.placeBid(user.getId(), id, amount, message);
            ra.addFlashAttribute("msgSuccess", "Your bid was submitted.");
            return "redirect:/freelancer/dashboard";
        } catch (BusinessRuleException ex) {
            Map<String, Object> form = new HashMap<>();
            form.put("amount", amount);
            form.put("message", message);
            model.addAttribute("form", form);
            model.addAttribute("project", projectService.getById(id));
            model.addAttribute("msgError", ex.getMessage());
            return "bid-form";
        }
    }

    @PostMapping("/bids/{id}/withdraw")
    public String withdraw(@PathVariable Long id, HttpServletRequest request, RedirectAttributes ra) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER);
        try {
            bidService.delete(id, user.getId(), user.getRole());
            ra.addFlashAttribute("msgSuccess", "Bid withdrawn.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/freelancer/dashboard";
    }
}
