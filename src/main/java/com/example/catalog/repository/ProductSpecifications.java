package com.example.catalog.repository;

import com.example.catalog.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Specification (Strategy + Factory) pattern: each filter is an independent,
 * composable predicate. The controller/service never builds JPQL/criteria
 * directly - they just ask this factory for a combined Specification, which
 * keeps filtering logic testable and open for extension (adding a new filter
 * = adding one new static method, no changes to existing ones).
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) ->
                StringUtils.hasText(category) ? cb.equal(root.get("category"), category) : null;
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) ->
                minPrice != null ? cb.greaterThanOrEqualTo(root.get("price"), minPrice) : null;
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) ->
                maxPrice != null ? cb.lessThanOrEqualTo(root.get("price"), maxPrice) : null;
    }

    public static Specification<Product> nameContains(String nameQuery) {
        return (root, query, cb) ->
                StringUtils.hasText(nameQuery)
                        ? cb.like(cb.lower(root.get("name")), "%" + nameQuery.toLowerCase() + "%")
                        : null;
    }

    /**
     * Combines every optional filter into a single specification. Null
     * predicates (returned by the strategies above when a filter isn't
     * supplied) are safely ignored by Specification.where(...).and(...).
     */
    public static Specification<Product> buildFilter(String category, BigDecimal minPrice,
                                                       BigDecimal maxPrice, String nameQuery) {
        return Specification
                .where(hasCategory(category))
                .and(priceGreaterThanOrEqual(minPrice))
                .and(priceLessThanOrEqual(maxPrice))
                .and(nameContains(nameQuery));
    }
}
