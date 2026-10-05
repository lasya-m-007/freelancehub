package com.freelancehub.security;

import com.freelancehub.model.Role;
import java.io.Serializable;

/** Minimal identity kept in the HttpSession (id + role, as the guide specifies). */
public class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final Role role;

    public SessionUser(Long id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public boolean isClient() { return role == Role.CLIENT; }
    public boolean isFreelancer() { return role == Role.FREELANCER; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}
