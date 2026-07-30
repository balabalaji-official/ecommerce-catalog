package com.example.catalog.service.impl;

import com.example.catalog.dto.ProductRequest;
import com.example.catalog.dto.ProductResponse;
import com.example.catalog.entity.Product;
import com.example.catalog.exception.DuplicateSkuException;
import com.example.catalog.exception.ProductNotFoundException;
import com.example.catalog.mapper.ProductMapper;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.repository.ProductSpecifications;
import com.example.catalog.service.ProductService;
import com.example.catalog.service.cache.ProductCache;
import com.example.catalog.service.cache.ProductCacheUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Cache-aside pattern for point reads (getProduct): check cache -> on miss,
 * read DB -> populate cache. Writes go straight to the DB inside a
 * transaction (source of truth), then cache maintenance (evict/repopulate)
 * is dispatched to a background thread pool via @Async so the client isn't
 * kept waiting on cache bookkeeping.
 *
 * List/filter queries deliberately bypass the cache - the filter
 * combination space (category x price range x name x page) is effectively
 * unbounded, so caching would thrash; instead we lean on DB indexes
 * (see Product entity) and pagination to keep those queries cheap.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCache productCache;
    private final ProductCacheUpdater productCacheUpdater;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }
        Product saved = productRepository.save(ProductMapper.toEntity(request));
        productCacheUpdater.warm(saved);
        return ProductMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product cached = productCache.get(id);
        if (cached != null) {
            return ProductMapper.toResponse(cached);
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productCacheUpdater.warm(product);
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // if the SKU is changing, make sure it doesn't collide with another product
        if (!existing.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        ProductMapper.applyUpdates(existing, request);
        Product saved = productRepository.save(existing); // @Version guards against lost updates
        productCacheUpdater.warm(saved);
        return ProductMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        productCacheUpdater.evict(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(String category, BigDecimal minPrice, BigDecimal maxPrice,
                                               String nameQuery, Pageable pageable) {
        var spec = ProductSpecifications.buildFilter(category, minPrice, maxPrice, nameQuery);
        return productRepository.findAll(spec, pageable).map(ProductMapper::toResponse);
    }


}
