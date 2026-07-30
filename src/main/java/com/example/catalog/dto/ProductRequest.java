package com.example.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Inbound API contract - intentionally decoupled from the JPA entity so the
 * persistence model can evolve independently of what clients send.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "category is required")
    @Size(max = 100)
    private String category;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "price must be >= 0")
    private BigDecimal price;

    @NotNull(message = "stockQuantity is required")
    @Min(value = 0, message = "stockQuantity must be >= 0")
    private Integer stockQuantity;

    @NotBlank(message = "sku is required")
    @Size(max = 64)
    private String sku;
}
