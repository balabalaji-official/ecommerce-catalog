package com.example.catalog.service;

import com.example.catalog.dto.ProductRequest;
import com.example.catalog.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProduct(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    Page<ProductResponse> listProducts(String category, BigDecimal minPrice, BigDecimal maxPrice,
                                        String nameQuery, Pageable pageable);
}
