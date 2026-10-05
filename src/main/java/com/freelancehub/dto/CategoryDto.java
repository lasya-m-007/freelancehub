package com.freelancehub.dto;

import com.freelancehub.model.Category;

public record CategoryDto(Long id, String name) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }
}
