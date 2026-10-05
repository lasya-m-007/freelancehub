package com.freelancehub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectRequest(@NotBlank @Size(max = 200) String title,
                             @NotBlank @Size(max = 4000) String description,
                             @NotNull @DecimalMin(value = "1.00") BigDecimal budget,
                             @NotNull @FutureOrPresent LocalDate deadline,
                             @NotNull Long categoryId) { }
