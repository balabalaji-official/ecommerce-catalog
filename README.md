# E-Commerce Product Catalog Service

A Spring Boot REST API for managing a product catalog: create, retrieve,
update, delete, and filter/paginate a few hundred thousand products, where
reads vastly outnumber writes.

## 1. Datastore choice

**H2, in-memory, accessed through Spring Data JPA (relational).**

Rationale:
- Filtering by category + price range is a relational-shaped query
  (`WHERE category = ? AND price BETWEEN ? AND ?`), and B-tree indexes on
  those columns make it cheap even at a few hundred thousand rows.
- Pagination (`LIMIT`/`OFFSET` via `Pageable`) is native and well-optimized.
- `@Version`-based optimistic locking gives safe concurrent writes for free.
- It's a genuine relational engine (not a fake), so the query/index/locking
  behavior described here is real, while still requiring zero external
  setup — it fits the "in-memory, runnable in an hour" constraint.

A document or KV store would be a reasonable alternative for very high
write throughput or a fast-changing/unstructured schema, but for this
workload (read-heavy, structured, range-filterable) relational + indexes is
the better fit.

## 2. Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Repository** | `ProductRepository` (Spring Data JPA) | Isolates persistence details from the service layer. |
| **Specification (Strategy + Factory)** | `ProductSpecifications` | Each filter (category, min price, max price, name) is an independent predicate; combined dynamically. Adding a new filter never touches existing ones. |
| **DTO + Mapper** | `dto/*`, `ProductMapper` | Decouples the public API contract from the JPA entity so either can evolve independently. |
| **Builder** | `Product`, DTOs (via Lombok `@Builder`) | Readable, safe construction of objects with many fields. |
| **Cache-Aside** | `ProductServiceImpl.getProduct` + `ProductCache` | Check cache → miss → read DB → populate cache, for the hot single-product read path. |
| **Centralized exception handling** | `GlobalExceptionHandler` (`@RestControllerAdvice`) | One place maps exceptions → HTTP status/body, keeping controllers clean. |
| **Dependency Injection / Singleton** | All `@Service`/`@Component`/`@Repository` beans | Managed lifecycle and testability via Spring's container. |
| **Optimistic Locking** | `@Version` on `Product` | Safe concurrent updates without pessimistic row locks. |

## 3. Where multithreading is used, and why

Two distinct, intentional uses — not multithreading for its own sake:

1. **`ProductCache` — concurrent shared-state protection.**
   A hand-rolled, bounded LRU cache for single-product lookups, guarded by
   a `ReentrantReadWriteLock`. Reads (the overwhelming majority per the
   stated workload) can proceed fully in parallel; writes (`put`/`evict`)
   get exclusive access only for the instant needed to mutate the map.
   This is a deliberate alternative to a plain `synchronized` map, which
   would serialize even concurrent readers and hurt exactly the access
   pattern this service is optimized for.

2. **`DataSeeder` — parallel work-splitting for a one-time bulk job.**
   On startup, an `ExecutorService` fans the generation of sample catalog
   data out across a fixed thread pool (batches persisted independently),
   then the main thread joins on all `Future`s before continuing. This
   mirrors how you'd parallelize a real bulk import/reindex job.

3. **`@Async` cache maintenance (`ProductCacheUpdater`) on a dedicated
   thread pool (`AsyncConfig`).** After a write commits to the database,
   cache warm/evict is dispatched to a background executor so client
   requests are never held up by cache bookkeeping. (Implemented as its own
   Spring bean rather than a private method on the service, because
   Spring's proxy-based `@Async` doesn't intercept self-invocation.)

List/filter queries deliberately **do not** go through the cache — the
filter combination space is unbounded, so caching there would thrash. They
rely on database indexes + pagination instead.

## 4. API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/products` | Create a product |
| `GET` | `/api/products/{id}` | Get one product (cache-aside) |
| `PUT` | `/api/products/{id}` | Update a product (optimistic locking) |
| `DELETE` | `/api/products/{id}` | Delete a product |
| `GET` | `/api/products?category=&minPrice=&maxPrice=&q=&page=&size=&sortBy=&sortDir=` | Filtered, paginated list |

Example request body:
```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "category": "Electronics",
  "price": 29.99,
  "stockQuantity": 50,
  "sku": "SKU-001"
}
```

Example filtered list call:
```
GET /api/products?category=Electronics&minPrice=10&maxPrice=100&page=0&size=20&sortBy=price&sortDir=asc
```

## 5. Project layout

```
src/main/java/com/example/catalog/
  CatalogApplication.java
  config/AsyncConfig.java
  controller/ProductController.java
  dto/ (ProductRequest, ProductResponse, PagedResponse)
  entity/Product.java
  exception/ (ProductNotFoundException, DuplicateSkuException, GlobalExceptionHandler, ErrorResponse)
  mapper/ProductMapper.java
  repository/ (ProductRepository, ProductSpecifications)
  service/ProductService.java
  service/impl/ProductServiceImpl.java
  service/cache/ (ProductCache, ProductCacheUpdater)
  seed/DataSeeder.java
src/main/resources/application.yml
src/test/java/.../ProductApiTests.java
```

## 6. How to run

**Prerequisites:** JDK 17+ and Maven 3.8+ (or use the Maven Wrapper if you
add one). Requires internet access on first build to download dependencies.

```bash
# from the project root (where pom.xml lives)
mvn clean install
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

On startup it seeds ~5,000 sample products across several categories (via
`DataSeeder`) so the filter/pagination endpoints have real volume to page
through. Adjust or disable this in `application.yml`:
```yaml
catalog:
  seed:
    enabled: true
    productCount: 5000
```

**H2 console** (to inspect the in-memory DB directly): visit
`http://localhost:8080/h2-console` and connect with JDBC URL
`jdbc:h2:mem:catalogdb`, user `sa`, empty password.

**Run tests:**
```bash
mvn test
```

**Quick smoke test with curl:**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","category":"Electronics","price":29.99,"stockQuantity":50,"sku":"SKU-001"}'

curl "http://localhost:8080/api/products?category=Electronics&minPrice=10&maxPrice=100&page=0&size=10"
```

## 7. Notes / trade-offs given the time-boxed scope

- No auth/authz layer — out of scope for this exercise, but the
  `GlobalExceptionHandler` and layered structure make it straightforward to
  add (e.g. Spring Security + a filter).
- No API documentation UI (e.g. springdoc-openapi) wired in, to keep the
  dependency list minimal; easy to add.
- Data is in-memory (H2) and resets on restart — appropriate for this
  assessment; swapping to Postgres/MySQL in production is a config-only
  change (same JPA/Hibernate code), since JDBC URL/driver is the only thing
  that would change.
