package com.freelancehub.service;

import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import java.util.List;

public interface UserService {
    User register(String name, String email, String rawPassword, Role role);
    User authenticate(String email, String rawPassword);
    User findById(Long id);
    List<User> findAll();
    User update(Long id, String name, String newPassword);
    void delete(Long id);
    long count();
    long countByRole(Role role);
}
