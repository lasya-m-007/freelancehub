package com.freelancehub.controller.web;

import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes the logged-in user to every Thymeleaf page as ${currentUser} (null when anonymous). */
@ControllerAdvice(basePackages = "com.freelancehub.controller.web")
public class GlobalModelAttributes {

    private final AuthHelper auth;

    public GlobalModelAttributes(AuthHelper auth) {
        this.auth = auth;
    }

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpServletRequest request) {
        return auth.current(request);
    }
}
