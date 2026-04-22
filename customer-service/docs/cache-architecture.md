# Cache Architecture Notes

## Why `CompositeCacheManager` is NOT a true L1 → L2 cache

In Spring applications it is common to combine two caches:

* **L1 (local cache)** – usually in-memory (e.g. Caffeine)
* **L2 (distributed cache)** – usually Redis

A common misunderstanding is that `CompositeCacheManager` creates a hierarchical cache where Spring first checks **Caffeine**, then **Redis**, and finally the **database**.

However, this is **not how `CompositeCacheManager` works**.

---

# How `CompositeCacheManager` actually works

`CompositeCacheManager` simply holds a **list of cache managers** and looks for a cache with a given name.

Example configuration:

```
CompositeCacheManager
 ├── CaffeineCacheManager
 └── RedisCacheManager
```

When Spring asks for cache `"users"`:

1. `CompositeCacheManager` checks the first cache manager
2. If that manager contains a cache with this name → it returns it
3. Other cache managers are **never used**

So the execution looks like:

```
Spring -> CompositeCacheManager
           -> first cache manager with cache name
```

This means:

* Only **one cache implementation is used**
* There is **no fallback to Redis if Caffeine misses**
* There is **no synchronization between caches**

---

# What a real L1 → L2 cache should do

A proper multi-level cache should work like this:

## Read flow

```
Request
   ↓
Caffeine (L1)
   ↓ miss
Redis (L2)
   ↓ miss
Database
```

If Redis returns data:

```
Redis hit
   ↓
populate Caffeine
```

This ensures:

* fast local reads
* shared distributed cache
* minimal database load

---

## Write flow

When data changes:

```
Database update
      ↓
Update Redis (L2)
      ↓
Invalidate or update Caffeine (L1)
```

Why?

Because **Caffeine caches are local to each application instance** and could otherwise return **stale data**.

---

# Why Spring does not support this automatically

Spring Cache abstraction is intentionally simple.

It assumes **one cache per cache name** and does not manage relationships between multiple cache layers.

Because of this:

* Spring **does not coordinate cache levels**
* Spring **does not populate L1 from L2**
* Spring **does not invalidate L1 automatically when L2 changes**

To build a real multi-level cache you must:

* implement a **custom Cache implementation**
* or use a **library that supports multi-level caching**
* or manually synchronize caches

---

# Recommended architecture

Typical production architecture:

```
L1 Cache: Caffeine (per application instance)
L2 Cache: Redis (shared across instances)
```

Read:

```
Caffeine -> Redis -> Database
```

Write:

```
Database -> Redis -> invalidate/update Caffeine
```

Benefits:

* very fast reads
* distributed consistency
* reduced database load

---

# Key takeaway

`CompositeCacheManager` is **not a hierarchical cache**.

It is only a **fallback mechanism for cache manager lookup**, not a system that coordinates multiple cache levels.

Using it expecting L1 → L2 behavior will lead to **incorrect caching assumptions**.

---

# Useful keywords for further research

* multi-level caching
* cache hierarchy
* L1 / L2 cache architecture
* cache invalidation strategies
* Spring custom Cache implementation

---
