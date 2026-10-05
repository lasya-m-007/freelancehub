package com.freelancehub.controller.web;

import com.freelancehub.model.ProjectStatus;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AuthHelper auth;
    private final ProjectService projectService;

    public HomeController(AuthHelper auth, ProjectService projectService) {
        this.auth = auth;
        this.projectService = projectService;
    }

    @GetMapping("/")
    public String landing(HttpServletRequest request, Model model) {
        SessionUser user = auth.current(request);
        if (user != null) {
            return "redirect:" + auth.dashboardPath(user.getRole());
        }
        model.addAttribute("recent",
                projectService.search(null, ProjectStatus.OPEN, null, null, null).stream().limit(3).toList());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request) {
        SessionUser user = auth.requireLogin(request);
        return "redirect:" + auth.dashboardPath(user.getRole());
    }
}
