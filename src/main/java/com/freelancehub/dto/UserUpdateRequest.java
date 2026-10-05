package com.freelancehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** newPassword is optional; leave null/blank to keep the current password. */
public record UserUpdateRequest(@NotBlank @Size(max = 100) String name, String newPassword) { }
