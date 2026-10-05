package com.freelancehub.service.impl;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.DuplicateResourceException;
import com.freelancehub.exception.InvalidCredentialsException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.service.UserService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final int MIN_PASSWORD = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(String name, String email, String rawPassword, Role role) {
        if (name == null || name.isBlank()) throw new BusinessRuleException("Name is required");
        if (name.trim().length() > 100) throw new BusinessRuleException("Name is too long");
        if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessRuleException("A valid email is required");
        }
        checkPassword(rawPassword);
        if (role == null) throw new BusinessRuleException("Role is required");

        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            throw new DuplicateResourceException("This email is already registered");
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalized);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized).orElseThrow(InvalidCredentialsException::new);
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
    }

    @Override
    @Transactional
    public User update(Long id, String name, String newPassword) {
        User user = findById(id);
        if (name == null || name.isBlank()) throw new BusinessRuleException("Name is required");
        if (name.trim().length() > 100) throw new BusinessRuleException("Name is too long");
        user.setName(name.trim());
        if (newPassword != null && !newPassword.isBlank()) {
            checkPassword(newPassword);
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessRuleException("Admin accounts cannot be deleted");
        }
        userRepository.delete(user);
        userRepository.flush(); // surface FK violations here, not at commit
    }

    @Override
    @Transactional(readOnly = true)
    public long count() { return userRepository.count(); }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(Role role) { return userRepository.countByRole(role); }

    private void checkPassword(String raw) {
        if (raw == null || raw.length() < MIN_PASSWORD) {
            throw new BusinessRuleException("Password must be at least " + MIN_PASSWORD + " characters");
        }
        if (raw.length() > 72) {
            throw new BusinessRuleException("Password must be at most 72 characters");
        }
    }
}
