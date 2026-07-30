package com.example.catalog.seed;

import com.example.catalog.entity.Product;
import com.example.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Populates the in-memory H2 store on startup so the filter/pagination
 * endpoints have realistic volume to page through.
 *
 * Generation is parallelized (CPU-bound: building 5,000 Product objects),
 * using a parallel stream. The actual DB write is a single saveAll() call
 * from the main thread.
 *
 * IMPORTANT: the objects are NOT persisted concurrently. With
 * GenerationType.IDENTITY, letting several threads call saveAll() at the
 * same time against H2 can race on auto-increment id assignment and throw
 * a duplicate primary key error under load. Building the data in parallel
 * and writing it from one thread gets the CPU speedup with none of that
 * risk - correctness beats a marginal write-side speedup for a one-time
 * startup task.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String[] CATEGORIES = {
            "Electronics", "Home & Kitchen", "Sports & Outdoors", "Books", "Toys", "Apparel", "Grocery"
    };

    private final ProductRepository productRepository;

    @Value("${catalog.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${catalog.seed.productCount:5000}")
    private int productCount;

    @Override
    public void run(String... args) {
        if (!seedEnabled || productRepository.count() > 0) {
            return;
        }

        long start = System.currentTimeMillis();

        // Parallel, CPU-bound generation - no shared mutable state, so this is safe.
        List<Product> products = IntStream.range(0, productCount)
                .parallel()
                .mapToObj(this::buildProduct)
                .collect(Collectors.toList());

        // Single-threaded, batched write - avoids identity-generation races.
        productRepository.saveAll(products);

        log.info("Seeded {} products in {} ms", productRepository.count(),
                System.currentTimeMillis() - start);
    }

    private Product buildProduct(int seq) {
        String category = CATEGORIES[seq % CATEGORIES.length];
        BigDecimal price = BigDecimal.valueOf(5 + (seq % 995) + 0.99)
                .setScale(2, RoundingMode.HALF_UP);
        return Product.builder()
                .name(category + " Item " + seq)
                .description("Auto-generated sample product #" + seq)
                .category(category)
                .price(price)
                .stockQuantity(10 + (seq % 100))
                .sku("SKU-" + seq)
                .build();
    }
}
