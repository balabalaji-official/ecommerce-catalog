package com.example.catalog.controller;

import com.example.catalog.dto.PagedResponse;
import com.example.catalog.dto.ProductRequest;
import com.example.catalog.dto.ProductResponse;
import com.example.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Filtered, paginated listing.
     * Example: GET /api/products?category=Electronics&minPrice=10&maxPrice=500&page=0&size=20&sortBy=price&sortDir=asc
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        int safeSize = Math.min(size, 100); // cap page size to protect the DB from abusive requests
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc")
                ? Sort.Order.desc(sortBy) : Sort.Order.asc(sortBy));
        Pageable pageable = PageRequest.of(page, safeSize, sort);

        Page<ProductResponse> result = productService.listProducts(category, minPrice, maxPrice, q, pageable);
        return ResponseEntity.ok(PagedResponse.from(result));
    }
}
