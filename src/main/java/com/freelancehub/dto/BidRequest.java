package com.freelancehub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** projectId is required when creating a bid; ignored on update. */
public record BidRequest(Long projectId,
                         @NotNull @DecimalMin(value = "1.00") BigDecimal amount,
                         @NotBlank @Size(max = 2000) String message) { }
