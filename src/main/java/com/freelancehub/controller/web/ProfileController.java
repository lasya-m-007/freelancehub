package com.freelancehub.controller.web;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.model.User;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.ReviewService;
import com.freelancehub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;
    private final ReviewService reviewService;
    private final AuthHelper auth;

    public ProfileController(UserService userService, ReviewService reviewService, AuthHelper auth) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.auth = auth;
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest request, Model model) {
        SessionUser su = auth.requireLogin(request);
        model.addAttribute("me", userService.findById(su.getId()));
        model.addAttribute("reviews", reviewService.findByReviewee(su.getId()));
        return "profile";
    }

    @PostMapping("/profile")
    public String update(@RequestParam(defaultValue = "") String name,
                         @RequestParam(defaultValue = "") String newPassword,
                         HttpServletRequest request, RedirectAttributes ra) {
        SessionUser su = auth.requireLogin(request);
        try {
            User updated = userService.update(su.getId(), name, newPassword);
            auth.refresh(request, updated);
            ra.addFlashAttribute("msgSuccess", "Profile updated.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
