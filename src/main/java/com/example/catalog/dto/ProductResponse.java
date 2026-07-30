package com.example.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private String sku;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
