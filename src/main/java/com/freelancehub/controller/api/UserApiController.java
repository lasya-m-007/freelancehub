package com.freelancehub.controller.api;

import com.freelancehub.dto.RegisterRequest;
import com.freelancehub.dto.UserDto;
import com.freelancehub.dto.UserUpdateRequest;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;
    private final AuthHelper auth;

    public UserApiController(UserService userService, AuthHelper auth) {
        this.userService = userService;
        this.auth = auth;
    }

    @GetMapping
    public List<UserDto> list(HttpServletRequest request) {
        auth.requireRole(request, Role.ADMIN);
        return userService.findAll().stream().map(UserDto::from).toList();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id, HttpServletRequest request) {
        requireSelfOrAdmin(request, id);
        return UserDto.from(userService.findById(id));
    }

    /** Admin-only creation (any role). Public sign-up is POST /api/auth/register. */
    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) {
        auth.requireRole(request, Role.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(
                userService.register(req.name(), req.email(), req.password(), req.role())));
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest req,
                          HttpServletRequest request) {
        requireSelfOrAdmin(request, id);
        return UserDto.from(userService.update(id, req.name(), req.newPassword()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        auth.requireRole(request, Role.ADMIN);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireSelfOrAdmin(HttpServletRequest request, Long id) {
        SessionUser user = auth.requireLogin(request);
        if (!user.isAdmin() && !user.getId().equals(id)) {
            throw new ForbiddenException("You can only access your own account");
        }
    }
}
