package com.example.catalog.repository;

import com.example.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repository pattern via Spring Data JPA - persistence details (SQL,
 * connection handling) stay out of the service layer entirely.
 *
 * JpaSpecificationExecutor gives us composable, type-safe dynamic queries
 * for the filter/list endpoint (see {@link ProductSpecifications}).
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}
