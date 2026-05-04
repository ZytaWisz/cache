# Code Review — Cache / Customer Platform

**Reviewer:** Senior Java Developer  
**Date:** 2026-05-04  
**Scope:** Full codebase — `customer-service`, `customer-audit-service`, legacy `src/` module, infrastructure (k8s, Docker Compose)

---

## Table of Contents

1. [Overall Architecture Assessment](#1-overall-architecture-assessment)
2. [Critical Issues](#2-critical-issues)
3. [Module: customer-service](#3-module-customer-service)
4. [Module: customer-audit-service](#4-module-customer-audit-service)
5. [Legacy `src/` Module](#5-legacy-src-module)
6. [Build & Dependency Management](#6-build--dependency-management)
7. [Configuration & Infrastructure](#7-configuration--infrastructure)
8. [Testing](#8-testing)
9. [Clean Code & General Observations](#9-clean-code--general-observations)
10. [Summary Score Card](#10-summary-score-card)

---

## 1. Overall Architecture Assessment

The project demonstrates a well-chosen set of architectural patterns:

- **Transactional Outbox Pattern** — changes to `Customer` are stored in an `outbox_event` table in the same transaction, and Debezium reads from it via CDC (Change Data Capture) to reliably publish events to Kafka.
- **Two-Level Cache (L1 + L2)** — Caffeine (in-process) + Redis (distributed), with custom `TwoLevelCache` and `TwoLevelCacheManager`.
- **Microservices decomposition** — `customer-service` (write + read), `customer-audit-service` (event consumer, audit log).
- **Spring Transactional Events** — cache eviction is tied to `AFTER_COMMIT`, which correctly avoids a race condition between the cache invalidation and the actual commit.

These are solid design choices. However, the execution has several correctness, quality, and maintainability issues described below.

---

## 2. Critical Issues

### 2.1 Wrong Jackson import (`tools.jackson` instead of `com.fasterxml.jackson`)

**Files affected:**
- `customer-service/src/main/java/com/example/customer/service/CustomerService.java` — line 15
- `customer-audit-service/src/main/java/com/example/audit/service/CustomerAuditLogService.java` — line 11

```java
// ❌ Wrong — non-standard, internal toolchain package
import tools.jackson.databind.ObjectMapper;

// ✅ Correct
import com.fasterxml.jackson.databind.ObjectMapper;
```

This is either a copy-paste mistake or an IDE auto-import error. The `tools.jackson` namespace is used by some internal frameworks/tooling but is **not the standard Jackson library**. This will cause a `ClassNotFoundException` at runtime in most standard Spring Boot setups. This is a **compilation-breaking** bug.

---

### 2.2 `ObjectMapper` instantiated as a local variable inside a hot path

**File:** `customer-service/.../CustomerService.java` — line 132

```java
// ❌ New ObjectMapper created on every event serialization
private String toJson(Object obj) {
    ObjectMapper mapper = new ObjectMapper();
    ...
}
```

`ObjectMapper` is thread-safe and expensive to instantiate. Creating a new instance on every call is a significant performance anti-pattern. It should be injected as a Spring bean (already available in the Spring context as `com.fasterxml.jackson.databind.ObjectMapper`) or declared as a class-level `static final` constant.

```java
// ✅ Inject as a dependency
private final ObjectMapper objectMapper;

public CustomerService(..., ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
}
```

---

### 2.3 `OutboxRepository` uses wrong ID type

**File:** `customer-service/.../OutboxRepository.java`

```java
// ❌ OutboxEvent has @Id of type Long, but repository declares UUID
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
```

The `OutboxEvent` entity has `@Id private Long id`, but the repository uses `UUID` as the key type. This is a type mismatch that will cause runtime errors on any `findById`, `deleteById`, etc. call.

```java
// ✅ Correct
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
```

---

### 2.4 Broken YAML in `application-k8s.yaml` (customer-service)

**File:** `customer-service/src/main/resources/application-k8s.yaml` — line 13

```yaml
# ❌ Garbage value appended to the property
  app:
    kafka:
      enabled: true 8080
```

`true 8080` is not a valid YAML value. This will cause application startup failure in the Kubernetes profile. Should be:

```yaml
  app:
    kafka:
      enabled: true
```

---

### 2.5 `@Transactional` on a `@KafkaListener` method

**File:** `customer-audit-service/.../CustomerAuditLogService.java` — line 25–26

```java
@Transactional
@KafkaListener(topics = "customer_topic", groupId = "customer-group")
public void listen(final String message) { ... }
```

Combining `@Transactional` with `@KafkaListener` in the same method creates ambiguity and can cause unexpected behaviour (e.g. the Kafka offset may be committed before or after the DB transaction commits, depending on the AckMode and transaction manager configuration). The method-level `@Transactional` should be extracted to a dedicated service method, keeping the listener thin:

```java
// ✅ Recommended pattern
@KafkaListener(topics = "customer_topic", groupId = "customer-group")
public void listen(final String message) {
    CustomerEventDto event = deserialize(message);
    auditLogWriter.save(event); // @Transactional on this method
}
```

---

## 3. Module: `customer-service`

### 3.1 `CustomerService` — No input validation propagated from controller

The controller accepts a `CustomerRequestBody` but neither applies `@Valid` nor does the service validate its inputs before use.

```java
// ❌ No @Valid annotation
@PostMapping("/customer")
public void createCustomer(@RequestBody CustomerRequestBody customerRequestBody) { ... }

// ✅ Should be
@PostMapping("/customer")
public void createCustomer(@RequestBody @Valid CustomerRequestBody customerRequestBody) { ... }
```

`CustomerRequestBody` fields (`name`, `email`) are also not annotated with `@NotBlank`, `@Email`, etc., even though the `Customer` entity has those constraints. Validation should happen at the boundary (controller/DTO), not only at the persistence layer.

---

### 3.2 `CustomerService` — `getCustomerByEmail` not annotated with `@Cacheable`

`getCustomer(Long id)` is cached, but `getCustomerByEmail(String email)` is not. This inconsistency can lead to cache misses and unexpected performance differences between the two lookup paths.

---

### 3.3 `CustomerService` — `@Transactional` missing on `getCustomerByEmail`

`getCustomerByEmail` reads from the database but is not marked `@Transactional(readOnly = true)`. All read-only repository calls should be wrapped in a read-only transaction for correctness and performance (allows the JPA provider to skip dirty-checking, and helps with read replicas).

---

### 3.4 `CustomerService` — Manual DTO construction repeated

The `CustomerDTO` constructor call is duplicated across multiple methods (`getCustomer`, `getCustomerByEmail`, `updateCustomer`, `getCustomers`). This violates the **DRY** principle and makes future field changes error-prone. A private helper method or a static factory on the DTO should be used:

```java
// ✅ Centralise mapping
private CustomerDTO toDto(Customer customer) {
    return new CustomerDTO(
        customer.getId(),
        customer.getName(),
        customer.getEmail(),
        customer.getCreatedAt(),
        customer.getUpdatedAt()
    );
}
```

---

### 3.5 `CustomerService` — `createCustomer` returns `void`; inconsistent API

`createCustomer` saves the customer and creates an outbox event, but returns `void`. The controller responds with `201 Created` but provides no location header or created resource body. Best practice is to return the created resource ID (or the full DTO) and optionally set a `Location` header.

---

### 3.6 `CustomerController` — `@PathVariable` typed as `String`, manually converted to `Long`

```java
// ❌ Unnecessary manual conversion
@GetMapping("/customer/{id}")
public CustomerDTO getCustomer(@PathVariable final String id) {
    return customerService.getCustomer(Long.valueOf(id));
}

// ✅ Let Spring do the conversion
@GetMapping("/customer/{id}")
public CustomerDTO getCustomer(@PathVariable final Long id) {
    return customerService.getCustomer(id);
}
```

Spring MVC handles the `String → Long` conversion automatically. The manual `Long.valueOf(id)` call adds noise and bypasses the framework's type-safe binding.

---

### 3.7 `CustomerController` — No global exception handler

`RuntimeException("Customer not found")` is thrown in the service but never mapped to an HTTP 404. Without a `@ControllerAdvice` / `@RestControllerAdvice`, the user will receive an unformatted 500 error. A custom exception class (e.g. `CustomerNotFoundException`) and a global handler should be introduced.

---

### 3.8 `TwoLevelCacheConfig` — `CacheManager` implemented as anonymous class

```java
// ❌ Anonymous class is hard to test, extend, and reason about
return new CacheManager() {
    @Override
    public Cache getCache(String name) { ... }
    ...
};
```

The anonymous `CacheManager` implementation in `TwoLevelCacheConfig` should be extracted to a proper named class (e.g. `TwoLevelCacheManager`). Anonymous classes are difficult to test in isolation, cannot be subclassed, and break readability when logic grows.

---

### 3.9 `TwoLevelCacheConfig` — `getCache()` creates a new `TwoLevelCache` on every call

```java
@Override
public Cache getCache(String name) {
    return new TwoLevelCache(
            caffeineManager.getCache(name),
            redisManager.getCache(name)
    );
}
```

Every call to `cacheManager.getCache("customerCache")` creates a new `TwoLevelCache` wrapper. Cache manager instances are expected to return the **same** `Cache` object for a given name. This can cause subtle bugs (e.g. L1 populated in one wrapper, not visible in another). The result should be cached in a `ConcurrentHashMap`.

---

### 3.10 `CaffeineCacheConfiguration` — `@EnableCaching` duplicated on 3 classes

`@EnableCaching` appears on `TwoLevelCacheConfig`, `CaffeineCacheConfiguration`, and `RedisCacheConfig`. This annotation is idempotent but should only appear once — typically on the `@SpringBootApplication` class or on a single dedicated configuration class.

---

### 3.11 `CompositeCacheManagerConfig` — Dead code in production

The entire body of `CompositeCacheManagerConfig` is commented out. This is dead code and should be **removed** from the production codebase. If it is kept as a reference/alternative implementation, it belongs in a separate branch or a dedicated documentation file.

---

### 3.12 `CustomerRequestBody` — Should be a record

`CustomerRequestBody` is a simple data holder with a constructor. It should be a `record` (Java 16+), consistent with how `CustomerDTO` and `CustomerEvent` are already defined.

```java
// ✅ Clean
public record CustomerRequestBody(String name, String email) {}
```

---

### 3.13 `AuditListener` — Uses `LocalDateTime.now()` directly

`LocalDateTime.now()` uses the system clock, which makes the code non-deterministic in tests and across time zones. The `Clock` abstraction should be injected:

```java
// ✅ Better — inject Clock
LocalDateTime now = LocalDateTime.now(clock);
```

For JPA entity listeners (which cannot be Spring beans by default), this can be addressed with `@Configurable` or by passing a `Clock` bean via a static utility.

---

### 3.14 `Customer` entity — `@NotBlank` and `@Email` on entity fields only

Validation annotations on JPA entities rely on Hibernate Validator being triggered before `EntityManager.persist()`. This is an implicit contract and differs from explicit controller-level validation. The entity-level annotations should be treated as a **last resort** safety net, not the primary validation mechanism.

---

## 4. Module: `customer-audit-service`

### 4.1 `CustomerAuditLogService` — Business logic in a Kafka listener

The `listen()` method is the Kafka entry point **and** the place where business logic (deserialization, entity construction, repository save) happens. This violates the Single Responsibility Principle. The listener should delegate immediately to a dedicated service method:

```java
@KafkaListener(...)
public void listen(String message) {
    auditService.processEvent(message);
}
```

---

### 4.2 `CustomerAuditLogService` — Exception wrapping without context

```java
} catch (Exception e) {
    throw new RuntimeException("Failed to process Kafka message", e);
}
```

Catching `Exception` broadly and re-throwing as `RuntimeException` is poor practice. Specific exceptions (e.g. `JsonProcessingException`) should be caught separately. The current approach will trigger Kafka retry on **any** failure, including non-retryable deserialization issues (poison pill). A `DeadLetterPublishingRecoverer` or custom `ErrorHandler` should handle deserialization failures differently from transient errors.

---

### 4.3 Kafka consumer hardcoded `groupId` duplicated

The `groupId` `"customer-group"` is hardcoded both in the `@KafkaListener` annotation and in `application.yml`. This creates a discrepancy risk. The annotation should use `${...}` property reference:

```java
@KafkaListener(topics = "customer_topic", groupId = "${spring.kafka.consumer.group-id}")
```

---

### 4.4 `CustomerAuditLog` — `@GeneratedValue(strategy = GenerationType.SEQUENCE)` without `@SequenceGenerator`

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)
private Long id;
```

Using `SEQUENCE` without a `@SequenceGenerator` will use Hibernate's default global sequence, which may conflict with other entities or databases. Either define an explicit `@SequenceGenerator` or use `IDENTITY` (as used in `customer-service`).

---

### 4.5 Inconsistent response format in `CustomerAuditLogController`

The controller returns `List<ConsumerAuditLogDto>` without pagination. For audit logs that may grow large, this can cause OOM and timeout issues. A paginated `Page<ConsumerAuditLogDto>` should be used.

---

### 4.6 Missing `@Repository` annotation on `CustomerAuditRepository`

`CustomerAuditRepository` does not have `@Repository` (unlike `CustomerRepository` in the customer-service). While Spring Data JPA auto-detects it, the annotation should be consistent across the codebase.

---

## 5. Legacy `src/` Module

The root `src/` directory contains a standalone Spring Boot application (`CacheApplication`) with its own customer management logic, Kafka producer/consumer configuration, and event classes. This module is **not declared as a Maven submodule** in the root `pom.xml` (only `customer-audit-service` and `customer-service` are declared), yet the root `pom.xml` contains a `<build>` section with `spring-boot-maven-plugin`, suggesting this was the original application.

**Issues:**

### 5.1 Duplicate / orphaned code

The `src/` module duplicates concepts that exist in `customer-service`:
- `CustomerService`, `CustomerController`, `CustomerRepository`, `Customer` entity, `CustomerDTO` — all largely duplicate their `customer-service` counterparts.
- This creates confusion about the authoritative implementation.

**Recommendation:** Either remove the `src/` module entirely if it has been superseded by the multi-module setup, or clearly document its purpose (e.g. as an experimental or educational module) and add it to the Maven build properly.

### 5.2 Kafka messages sent as plain formatted strings

```java
// ❌ Plain text Kafka messages — fragile and untyped
kafkaTemplate.send(CUSTOMER_TOPIC, String.valueOf(event.customerId()),
    String.format("CUSTOMER CREATED: ID: %s, name: %s,email: %s.", ...));
```

The legacy module sends Kafka messages as human-readable strings. The production `customer-service` correctly serializes events as JSON. The legacy approach is fragile, untyped, and not compatible with the Debezium outbox pattern used in production.

---

## 6. Build & Dependency Management

### 6.1 Root `pom.xml` declares dependencies for the parent POM

The root `pom.xml` (with `<packaging>pom</packaging>`) contains `<dependencies>` directly. In a multi-module Maven project, shared dependencies should be placed in `<dependencyManagement>` to manage versions without forcing all modules to inherit them. Putting them directly in `<dependencies>` forces **all** child modules to inherit all these dependencies (including `spring-boot-starter-kafka`, `spring-boot-starter-data-redis`, `caffeine`, etc.), even if a particular module does not need them.

```xml
<!-- ✅ Use dependencyManagement -->
<dependencyManagement>
    <dependencies>
        <dependency>...</dependency>
    </dependencies>
</dependencyManagement>
```

### 6.2 `spring-boot-h2console` artifact ID is non-standard

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>
```

The correct artifact ID is `spring-boot-devtools` (which includes H2 console support) or simply add the H2 dependency with `scope=runtime` alongside enabling H2 console in `application.yaml`. The artifact `spring-boot-h2console` does not exist in the standard Spring Boot BOM and will fail to resolve.

### 6.3 `spring-boot-starter-web` and `spring-boot-starter-webmvc` both declared

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
...
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

`spring-boot-starter-web` already includes Spring MVC. Adding `spring-boot-starter-webmvc` separately is redundant.

### 6.4 Lombok `scope` set to `compile`

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.42</version>
    <scope>compile</scope>
</dependency>
```

Lombok should use `<scope>provided</scope>` (or the default `compile` scope is sometimes acceptable, but `provided` is semantically more correct since Lombok is an annotation processor only needed at compile time, not at runtime). Also, the version should be managed via the Spring Boot BOM (remove the explicit `<version>` tag).

### 6.5 `spring-boot-maven-plugin` in parent POM may cause issues

The `spring-boot-maven-plugin` in the parent POM (with `<packaging>pom</packaging>`) will be inherited by all submodules and will attempt to repackage their JARs as executable Spring Boot fat JARs. This is usually fine, but the plugin configuration should be moved to each child module that actually produces an executable JAR.

---

## 7. Configuration & Infrastructure

### 7.1 Plain-text credentials in `application-local.yaml`

```yaml
# customer-service/src/main/resources/application-local.yaml
datasource:
  username: user
  password: password

# customer-audit-service/src/main/resources/application-local.yaml
datasource:
  username: postgres
  password: postgres
```

Even for local development, committing credentials to source control is a bad habit. A `.env` file (gitignored) with environment variable references (`${DB_USERNAME}`, `${DB_PASSWORD}`) is preferred.

### 7.2 `show-sql: true` and `format_sql: true` in base `application.yaml`

SQL logging should not be enabled by default in the base profile — it adds noise and can expose sensitive data in logs. These settings belong in the local/dev profile only.

### 7.3 `ddl-auto: update` in production-facing configuration

Both `application.yaml` files (base profiles) set `hibernate.ddl-auto=update`. `update` is dangerous in production as it can silently alter the schema. This should be `validate` (or `none`) in the base/production profile and `create-drop` or `update` only in local/test profiles.

### 7.4 `kafka.bootstrap-servers: localhost:9092` hardcoded in base `application.yml` (audit-service)

```yaml
# application.yml (base profile)
kafka:
  bootstrap-servers: localhost:9092
```

A localhost address in the base profile will be wrong in all non-local environments. This should be parameterized with a default:

```yaml
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### 7.5 Inconsistent YAML file extension

The `customer-service` uses `.yaml` extensions, while `customer-audit-service` uses `.yml` for its base configuration (`application.yml`) and `.yaml` for profiles. Be consistent — prefer `.yaml` throughout.

### 7.6 `server.port` inside `spring:` block in `application.yml` (audit-service)

```yaml
spring:
  ...
  server:
    port: 8081
```

`server.port` is a top-level Spring Boot property and should not be nested inside `spring:`. This configuration is currently being ignored.

```yaml
# ✅ Correct placement
server:
  port: 8081
```

---

## 8. Testing

### 8.1 `CompositeCacheTest` — Empty test class with all tests commented out

`CompositeCacheTest` contains only commented-out test code. This is dead weight in the test suite and should either be implemented or removed.

### 8.2 Test class naming is misleading

- `CustomerServiceCacheIntegrationTestConfig` — the suffix `Config` suggests it's a configuration class, not a test. Rename to `CustomerServiceCacheIntegrationTest`.
- `CustomerCacheUpdateIntegrationTestConfig` → `CustomerCacheUpdateIntegrationTest`
- `RedisIntegrationTestConfig` → `RedisIntegrationTestBase` or `AbstractRedisIntegrationTest`

### 8.3 `CustomerOptimisticLockTest` — Self-documented inconsistency

The test contains inline comments acknowledging that the first test (`shouldThrowOptimisticLockException`) is not a reliable test because the `CountDownLatch` does not synchronize the reads, only the start signal. Tests should be deterministic. The unreliable test should either be fixed (as the CyclicBarrier variants do) or removed.

### 8.4 No negative-path tests

The test suite covers happy-path scenarios. There are no tests for:
- Customer not found (404 scenario)
- Duplicate email constraint violation
- Invalid input (missing name, invalid email)
- Kafka deserialization failure handling

### 8.5 `@SpringBootTest` on `RedisIntegrationTestConfig` (base class)

`RedisIntegrationTestConfig` is an abstract base class annotated with `@SpringBootTest`. The annotation will be inherited by all subclasses, which is intentional, but `@SpringBootTest` on an abstract class that is not itself a test can confuse IDEs and CI tools. The annotation should be on the concrete test classes only.

---

## 9. Clean Code & General Observations

### 9.1 Inconsistent code formatting

- Some files have spaces after class-opening braces; others do not.
- Spacing around `=`, method calls, and blocks is inconsistent (compare `CustomerEvent event=new CustomerEvent(...)` vs. standard Java spacing).
- A project-wide formatter (e.g. Google Java Format or the default IntelliJ scheme) should be applied and enforced via CI.

### 9.2 Missing Lombok where appropriate, used inconsistently

- `CustomerService` uses a manual constructor instead of `@RequiredArgsConstructor` (compare to `CustomerController` which uses it).
- `CustomerAuditLogService` uses a manual constructor.
- Choose a consistent style — either always use Lombok constructors for Spring components or always write them manually.

### 9.3 `@Slf4j` only on `CustomerAuditLogService`

Logging is only present in `CustomerAuditLogService`. Critical operations in `CustomerService` (create, update, cache eviction) have no logging whatsoever, making production troubleshooting very difficult.

### 9.4 `CustomerUpdateCacheListener` — `Objects.requireNonNull` used as inline null-check

```java
Objects.requireNonNull(cacheManager.getCache("customerCache")).evict(event.customerId());
```

`cacheManager.getCache("customerCache")` will return `null` only if the cache is not configured, which is a setup error. Using `Objects.requireNonNull` inline with `.evict()` chained on the same line is both hard to read and throws `NullPointerException` with no meaningful message. Extract to a variable with a meaningful null message:

```java
Cache cache = cacheManager.getCache("customerCache");
if (cache == null) {
    log.warn("Cache 'customerCache' not found; skipping eviction for id={}", event.customerId());
    return;
}
cache.evict(event.customerId());
```

### 9.5 Package naming mismatch between test and production code

Test classes are in `com.example.cache` package but the production code they test lives in `com.example.customer`. This makes test discovery tools and package-private access patterns confusing. Test classes should reside in the same package as the classes they test.

### 9.6 `AggregateType` enum has only one value

```java
public enum AggregateType {
    CUSTOMER
}
```

A single-value enum provides no benefit over a constant. If the outbox pattern is intended to support multiple aggregate types in the future, this is fine — but a comment explaining this intent would be helpful.

### 9.7 `CustomerEvent` and `CustomerEventDto` are structurally identical

`CustomerEvent` (customer-service) and `CustomerEventDto` (customer-audit-service) are identical records with the same fields. This duplication is acceptable in microservices (avoiding shared kernel), but it should be explicitly documented as an intentional design decision, as there is a risk the two evolve out of sync.

---

## 10. Summary Score Card

| Category                     | Rating  | Comment                                      |
|------------------------------|---------|----------------------------------------------|
| Architecture / Patterns      | ★★★★☆   | Good choices (Outbox, Two-Level Cache, CDC)  |
| Correctness                  | ★★☆☆☆   | Multiple runtime-breaking bugs present       |
| Code Quality / Clean Code    | ★★★☆☆   | Inconsistencies, duplication, dead code      |
| Error Handling               | ★★☆☆☆   | Broad exception catching, missing 404 mapping|
| Testing                      | ★★★☆☆   | Good integration tests; dead/unreliable tests|
| Configuration Management     | ★★☆☆☆   | Hardcoded credentials, broken YAML, ddl=update|
| Build / Dependency Mgmt      | ★★☆☆☆   | Wrong artifact IDs, misused parent POM       |

---

## Priority Action Items

1. **[BLOCKER]** Fix wrong Jackson import (`tools.jackson` → `com.fasterxml.jackson`).
2. **[BLOCKER]** Fix `OutboxRepository` ID type (`UUID` → `Long`).
3. **[BLOCKER]** Fix broken YAML `enabled: true 8080` in `application-k8s.yaml`.
4. **[HIGH]** Inject `ObjectMapper` as a Spring bean instead of instantiating inline.
5. **[HIGH]** Add `@Valid` to controller request bodies and annotate `CustomerRequestBody` fields.
6. **[HIGH]** Add `@RestControllerAdvice` with custom exception classes (`CustomerNotFoundException` → 404).
7. **[HIGH]** Move `<dependencies>` to `<dependencyManagement>` in parent POM; fix non-existent `spring-boot-h2console` artifact.
8. **[HIGH]** Remove or fix the broken YAML `server.port` nesting in the audit-service.
9. **[MEDIUM]** Extract `TwoLevelCacheManager` from anonymous class; cache instances in `ConcurrentHashMap`.
10. **[MEDIUM]** Remove commented-out dead code (`CompositeCacheManagerConfig`, `CompositeCacheTest`).
11. **[MEDIUM]** Fix `@Transactional` + `@KafkaListener` misuse in audit service.
12. **[MEDIUM]** Change `ddl-auto: update` to `validate` in base/production profiles.
13. **[LOW]** Apply consistent code formatting project-wide.
14. **[LOW]** Add structured logging (`@Slf4j`) to `CustomerService`.
15. **[LOW]** Rename test classes to drop misleading `Config` suffix.
