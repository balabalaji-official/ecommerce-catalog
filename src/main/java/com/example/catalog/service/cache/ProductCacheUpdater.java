package com.example.catalog.service.cache;

import com.example.catalog.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Kept as its own Spring bean (rather than plain private methods on
 * ProductServiceImpl) specifically so @Async takes effect: Spring's
 * proxy-based AOP does not intercept self-invocation (a method calling
 * another method on "this"), so @Async on a method called from within the
 * same class would silently run synchronously. Injecting this as a
 * collaborator sidesteps that pitfall.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheUpdater {

    private final ProductCache productCache;

    @Async("cacheTaskExecutor")
    public void warm(Product product) {
        try {
            productCache.put(product.getId(), product);
        } catch (Exception e) {
            log.warn("Failed to warm cache for product {}", product.getId(), e);
        }
    }

    @Async("cacheTaskExecutor")
    public void evict(Long id) {
        try {
            productCache.evict(id);
        } catch (Exception e) {
            log.warn("Failed to evict cache for product {}", id, e);
        }
    }
}
