This is a robust, high-performance Spring Boot microservice that acts as the central API gateway and guardrail system. It demonstrates handling concurrent requests, managing distributed state using Redis, and implementing event-driven scheduling.

## Tech Stack
- Java 17
- Spring Boot 3.2.x
- PostgreSQL
- Redis (Spring Data Redis)
- Docker & Docker Compose

## Getting Started

1. **Start the Infrastructure**
   ```bash
   docker-compose up -d
   ```
   This will start a local PostgreSQL instance on port `5432` and a Redis instance on port `6379`.

2. **Run the Application**
   You can run the application using Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or run the `CoreApiApplication` class from your IDE.

3. **Database Setup**
   The application is configured to automatically update the schema (`spring.jpa.hibernate.ddl-auto=update`).
   You can pre-populate `users` and `bots` tables manually or via a database script.

4. **Testing**
   A Postman collection `postman_collection.json` is provided in the repository root to easily test the endpoints.

## Guardrails & Thread Safety Approach

To prevent AI compute runaway and handle high concurrency (e.g., the "Race Conditions" Spam Test), this application relies entirely on **Redis Atomic Operations** rather than Java-level locking or standard database constraints. Redis is single-threaded and guarantees that its basic commands are atomic.

### 1. Virality Score (Real-time Calculation)
- We use the Redis `INCR` (or `INCRBY`) command to increment the virality score (`post:{id}:virality_score`). 
- This operation is completely atomic. If 200 bots interact at the exact same millisecond, Redis queues these operations and executes them sequentially, guaranteeing the score is accurate without race conditions.

### 2. Horizontal Cap (Bot Replies Limit)
- **Goal:** Max 100 bot replies per post.
- **Approach:** Before creating a comment, the API executes `INCR` on `post:{id}:bot_count`.
- `INCR` returns the new count atomically. If the returned count is `> 100`, the API immediately rejects the request with a `429 Too Many Requests`. 
- By checking the result of `INCR`, we avoid the "Check-Then-Act" race condition that would happen if we used `GET` followed by `SET` or a database query.

### 3. Vertical Cap (Thread Depth)
- **Goal:** Reject comment threads deeper than 20 levels.
- **Approach:** This is calculated during the comment creation based on the parent comment's depth level. Because comments are immutable once created, checking depth doesn't inherently suffer from race conditions in the same way distributed counters do. If `depth > 20`, the request is rejected with `400 Bad Request`.

### 4. Cooldown Cap
- **Goal:** Bot cannot interact with a specific human more than once per 10 minutes.
- **Approach:** We use the Redis `SETNX` (Set if Not eXists) command via `opsForValue().setIfAbsent()` on the key `cooldown:bot_{id}:human_{id}` with a 10-minute TTL.
- `SETNX` atomically sets the key if it doesn't exist and returns `true`. If the key already exists, it returns `false`. This effectively implements a distributed lock. If `false` is returned, the API blocks the interaction.

## The Notification Engine (Smart Batching)

- **Redis Throttler:** When a bot interacts with a human's post, we check for a 15-minute cooldown key. If it exists, we push the notification to a Redis List (`RPUSH`). If it doesn't, we send it immediately and set the cooldown.
- **CRON Sweeper:** A Spring `@Scheduled` task runs every 5 minutes. It scans for pending notifications, pops them from the list, logs a summarized message, and clears the list. This ensures users are not spammed with notifications.

## Architecture & Statelessness
The Spring Boot application remains completely stateless. All counters, cooldowns, and pending notifications are stored exclusively in Redis, satisfying the statelessness requirement. The PostgreSQL database acts as the source of truth for the actual content, but Redis acts as the gatekeeper.
