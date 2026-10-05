package com.freelancehub.dto;

import com.freelancehub.model.Role;
import com.freelancehub.model.User;

public record UserDto(Long id, String name, String email, Role role, double averageRating, int ratingCount) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getAverageRating(), u.getRatingCount());
    }
}
