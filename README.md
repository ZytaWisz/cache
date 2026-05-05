# Customer Platform — Caching, Outbox & Event Sourcing Demo

A Spring Boot multi-module project that demonstrates several production-grade backend patterns:

- **Two-Level Caching** (Caffeine L1 + Redis L2) with a custom `TwoLevelCacheManager`
- **Transactional Outbox Pattern** with Debezium CDC to guarantee at-least-once Kafka delivery
- **Event-driven Audit Log** via a dedicated `customer-audit-service` Kafka consumer
- **Optimistic Locking** with JPA `@Version` for concurrent write protection
- **Kubernetes-ready** deployment with ConfigMaps, Secrets, and health probes

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Modules](#modules)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Running Locally (Docker Compose)](#running-locally-docker-compose)
- [Running in Kubernetes](#running-in-kubernetes)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [How the Caching Works](#how-the-caching-works)
- [How the Outbox Pattern Works](#how-the-outbox-pattern-works)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                           Client / HTTP                              │
└─────────────────────────────┬────────────────────────────────────────┘
                              │ REST
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      customer-service  (:8080)                      │
│                                                                     │
│  ┌──────────────┐   ┌─────────────────┐   ┌──────────────────────┐ │
│  │ Controller   │──▶│  CustomerService │──▶│ TwoLevelCacheManager │ │
│  └──────────────┘   └────────┬────────┘   │  (Caffeine + Redis)  │ │
│                              │             └──────────────────────┘ │
│                              │ @Transactional                       │
│                     ┌────────▼────────┐                             │
│                     │  CustomerRepo   │                             │
│                     │  OutboxRepo     │──┐  MySQL                   │
│                     └─────────────────┘  │  (customer + outbox_evt) │
└──────────────────────────────────────────┼─────────────────────────┘
                                           │
                              ┌────────────▼──────────┐
                              │  Debezium (Kafka       │
                              │  Connect + CDC)        │
                              └────────────┬──────────┘
                                           │  customer_topic
                              ┌────────────▼──────────┐
                              │        Kafka           │
                              └────────────┬──────────┘
                                           │
┌──────────────────────────────────────────▼─────────────────────────┐
│                  customer-audit-service  (:8081)                    │
│                                                                     │
│  ┌─────────────────┐   ┌───────────────────────────────────────┐   │
│  │  @KafkaListener │──▶│  CustomerAuditLogService              │   │
│  └─────────────────┘   └───────────────┬───────────────────────┘   │
│                                        │                            │
│                              ┌─────────▼──────────┐                │
│                              │ CustomerAuditRepo   │──  PostgreSQL  │
│                              └────────────────────┘   (audit_log)  │
└────────────────────────────────────────────────────────────────────┘
```

---

## Modules

| Module                   | Description                                                                                |
|--------------------------|--------------------------------------------------------------------------------------------|
| `customer-service`       | REST API for managing customers. Writes to MySQL, caches in Caffeine+Redis, publishes events via Outbox Pattern |
| `customer-audit-service` | Kafka consumer that reads customer events and persists an immutable audit log in PostgreSQL |

> **Note:** The root `src/` directory contains an earlier prototype / exploratory version of the application. It is not part of the production multi-module build and is kept for reference only.

---

## Technology Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 21                             |
| Framework          | Spring Boot 4.x                     |
| Persistence        | Spring Data JPA + Hibernate         |
| DB (customer)      | MySQL 8                             |
| DB (audit)         | PostgreSQL 15                       |
| Cache L1           | Caffeine                            |
| Cache L2           | Redis 7                             |
| Messaging          | Apache Kafka (KRaft mode)           |
| CDC                | Debezium MySQL Connector            |
| Containers         | Docker / Docker Compose             |
| Orchestration      | Kubernetes                          |
| Build              | Maven (multi-module)                |
| Utilities          | Lombok, Jackson                     |
| Testing            | JUnit 5, Testcontainers, Mockito    |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- `kubectl` (for Kubernetes deployment)

---

## Running Locally (Docker Compose)

### 1. Start infrastructure (Kafka + Kafka UI)

The root `docker-compose.yaml` starts Kafka (KRaft mode) and Kafka UI:

```bash
docker compose up -d
```

Kafka UI is available at: [http://localhost:8090](http://localhost:8090)

### 2. Start MySQL for customer-service

```bash
# Start MySQL (example using Docker directly)
docker run -d \
  --name customer-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=customer_db \
  -e MYSQL_USER=user \
  -e MYSQL_PASSWORD=password \
  -p 3306:3306 \
  mysql:8
```

### 3. Start PostgreSQL for customer-audit-service

```bash
docker run -d \
  --name audit-postgres \
  -e POSTGRES_DB=audit \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  postgres:15
```

### 4. Start Redis

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 5. Run the services

```bash
# customer-service
cd customer-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# customer-audit-service (in a separate terminal)
cd customer-audit-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Running in Kubernetes

All Kubernetes manifests are located in the `k8s/` directory.

### Namespace

```bash
kubectl apply -f k8s/namespace.yaml
```

### Infrastructure

```bash
# Redis
kubectl apply -f k8s/redis/

# Kafka + Kafka Connect
kubectl apply -f k8s/kafka/

# Kafka-ui
kubectl apply -f k8s/kafka-ui/

# MySQL (for customer-service)
kubectl apply -f k8s/customer-service/mysql-deployment.yaml

# PostgreSQL (for customer-audit-service)
kubectl apply -f k8s/customer-audit-service/postgres-deployment.yaml
```

### Applications

```bash
# customer-service
kubectl apply -f k8s/customer-service/

# customer-audit-service
kubectl apply -f k8s/customer-audit-service/
```

### Register Debezium Connector

```bash
# Apply the Debezium connector config and registration job
kubectl apply -f k8s/kafka/debezium-connector-config.yaml
kubectl apply -f k8s/kafka/register-customer-outbox-connector-job.yaml
```

### Secrets

Before applying the services, populate the secrets with your Base64-encoded values:

```bash
kubectl apply -f k8s/customer-service/customer-service-secret.yaml
kubectl apply -f k8s/customer-audit-service/customer-audit-service-secret.yaml
kubectl apply -f k8s/kafka/debezium-connector-secret.yaml
```

---

## Configuration

### Spring Profiles

| Profile | Used for                        | Activated via                        |
|---------|---------------------------------|--------------------------------------|
| `local` | Local development               | Default (set in `application.yaml`)  |
| `k8s`   | Kubernetes deployment           | `SPRING_PROFILES_ACTIVE=k8s` env var |

### Key Environment Variables (k8s profile)

#### customer-service

| Variable               | Description                 |
|------------------------|-----------------------------|
| `DB_URL`               | MySQL JDBC URL              |
| `DB_USERNAME`          | MySQL username              |
| `DB_PASSWORD`          | MySQL password              |
| `REDIS_HOST`           | Redis hostname              |
| `REDIS_PORT`           | Redis port                  |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile     |

#### customer-audit-service

| Variable               | Description                 |
|------------------------|-----------------------------|
| `DB_URL`               | PostgreSQL JDBC URL         |
| `DB_USERNAME`          | PostgreSQL username         |
| `DB_PASSWORD`          | PostgreSQL password         |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile     |

### Cache Configuration

| Cache      | L1 TTL    | L2 TTL    | Max Size (L1) |
|------------|-----------|-----------|----------------|
| `customerCache` | 1 min | 5 min  | 2 000 entries  |

---

## API Reference

### customer-service — Base URL: `http://localhost:8080`

#### Create Customer

```http
POST /api/customer
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john.doe@example.com"
}
```

Response: `201 Created`

---

#### Get Customer by ID

```http
GET /api/customer/{id}
```

Response: `200 OK`
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "createdAt": "2026-05-04T10:00:00",
  "updatedAt": null
}
```

---

#### Update Customer

```http
PUT /api/customer/{id}
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane.doe@example.com"
}
```

Response: `200 OK` with updated `CustomerDTO`

---

#### Get All Customers

```http
GET /api/customers
```

Response: `200 OK` — array of `CustomerDTO`

---

### customer-audit-service — Base URL: `http://localhost:8081`

#### Get All Audit Logs

```http
GET /api/customer/logs
```

Response: `200 OK`
```json
[
  {
    "id": 1,
    "customerId": 1,
    "customerEventType": "CREATED",
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerCreatedAt": "2026-05-04T10:00:00",
    "customerUpdatedAt": null,
    "eventConsumedAt": "2026-05-04T10:00:01"
  }
]
```

---

### Health Endpoints

```http
GET /actuator/health         # Liveness + readiness probes
GET /actuator/health/liveness
GET /actuator/health/readiness
```

---

## How the Caching Works

The `customer-service` implements a **two-level cache strategy**:

```
Read path:
  1. Check L1 (Caffeine — in-process, very fast, ~1 min TTL)
  2. On L1 miss → check L2 (Redis — distributed, ~5 min TTL)
  3. On L2 miss → populate both L1 and L2 from the database
  4. On L2 hit → populate L1 ("backfill")

Write/Update path:
  1. Update entity in DB (within transaction)
  2. After @Transactional COMMIT → publish Spring ApplicationEvent
  3. CustomerUpdateCacheListener (AFTER_COMMIT phase) evicts the key from both L1 and L2
  4. Next read will reload from DB and re-populate the cache
```

The `TwoLevelCache` class implements `org.springframework.cache.Cache` and wraps both Caffeine and Redis caches, making the two-level behaviour transparent to `@Cacheable` annotations.

---

## How the Outbox Pattern Works

The Outbox Pattern guarantees that database writes and Kafka event publishing are **always consistent**, even in the face of crashes:

```
1. CustomerService.createCustomer() / updateCustomer()
   └─ Saves Customer entity to MySQL
   └─ Saves OutboxEvent (with JSON payload) to MySQL
      → Both writes happen in the SAME @Transactional boundary

2. Debezium MySQL Connector (running in Kafka Connect)
   └─ Watches the outbox_event table via MySQL binlog (CDC)
   └─ Applies EventRouter SMT to extract the payload
   └─ Publishes to Kafka topic: customer_topic

3. customer-audit-service
   └─ @KafkaListener consumes from customer_topic
   └─ Deserializes JSON payload to CustomerEventDto
   └─ Persists CustomerAuditLog to PostgreSQL
```

This pattern avoids the dual-write problem: if the application crashes after saving the customer but before sending to Kafka, Debezium will pick up the outbox event on restart and publish it.

---

## Running Tests

### Unit & Integration Tests

```bash
# Run all tests (requires Docker for Testcontainers)
mvn test

# Run tests for a specific module
mvn test -pl customer-service
mvn test -pl customer-audit-service
```

### Test Coverage

| Test Class                              | What it covers                                         |
|-----------------------------------------|--------------------------------------------------------|
| `CustomerServiceCacheIntegrationTest`   | L1 → L2 → DB cache hit ordering with real Redis        |
| `CustomerCacheUpdateIntegrationTest`    | Cache eviction on customer update                      |
| `CustomerOptimisticLockTest`            | JPA `@Version` optimistic locking under concurrency    |

Tests use **Testcontainers** to spin up real Redis and MySQL instances — no mocking of infrastructure.

---

## Project Structure

```
cache/                                  ← Root Maven project (pom packaging)
├── docker-compose.yaml                 ← Local Kafka + Kafka UI
├── pom.xml                             ← Parent POM (dependency management)
│
├── customer-service/                   ← Spring Boot service: customer CRUD + caching
│   ├── src/main/java/com/example/customer/
│   │   ├── controller/                 ← REST controllers
│   │   ├── service/                    ← Business logic
│   │   ├── repository/                 ← Spring Data JPA repositories
│   │   ├── entity/                     ← JPA entities (Customer, OutboxEvent)
│   │   ├── dto/                        ← Data Transfer Objects
│   │   ├── events/                     ← Spring ApplicationEvents
│   │   ├── listener/                   ← Event listeners (cache eviction)
│   │   ├── model/                      ← Marker interfaces (Creatable, Updatable)
│   │   ├── enums/                      ← Enumerations
│   │   └── config/
│   │       ├── twolevel/               ← Two-level cache configuration
│   │       └── composite/              ← (Reference only) CompositeCacheManager
│   ├── src/main/resources/
│   │   ├── application.yaml            ← Base configuration
│   │   ├── application-local.yaml      ← Local dev overrides
│   │   └── application-k8s.yaml        ← Kubernetes overrides
│   ├── src/test/                       ← Integration tests (Testcontainers)
│   └── docs/
│       ├── cache-architecture.md       ← Cache design notes
│       └── kubernetes-deployment-guide.md
│
├── customer-audit-service/             ← Spring Boot service: Kafka consumer + audit log
│   ├── src/main/java/com/example/audit/
│   │   ├── controller/                 ← REST controllers
│   │   ├── service/                    ← Kafka listener + business logic
│   │   ├── repository/                 ← Spring Data JPA repositories
│   │   ├── entity/                     ← JPA entities (CustomerAuditLog)
│   │   ├── dto/                        ← Data Transfer Objects
│   │   ├── listener/                   ← JPA entity listener (AuditListener)
│   │   ├── model/                      ← Marker interfaces
│   │   ├── enums/                      ← Enumerations
│   │   └── config/                     ← Kafka error handling configuration
│   └── src/main/resources/
│       ├── application.yml             ← Base configuration
│       ├── application-local.yaml      ← Local dev overrides
│       └── application-k8s.yaml        ← Kubernetes overrides
│
├── k8s/                                ← Kubernetes manifests
│   ├── namespace.yaml
│   ├── customer-service/               ← Deployment, ConfigMap, Secret, MySQL
│   ├── customer-audit-service/         ← Deployment, ConfigMap, Secret, PostgreSQL
│   ├── kafka/                          ← Kafka, Kafka Connect, Debezium connector
│   ├── kafka-ui/                       ← Kafka UI deployment
│   └── redis/                          ← Redis deployment
│
└── src/                                ← Legacy/prototype module (not in Maven build)
    └── main/java/com/example/cache/    ← Earlier version with manual Kafka config
```
