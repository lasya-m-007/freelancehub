package com.freelancehub.controller.web;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.InvalidCredentialsException;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthHelper auth;

    public AuthController(UserService userService, AuthHelper auth) {
        this.userService = userService;
        this.auth = auth;
    }

    @GetMapping("/register")
    public String registerForm(HttpServletRequest request) {
        SessionUser user = auth.current(request);
        return user != null ? "redirect:" + auth.dashboardPath(user.getRole()) : "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam(defaultValue = "") String name,
                           @RequestParam(defaultValue = "") String email,
                           @RequestParam(defaultValue = "") String password,
                           @RequestParam(defaultValue = "") String role,
                           Model model, RedirectAttributes ra) {
        try {
            Role parsed = parsePublicRole(role);
            userService.register(name, email, password, parsed);
            ra.addFlashAttribute("msgSuccess", "Account created. Please log in.");
            return "redirect:/login";
        } catch (BusinessRuleException ex) {
            Map<String, Object> form = new HashMap<>();
            form.put("name", name);
            form.put("email", email);
            form.put("role", role);
            model.addAttribute("form", form);
            model.addAttribute("msgError", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm(HttpServletRequest request) {
        SessionUser user = auth.current(request);
        return user != null ? "redirect:" + auth.dashboardPath(user.getRole()) : "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = "") String email,
                        @RequestParam(defaultValue = "") String password,
                        HttpServletRequest request, Model model) {
        try {
            User user = userService.authenticate(email, password);
            auth.login(request, user);
            return "redirect:" + auth.dashboardPath(user.getRole());
        } catch (InvalidCredentialsException ex) {
            model.addAttribute("msgError", ex.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        auth.logout(request);
        return "redirect:/login?loggedout";
    }

    /** Public sign-up may only create CLIENT or FREELANCER accounts. */
    private Role parsePublicRole(String raw) {
        try {
            Role role = Role.valueOf(raw.trim().toUpperCase());
            if (role == Role.ADMIN) throw new BusinessRuleException("Invalid account type");
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Please choose an account type");
        }
    }
}
