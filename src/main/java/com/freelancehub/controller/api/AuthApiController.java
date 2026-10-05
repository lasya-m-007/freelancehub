package com.freelancehub.controller.api;

import com.freelancehub.dto.LoginRequest;
import com.freelancehub.dto.RegisterRequest;
import com.freelancehub.dto.UserDto;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserService userService;
    private final AuthHelper auth;

    public AuthApiController(UserService userService, AuthHelper auth) {
        this.userService = userService;
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new ForbiddenException("Admin accounts cannot be self-registered");
        }
        User user = userService.register(req.name(), req.email(), req.password(), req.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
    }

    /** Starts a session; Postman keeps the JSESSIONID cookie automatically. */
    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        User user = userService.authenticate(req.email(), req.password());
        auth.login(request, user);
        return UserDto.from(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me(HttpServletRequest request) {
        SessionUser su = auth.requireLogin(request);
        return UserDto.from(userService.findById(su.getId()));
    }
}
