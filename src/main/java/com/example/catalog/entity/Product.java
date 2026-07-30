package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Core persistence model. Indexes on category / price (and the composite)
 * back the filtered-list endpoint, since reads vastly outnumber writes and
 * category+price-range is the dominant query shape.
 *
 * @Version enables optimistic locking so concurrent updates to the same
 * product (e.g. two admins editing stock at once) fail fast with a 409
 * instead of silently clobbering each other.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_category", columnList = "category"),
                @Index(name = "idx_product_price", columnList = "price"),
                @Index(name = "idx_product_category_price", columnList = "category, price"),
                @Index(name = "idx_product_sku", columnList = "sku", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
