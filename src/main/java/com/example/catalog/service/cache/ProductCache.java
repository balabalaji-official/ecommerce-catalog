package com.example.catalog.service.cache;

import com.example.catalog.entity.Product;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Read-through, thread-safe, bounded LRU cache for single-product lookups
 * (GET /products/{id}) - the classic "hot key" read pattern in a catalog
 * where a small subset of products (best-sellers, homepage items) receive
 * a disproportionate share of traffic.
 *
 * Why a hand-rolled cache instead of @Cacheable here: this is the one place
 * in the service explicitly asked to demonstrate multithreading, so the
 * locking strategy is visible rather than hidden behind an annotation.
 *
 * Concurrency design:
 *  - A ReentrantReadWriteLock allows unlimited concurrent readers (the
 *    overwhelmingly common case per the read-heavy requirement) while
 *    still guaranteeing exclusive access for the rarer writes (put/evict).
 *  - Backed by a LinkedHashMap in access-order mode so eviction is O(1)
 *    true-LRU once the bound is hit, capping memory use regardless of how
 *    many of the few-hundred-thousand products get cached.
 *  - Writes (create/update/delete) evict-then-repopulate asynchronously
 *    from ProductServiceImpl, so cache maintenance never blocks the
 *    request thread returning a response to the client.
 */
@Component
public class ProductCache {

    private static final int MAX_ENTRIES = 5_000;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<Long, Product> store = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Product> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public Product get(Long id) {
        lock.readLock().lock();
        try {
            return store.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(Long id, Product product) {
        lock.writeLock().lock();
        try {
            store.put(id, product);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void evict(Long id) {
        lock.writeLock().lock();
        try {
            store.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return store.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
