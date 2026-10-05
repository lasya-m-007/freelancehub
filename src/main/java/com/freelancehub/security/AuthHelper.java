package com.freelancehub.security;

import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.UnauthorizedException;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import org.springframework.stereotype.Component;

/** Session login/logout + role guards used at the top of every protected controller method. */
@Component
public class AuthHelper {

    public static final String SESSION_KEY = "user";

    /** Start a fresh session (prevents session fixation) and store id + role. */
    public void login(HttpServletRequest request, User user) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_KEY, new SessionUser(user.getId(), user.getName(), user.getRole()));
    }

    public void refresh(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(SESSION_KEY, new SessionUser(user.getId(), user.getName(), user.getRole()));
        }
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** @return the logged-in user or null (never creates a session). */
    public SessionUser current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object o = session.getAttribute(SESSION_KEY);
        return (o instanceof SessionUser su) ? su : null;
    }

    public SessionUser requireLogin(HttpServletRequest request) {
        SessionUser user = current(request);
        if (user == null) {
            throw new UnauthorizedException("Please log in to continue");
        }
        return user;
    }

    public SessionUser requireRole(HttpServletRequest request, Role... roles) {
        SessionUser user = requireLogin(request);
        if (!Arrays.asList(roles).contains(user.getRole())) {
            throw new ForbiddenException("You do not have permission to do that");
        }
        return user;
    }

    public String dashboardPath(Role role) {
        return switch (role) {
            case ADMIN -> "/admin/dashboard";
            case CLIENT -> "/client/dashboard";
            case FREELANCER -> "/freelancer/dashboard";
        };
    }
}
