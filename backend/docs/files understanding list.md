<br />

---

🧭 How to Use This Roadmap
-----------------------------

-   [ ] `pom.xml` (`backend/pom.xml`)
    -   **Responsibility:** Maven dependencies, compiler plugins, and version definitions.

    -   **What to understand:** Java 25 compatibility, Spring Boot 4.1.1 starters (`webmvc`, `websocket`, `data-jpa`, `data-redis`, `security`), JJWT 0.13 dependencies, and Liquibase plugin.

    -   **Connections:** Yeh define karta hai ki poore project me kaun si libraries available hain.

    -   **Why now:** Kisi bhi project ko samajhne ka pehla step uski dependency tree dekhna hota hai.

-   [ ] `docker-compose.yaml` (`backend/docker-compose.yaml`)
    -   **Responsibility:** Local development environment container definitions.

    -   **What to understand:** PostgreSQL 16 container (`sandesh-postgres` on port 5432 with health check) and Redis 7 container (`sandesh-redis` on port 6379 with `redis-cli ping`).

    -   **Connections:** Inhi do services se backend connect hota hai runtime par.

    -   **Why now:** Backend ko run karne ke liye infrastructure pehle ready hona chahiye.

-   [ ] `.env.example` (`backend/.env.example`)
    -   **Responsibility:** Template for environment variables and secrets.

    -   **What to understand:** Database credentials placeholders, JWT signing key variable (`SANDESH_JWT_SECRET`), pool sizes, and port defaults.

    -   **Connections:** Spring Boot `application.yaml` inhi environment variables ko interpolate karta hai.

    -   **Why now:** Production safety rules samajhna zaroori hai (never hardcode plain text secrets).

-   [ ] `db.changelog-master.yaml` (`backend/src/main/resources/db/changelog/db.changelog-master.yaml`)
    -   **Responsibility:** Master Liquibase migration registry.

    -   **What to understand:** Kaise sequentially 11 individual SQL migration files register kiye gaye hain.

    -   **Connections:** Database table creation order yahan se decide hota hai.

    -   **Why now:** JPA entities dekhne se pehle database tables ka schema dekhna foundational hai.

-   [ ] `001-create-users-schema.sql` (`backend/src/main/resources/db/changelog/001-create-users-schema.sql`)
    -   **Responsibility:** Base `users` table definition.

    -   **What to understand:** `id UUID PRIMARY KEY`, `email VARCHAR(120) UNIQUE`, `password_hash`, and audit timestamps.

    -   **Connections:** `UserAccount.java` entity is table se map hoti hai.

    -   **Why now:** User table sabhi entities ka parent reference hai.

-   [ ] `002-alter-users-schema.sql` (`backend/src/main/resources/db/changelog/002-alter-users-schema.sql`)
    -   **Responsibility:** Schema modification for user flags.

    -   **What to understand:** Adds `enabled BOOLEAN NOT NULL DEFAULT TRUE`.

    -   **Connections:** Used by Spring Security to disable compromised accounts.

    -   **Why now:** Second step in users schema evolution.

-   [ ] `003-create-user-blocks-schema.sql` (`backend/src/main/resources/db/changelog/003-create-user-blocks-schema.sql`)
    -   **Responsibility:** User blocking relational table.

    -   **What to understand:** Composite unique constraint on `(blocker_id, blocked_id)` and foreign keys to `users`.

    -   **Connections:** Used by `UserBlock.java` entity for user privacy.

    -   **Why now:** Security constraint before conversations can be created.

-   [ ] `004-create-conversations-schema.sql` (`backend/src/main/resources/db/changelog/004-create-conversations-schema.sql`)
    -   **Responsibility:** Chat rooms table (`conversations`).

    -   **What to understand:** `id UUID PRIMARY KEY`, `type VARCHAR(16)` (DIRECT/GROUP), `title`, `creator_id`.

    -   **Connections:** Maps to `Conversation.java`.

    -   **Why now:** Container for messages and members.

-   [ ] `005-create-conversation-members-schema.sql` (`backend/src/main/resources/db/changelog/005-create-conversation-members-schema.sql`)
    -   **Responsibility:** Many-to-Many membership table with attributes.

    -   **What to understand:** Composite primary key `(conversation_id, user_id)`, `role VARCHAR(16)`, `hidden_after_sequence`, and `read_through_sequence_number`.

    -   **Connections:** Maps to `ConversationMember.java`.

    -   **Why now:** Controls who can read and post in a conversation.

-   [ ] `006-create-messages-schema.sql` (`backend/src/main/resources/db/changelog/006-create-messages-schema.sql`)
    -   **Responsibility:** Core messages table.

    -   **What to understand:**

        1.  Composite unique constraint `uq_messages_conversation_sequence (conversation_id, sequence_number)`.
        2.  Composite unique constraint `uq_messages_sender_client_message_id (sender_id, client_message_id)`.

    -   **Connections:** Enforces strictly monotonic sequence numbers and idempotency at the database engine level.

    -   **Why now:** The most critical table in the entire system.

-   [ ] `007-create-message-receipts-schema.sql` (`backend/src/main/resources/db/changelog/007-create-message-receipts-schema.sql`)
    -   **Responsibility:** Per-recipient delivery and read receipt ticks.

    -   **What to understand:** Composite primary key `(message_id, recipient_id)`, `state VARCHAR(16)` (DELIVERED, READ), and timestamps.

    -   **Connections:** Maps to `MessageReceipt.java`.

    -   **Why now:** Powers single tick, double tick, and blue tick indicators.

-   [ ] `008-create-outbox-events-schema.sql` (`backend/src/main/resources/db/changelog/008-create-outbox-events-schema.sql`)
    -   **Responsibility:** Transactional Outbox pattern event queue table.

    -   **What to understand:** `id UUID`, `aggregate_type`, `aggregate_id`, `event_type`, `payload TEXT`, `status VARCHAR(16)` (PENDING, PUBLISHED, FAILED), `retry_count`, and `next_retry_at`.

    -   **Connections:** Prevents distributed dual-write inconsistencies.

    -   **Why now:** Foundation of reliable real-time event streaming.

-   [ ] `009-create-processed-events-schema.sql` (`backend/src/main/resources/db/changelog/009-create-processed-events-schema.sql`)
    -   **Responsibility:** Consumer idempotency deduplication table.

    -   **What to understand:** `event_id UUID PRIMARY KEY` and `processed_at TIMESTAMP`.

    -   **Connections:** Ensures Redis Stream worker processes each outbox event exactly once.

    -   **Why now:** Guards against duplicate notifications and duplicate message fanouts.

-   [ ] `010-create-audit-events-schema.sql` (`backend/src/main/resources/db/changelog/010-create-audit-events-schema.sql`)
    -   **Responsibility:** Security and business compliance audit trail.

    -   **What to understand:** `actor_id`, `action`, `resource_type`, `resource_id`, `metadata`.

    -   **Connections:** Records administrative actions and user safety events.

    -   **Why now:** Essential for enterprise security and traceability.

-   [ ] `011-alter-device-tokens.sql` (`backend/src/main/resources/db/changelog/011-alter-device-tokens.sql`)
    -   **Responsibility:** FCM/APNS mobile push token storage table (`devices`).

    -   **What to understand:** `user_id`, `device_id`, `platform`, `push_token`, `notifications_enabled`.

    -   **Connections:** Used by `NotificationService` when recipient is offline.

    -   **Why now:** Prerequisite for offline mobile push notifications.

-   [ ] `application.yaml` (`backend/src/main/resources/application.yaml`)
    -   **Responsibility:** Production configuration definitions.

    -   **What to understand:**

        1.  HikariCP connection pool settings (`maximum-pool-size: 20`, `leak-detection-threshold: 2000`).
        2.  Redis standalone connection details and Lettuce pool.
        3.  Custom `sandesh.*` namespace properties (JWT secret, token TTL, outbox poll intervals, stream names).
        4.  STOMP allowed origins and consumer group names.

    -   **Connections:** Binds all infrastructure beans at startup.

    -   **Why now:** Shows how Spring Boot translates YAML properties into Java configurations.

Yeh document ek **step-by-step learning checklist** hai. Sandesh ke classes ko random order me ya alphabetic package order me padhne ke bajaye, is roadmap ko aise design kiya gaya hai ki **har class apne previous class ka logical next step** hai:
1. Hamesha **Phase 0** se start karo aur sequentially aage badho.
2. Har item ke checkbox `- [ ]` ko tick karo jab aap us file ko apne code editor (IntelliJ / VS Code) me open karke uska code walkthrough complete kar lo.
3. Har phase ke end me diye gaye **Self-Assessment & Concept Check** ke sawaal khud solve karne ki koshish karo bina code dekhe. Agar aap concept verbally explain kar sakte ho, tabhi agle phase par move karo!

---

## 🗺️ Roadmap Overview: The 13 Learning Milestones

```
Phase 0: Orientation, Build & Database Foundations
   ↓
Phase 1: Application Bootstrap & Web Exception Infrastructure
   ↓
Phase 2: Authentication, Cryptography & Security Gatekeeper
   ↓
Phase 3: User Directory & Relational Safety (Blocking Engine)
   ↓
Phase 4: Conversation Aggregate & Membership Management
   ↓
Phase 5: Core Messaging: Persistence, Monotonic Sequences & Idempotency
   ↓
Phase 6: Message Receipts & Monotonic State Machine (Ticks)
   ↓
Phase 7: Transactional Outbox Pattern & Background Publisher
   ↓
Phase 8: Redis Streams, Consumer Groups & The Delivery Worker
   ↓
Phase 9: WebSocket STOMP Gateway & Redis Pub/Sub Fanout
   ↓
Phase 10: Real-Time Presence (ZSET) & Typing Indicators
   ↓
Phase 11: Auxiliary Systems: Search, Devices, Push & Audit Trail
   ↓
Phase 12: End-to-End Application Verification, Manual Testing & System Design Defense
```

---

## Phase 0: Orientation, Build & Database Foundations

### Phase Objective
Is phase ke end tak aapko pata hoga ki project kaun se dependencies use kar raha hai, local PostgreSQL aur Redis containers kaise boot hote hain, environment variables kaise configure hote hain, aur database schema migrations ka sequential structure kya hai.

### Prerequisites
- Basic understanding of Maven (`pom.xml`), Docker containers, and Relational Databases (SQL).

### Ordered Checklist

-   Phase 0 completed.

-   Concept of Spring Boot `@SpringBootApplication`, Servlet Container lifecycle, and HTTP Status Codes.

### Self-Assessment & Concept Check
1. `006-create-messages-schema.sql` me `uq_messages_conversation_sequence` unique constraint kyu lagayi gayi hai?
2. Transactional outbox table (`outbox_events`) ka use karke hum distributed systems ki kaun si classic problem solve karte hain?

---

## Phase 1: Application Bootstrap & Web Exception Infrastructure

### Phase Objective
Is phase ke baad aapko samajh aayega ki Sandesh application kaise boot hoti hai, background `@Async` tasks kaise configure hote hain, aur error aane par Spring Web standard JSON error responses kaise generate karta hai.

### Prerequisites
-   [ ] `SandeshApplication.java` (`backend/src/main/java/com/aadi/sandesh/SandeshApplication.java`)
    -   **Responsibility:** Spring Boot main entry point.

    -   **What to understand:** `@SpringBootApplication`, `@EnableScheduling` (enables periodic outbox polling and token cleanup), and `main()` method.

    -   **Connections:** Application bootstrap root.

    -   **Why now:** Entry point to observe annotations and Spring application context creation.

-   [ ] `AsyncConfig.java` (`backend/src/main/java/com/aadi/sandesh/config/AsyncConfig.java`)
    -   **Responsibility:** Thread pool executor configuration for asynchronous tasks.

    -   **What to understand:** `@EnableAsync`, custom `ThreadPoolTaskExecutor` bean naming `taskExecutor`, core pool size, max pool size, queue capacity, and thread name prefix `sandesh-async-`.

    -   **Connections:** Powers background jobs (push notifications, audit logging) without blocking HTTP threads.

    -   **Why now:** Establishes non-blocking execution baseline.

-   [ ] `FieldViolation.java` (`backend/src/main/java/com/aadi/sandesh/dto/common/FieldViolation.java`)
    -   **Responsibility:** Record holding a single validation error (`field`, `message`).

    -   **What to understand:** Clean immutable record structure for field-level validation errors.

    -   **Connections:** Used inside `ApiError.java`.

    -   **Why now:** Foundational DTO for error responses.

-   [ ] `ApiError.java` (`backend/src/main/java/com/aadi/sandesh/dto/common/ApiError.java`)
    -   **Responsibility:** Standardized JSON error response model.

    -   **What to understand:** Attributes: `timestamp`, `status`, `code`, `message`, `path`, and `violations`.

    -   **Connections:** Serialized and returned by `GlobalExceptionHandler`.

    -   **Why now:** Consistent API contract for all failure cases.

-   [ ] Domain Exception Classes (`backend/src/main/java/com/aadi/sandesh/exception/`)
    -   [ ] `NotFoundException.java` — Thrown when resource (user, message, conversation) does not exist (HTTP 404).

    -   [ ] `UnauthorizedException.java` — Thrown when authentication is missing or credentials invalid (HTTP 401).

    -   [ ] `ForbiddenException.java` — Thrown when authenticated user is not authorized or not a conversation member (HTTP 403).

    -   [ ] `ConflictException.java` — Thrown on duplicate registration, direct chat with blocked user, or clientMessageId conflict (HTTP 409).

    -   [ ] `RateLimitExceededException.java` — Thrown when user sends messages faster than allowed rate (HTTP 429).

    -   **What to understand:** Why each exception inherits from `RuntimeException` and how specific HTTP status codes map to specific business failures.

    -   **Connections:** Thrown by service layer classes across the entire codebase.

    -   **Why now:** Exception classes must be understood before reading the services that throw them.

-   [ ] `GlobalExceptionHandler.java` (`backend/src/main/java/com/aadi/sandesh/exception/GlobalExceptionHandler.java`)
    -   **Responsibility:** `@RestControllerAdvice` intercepting all controller exceptions.

    -   **What to understand:** `@ExceptionHandler` methods for custom exceptions, Spring Security `AccessDeniedException`, validation `MethodArgumentNotValidException`, and fallback `Exception.class`.

    -   **Connections:** Intercepts exceptions thrown from any controller/service and converts them into `ResponseEntity<ApiError>`.

    -   **Why now:** Guarantees that clients always get structured JSON instead of raw stack traces.

### Ordered Checklist

-   Phase 1 completed.

-   Concept of HTTP Headers, Symmetric Encryption (HMAC-SHA256), and Stateless REST Security.

### Self-Assessment & Concept Check
1. Agar koi client request me invalid email format bhejta hai, to request kis exception handler tak jayegi aur response format kya hoga?
2. `UnauthorizedException` (401) aur `ForbiddenException` (403) ke beech ka exact difference kya hai?

---

## Phase 2: Authentication, Cryptography & Security Gatekeeper

### Phase Objective
Is phase ke end tak aap samjhenge ki user registration, password hashing (BCrypt), JWT token generation/parsing, security filter chains, aur current user identity extraction kaise kaam karte hain.

### Prerequisites
-   [ ] `UserAccount.java` (`backend/src/main/java/com/aadi/sandesh/domain/UserAccount.java`)
    -   **Responsibility:** JPA Entity for user credentials.

    -   **What to understand:** Fields (`id`, `displayName`, `email`, `passwordHash`, `enabled`), `@PreUpdate` updating `updatedAt`, and UUID primary key generation.

    -   **Connections:** Persisted in `users` table; manipulated by `AuthService`.

    -   **Why now:** Authentication entity prerequisite.

-   [ ] `UserAccountRepository.java` (`backend/src/main/java/com/aadi/sandesh/repository/UserAccountRepository.java`)
    -   **Responsibility:** Spring Data JPA interface for user queries.

    -   **What to understand:** `findByEmail(String email)`, `existsByEmail(String email)`, and native projection queries returning `UserResponse`.

    -   **Connections:** Data access layer for authentication and user lookups.

    -   **Why now:** Enables querying users by email during login.

-   [ ] `SecurityConfig.java` (`backend/src/main/java/com/aadi/sandesh/config/SecurityConfig.java`)
    -   **Responsibility:** Spring Security 6 filter chain configuration.

    -   **What to understand:**

        1.  Disables CSRF (stateless REST with JWT doesn't need CSRF tokens).
        2.  Sets `SessionCreationPolicy.STATELESS`.
        3.  Whitelists public paths: `/api/auth/**`, `/ws/**`, `/actuator/health`.
        4.  Secures all other endpoints with `authenticated()`.
        5.  Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
        6.  Configures `BCryptPasswordEncoder` bean.

    -   **Connections:** Ingress security gatekeeper for every single HTTP request.

    -   **Why now:** Must understand authorization rules before inspecting protected controllers.

-   [ ] `JwtService.java` (`backend/src/main/java/com/aadi/sandesh/security/JwtService.java`)
    -   **Responsibility:** JWT token minting, signing, and cryptographic verification.

    -   **What to understand:**

        1.  JJWT `Keys.hmacShaKeyFor()` using `SANDESH_JWT_SECRET`.
        2.  `generateAccessToken(userId, email)`: adds claims `sub=userId`, `email=email`, issue time, and expiration time ($now + TTL$).
        3.  `validateTokenAndGetClaims(token)`: verifies HMAC signature, checks expiration, and returns `Claims`.

    -   **Connections:** Used by `AuthService` on login, and `JwtAuthenticationFilter` on every request.

    -   **Why now:** Core cryptographic token engine.

-   [ ] `ChatPrincipal.java` (`backend/src/main/java/com/aadi/sandesh/security/ChatPrincipal.java`)
    -   **Responsibility:** Custom security principal object representing the authenticated user.

    -   **What to understand:** Implements `AuthenticatedPrincipal`, stores `UUID userId` and `String email`.

    -   **Connections:** Placed inside Spring's `SecurityContextHolder.getContext().getAuthentication()`.

    -   **Why now:** Bridges JWT claims into Spring Security's caller identity.

-   [ ] `CurrentUser.java` (`backend/src/main/java/com/aadi/sandesh/security/CurrentUser.java`)
    -   **Responsibility:** Helper utility class to extract `ChatPrincipal` cleanly from controllers.

    -   **What to understand:** `currentUser.id(authentication)` safely casts `principal` to `ChatPrincipal` and extracts UUID.

    -   **Connections:** Used in every protected REST controller method.

    -   **Why now:** Eliminates duplicate typecasting across controllers.

-   [ ] `IssuedToken.java` (`backend/src/main/java/com/aadi/sandesh/security/IssuedToken.java`)
    -   **Responsibility:** Immutable record holding `token`, `expiresAt`, and `userId`.

    -   **What to understand:** Clean return type for generated credentials.

    -   **Connections:** Produced by `JwtService` and packed into `AuthResponse`.

    -   **Why now:** Clean encapsulation of issued tokens.

-   [ ] `JwtAuthenticationFilter.java` (`backend/src/main/java/com/aadi/sandesh/security/JwtAuthenticationFilter.java`)
    -   **Responsibility:** `OncePerRequestFilter` inspecting every HTTP request.

    -   **What to understand:**

        1.  Reads `Authorization: Bearer <token>` header.
        2.  Validates token via `JwtService`.
        3.  Builds `ChatPrincipal` and creates `UsernamePasswordAuthenticationToken`.
        4.  Populates `SecurityContextHolder`.
        5.  Calls `filterChain.doFilter()`.

    -   **Connections:** Executes before controllers; populates the security context.

    -   **Why now:** Connects incoming HTTP headers to authenticated application state.

-   [ ] Auth DTOs (`backend/src/main/java/com/aadi/sandesh/dto/auth/`)
    -   [ ] `RegisterRequest.java` — Holds `displayName`, `email` (`@Email`), and `password` (`@NotBlank`, `@Size(min=8)`).

    -   [ ] `LoginRequest.java` — Holds `email` and `password`.

    -   [ ] `AuthResponse.java` — Returns `accessToken`, `tokenType: Bearer`, `expiresIn`, and user summary.

    -   **What to understand:** Bean validation annotations and payload contracts.

    -   **Connections:** Request/Response payloads for `AuthController`.

    -   **Why now:** Required before reading `AuthService`.

-   [ ] `AuthService.java` (`backend/src/main/java/com/aadi/sandesh/service/AuthService.java`)
    -   **Responsibility:** Authentication business logic orchestrator.

    -   **What to understand:**

        1.  `register()`: checks `existsByEmail()`, hashes password via BCrypt, saves `UserAccount`, records audit event, generates JWT.
        2.  `login()`: finds user by email, calls `passwordEncoder.matches()`, verifies `user.isEnabled()`, records audit, returns JWT.

    -   **Connections:** Uses `UserAccountRepository`, `PasswordEncoder`, `JwtService`, and `AuditService`.

    -   **Why now:** The brain of the authentication subsystem.

-   [ ] `AuthController.java` (`backend/src/main/java/com/aadi/sandesh/controller/AuthController.java`)
    -   **Responsibility:** REST endpoints for authentication.

    -   **What to understand:** `POST /api/auth/register` (returns 201 Created) and `POST /api/auth/login` (returns 200 OK).

    -   **Connections:** Client HTTP entry point for registration and login.

    -   **Why now:** Completes the authentication slice.

### Ordered Checklist

-   [ ] `UserResponse.java` (`backend/src/main/java/com/aadi/sandesh/dto/user/UserResponse.java`)
    -   **Responsibility:** Public profile representation of a user (`id`, `displayName`, `email`).

    -   **What to understand:** Sensitive fields like `passwordHash` are never exposed.

    -   **Connections:** Returned by user search and conversation member lists.

    -   **Why now:** Core DTO for public user representations.

-   [ ] `UserDirectoryService.java` (`backend/src/main/java/com/aadi/sandesh/service/UserDirectoryService.java`)
    -   **Responsibility:** User discovery and search business service.

    -   **What to understand:** `searchUsers(query, limit)` searching by displayName or email with case-insensitive matching.

    -   **Connections:** Injected into `UserController`.

    -   **Why now:** Allows clients to find people to chat with.

-   [ ] `UserController.java` (`backend/src/main/java/com/aadi/sandesh/controller/UserController.java`)
    -   **Responsibility:** REST endpoints for user profile and directory queries.

    -   **What to understand:** `GET /api/users/me` and `GET /api/users/search?q={query}`.

    -   **Connections:** HTTP transport for user profile and search.

    -   **Why now:** Completes the user discovery feature.

-   [ ] `UserBlock.java` (`backend/src/main/java/com/aadi/sandesh/domain/UserBlock.java`)
    -   **Responsibility:** JPA Entity for user blocking relations.

    -   **What to understand:** `id`, `blockerId`, `blockedId`, and unique composite constraint.

    -   **Connections:** Persisted in `user_blocks` table.

    -   **Why now:** Domain entity for the blocking engine.

-   [ ] `UserBlockRepository.java` (`backend/src/main/java/com/aadi/sandesh/repository/UserBlockRepository.java`)
    -   **Responsibility:** Database queries for blocks.

    -   **What to understand:** `existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId)` and `isBlockedEitherWay(UUID userA, UUID userB)`.

    -   **Connections:** Queried before sending messages, initiating direct chats, or publishing typing status.

    -   **Why now:** Safety gatekeeper at database level.

-   [ ] Safety DTOs (`backend/src/main/java/com/aadi/sandesh/dto/safety/`)
    -   [ ] `BlockUserRequest.java` — Contains `userId` to block.

    -   [ ] `BlockedUserResponse.java` — Profile summary of blocked person with blocked timestamp.

    -   **What to understand:** Simple DTO contracts for safety operations.

    -   **Connections:** Payloads for `UserSafetyController`.

    -   **Why now:** Required before reading safety service.

-   [ ] `UserSafetyService.java` (`backend/src/main/java/com/aadi/sandesh/service/UserSafetyService.java`)
    -   **Responsibility:** Business logic for blocking, unblocking, and block enforcement.

    -   **What to understand:**

        1.  `blockUser()`: Prevents self-blocking (`callerId.equals(targetId)`), creates idempotent block record, logs audit event.
        2.  `unblockUser()`: Removes block record.
        3.  `isBlockedEitherWay()`: Returns true if user A blocked B OR user B blocked A.

    -   **Connections:** Injected into `ConversationService` and `TypingService`.

    -   **Why now:** Central safety engine for the entire chat system.

-   [ ] `UserSafetyController.java` (`backend/src/main/java/com/aadi/sandesh/controller/UserSafetyController.java`)
    -   **Responsibility:** REST endpoints for user blocking.

    -   **What to understand:** `POST /api/safety/blocks` (204 No Content), `GET /api/safety/blocks/{userId}`, and `DELETE /api/safety/blocks/{userId}` (204 No Content).

    -   **Connections:** HTTP interface for privacy settings.

    -   **Why now:** Completes the user directory and safety slice.

### Self-Assessment & Concept Check
1. Trace the exact path of an incoming HTTP request with `Authorization: Bearer <valid_jwt>` to the point where `currentUser.id(authentication)` returns the caller's UUID.
2. Agar client galat password enter kare, to execution flow `AuthService` se lekar HTTP response tak kaise travel karta hai?

---

## Phase 3: User Directory & Relational Safety (Blocking Engine)

### Phase Objective
Is phase ke baad aap samjhenge ki users doosre users ko kaise discover karte hain aur user blocking engine direct chats aur notifications ko prevent karne ke liye kaise kaam karta hai.

### Prerequisites
- Phase 2 completed.

### Ordered Checklist

-   [ ] Enums: `ConversationType.java` & `MemberRole.java` (`backend/src/main/java/com/aadi/sandesh/domain/enums/`)
    -   **Responsibility:** Core domain enums.

    -   **What to understand:** `ConversationType` (DIRECT, GROUP) and `MemberRole` (OWNER, ADMIN, MEMBER) with helper method `canManageMembers()`.

    -   **Connections:** Used inside `Conversation` and `ConversationMember`.

    -   **Why now:** Foundational domain definitions for chats.

-   [ ] `Conversation.java` (`backend/src/main/java/com/aadi/sandesh/domain/Conversation.java`) & `ConversationMember.java` (`backend/src/main/java/com/aadi/sandesh/domain/ConversationMember.java`)
    -   **Responsibility:** JPA Entities representing chat rooms and participant membership.

    -   **What to understand:** `Conversation` has `type`, `title`, `creatorId`. `ConversationMember` has composite identity (`conversationId`, `userId`), `role`, `hiddenAfterSequence`, and `readThroughSequenceNumber`.

    -   **Connections:** Maps to `conversations` and `conversation_members` tables.

    -   **Why now:** Primary domain aggregates for messaging.

-   [ ] Repositories: `ConversationRepository.java` & `ConversationMemberRepository.java` (`backend/src/main/java/com/aadi/sandesh/repository/`)
    -   **Responsibility:** Complex query execution for conversations and members.

    -   **What to understand:** `findExistingDirectConversation` (matching exact 2 members), `findMemberResponses`, and `findRecipientIds`.

    -   **Connections:** Persistence gateway for conversation use cases.

    -   **Why now:** Data access layer for conversations.

-   [ ] Conversation DTOs (`backend/src/main/java/com/aadi/sandesh/dto/conversation/`)
    -   [ ] `CreateConversationRequest.java` — Holds type, optional title, and member IDs set.

    -   [ ] `AddMemberRequest.java` — Holds user ID and initial role for adding group members.

    -   [ ] `UpdateConversationRequest.java` — Holds updated title.

    -   [ ] `MemberResponse.java` & `ConversationMemberResponse.java` — Participant profile and role representations.

    -   [ ] `ConversationResponse.java` — Full chat summary including members list and latest message preview.

    -   **What to understand:** How data is shaped when transferring between client and server.

    -   **Connections:** Request and response contracts for `ConversationController`.

    -   **Why now:** Required before reading `ConversationService`.

-   [ ] `MembershipService.java` (`backend/src/main/java/com/aadi/sandesh/service/MembershipService.java`)
    -   **Responsibility:** Reusable membership gatekeeper.

    -   **What to understand:** `requireMember(conversationId, userId)` (throws `ForbiddenException` if caller is not in chat) and `requireManager()` (requires OWNER or ADMIN).

    -   **Connections:** Reused by `ConversationService`, `MessageService`, `ReceiptService`, `MessageQueryService`, and `TypingService`.

    -   **Why now:** Central authorization utility for all messaging operations.

-   [ ] `ConversationEventService.java` (`backend/src/main/java/com/aadi/sandesh/service/ConversationEventService.java`)
    -   **Responsibility:** Writes conversation lifecycle events to outbox.

    -   **What to understand:** `conversationChangedForMembers()`, `memberRemoved()`, and `conversationRemoved()` saving `OutboxEvent` with `EventType.CONVERSATION_CREATED / UPDATED / REMOVED`.

    -   **Connections:** Bridges conversation changes to the Outbox publishing engine.

    -   **Why now:** Explains how conversation updates become real-time events.

-   [ ] `ConversationService.java` (`backend/src/main/java/com/aadi/sandesh/service/ConversationService.java`)
    -   **Responsibility:** Complete conversation management business logic.

    -   **What to understand:**

        1.  `create()`: Direct chat must have exactly 2 members; blocked checks; existing direct conversation deduplication; group chat requires $\ge 3$ members.
        2.  `get()` & `list()`: Enforces membership and calculates latest message previews.
        3.  `addMember()` & `removeMember()`: Enforces role permissions via `membershipService.requireManager()`.
        4.  `deleteForUser()`: Distinguishes between group hard delete (owner only, no other members) vs soft hide (`hiddenAfterSequence = latestSequence`).

    -   **Connections:** Main orchestrator of the conversation lifecycle.

    -   **Why now:** Heart of the chat room management system.

-   [ ] `ConversationController.java` (`backend/src/main/java/com/aadi/sandesh/controller/ConversationController.java`)
    -   **Responsibility:** REST endpoints for conversation operations.

    -   **What to understand:** `POST /api/conversations`, `GET /api/conversations/{id}`, `GET /api/conversations`, `POST /api/conversations/{id}/members`, `DELETE /api/conversations/{id}/members/{userId}`, and `DELETE /api/conversations/{id}`.

    -   **Connections:** HTTP transport for conversations.

    -   **Why now:** Completes the conversation API layer.

### Self-Assessment & Concept Check
1. Agar User A ne User B ko block kiya hai, to database me kaun si row banti hai (`blocker_id` kaun hota hai aur `blocked_id` kaun)?
2. Self-blocking kyu prohibited hai aur code me kahan check hoti hai?

---

## Phase 4: Conversation Aggregate & Membership Management

### Phase Objective
Is phase ke end tak aap samjhenge ki Direct vs Group conversations kaise bante hain, membership roles (`OWNER`, `ADMIN`, `MEMBER`) kaise enforce hote hain, aur chat clearing ("Soft Hiding") kaise kaam karti hai.

### Prerequisites
- Phase 2 (Authentication) and Phase 3 (Blocking) completed.

### Ordered Checklist

-   Phase 4 completed.

-   Concept of Database Transactions (`@Transactional`), Row-Level Locking, and Idempotent APIs.

### Self-Assessment & Concept Check
1. Agar User A aur User B ke beech pehle se direct conversation exist karti hai, aur User A dobara `create` call karta hai, to kya hota hai?
2. "Soft Hiding" kya hai aur `hiddenAfterSequence` column WhatsApp ke 'Delete Chat' feature ko kaise replicate karta hai?

---

## Phase 5: Core Messaging: Persistence, Monotonic Sequences & Idempotency

### Phase Objective
Is phase ke end tak aap samjhenge ki message bhejne par database me sequence numbers kaise monotonically increment hote hain, duplicate network calls ko `clientMessageId` se kaise handle kiya jata hai, aur history synchronization pagination kaise kaam karti hai.

### Prerequisites
-   [ ] Messaging DTOs (`backend/src/main/java/com/aadi/sandesh/dto/message/`)
    -   [ ] `SendMessageRequest.java` — Holds `clientMessageId: UUID` and `content: String` (`@NotBlank`, `@Size(max=4000)`).

    -   [ ] `MessageResponse.java` — Holds `id`, `conversationId`, `senderId`, `sequenceNumber`, `content`, `createdAt`, and `receiptState`.

    -   [ ] `MessageSyncResponse.java` — Wrapper returning `List<MessageResponse>`, `hasMore: boolean`, and `nextSequenceNumber: long`.

    -   **What to understand:** Clean separation of client input and server response contracts.

    -   **Connections:** Serialization models for message REST endpoints.

    -   **Why now:** Prerequisite for message service and controller.

-   [ ] `Message.java` (`backend/src/main/java/com/aadi/sandesh/domain/Message.java`)
    -   **Responsibility:** JPA Entity for persisted chat messages.

    -   **What to understand:**

        1.  Fields: `id`, `conversationId`, `senderId`, `sequenceNumber`, `clientMessageId`, `content`, `createdAt`.
        2.  Enforces non-blank content validation in entity lifecycle.

    -   **Connections:** Maps to `messages` table; root entity for receipts.

    -   **Why now:** Core messaging persistence model.

-   [ ] `MessageRepository.java` (`backend/src/main/java/com/aadi/sandesh/repository/MessageRepository.java`)
    -   **Responsibility:** Spring Data JPA interface for message queries.

    -   **What to understand:**

        1.  `findMaxSequenceNumber(conversationId)`: Calculates current highest sequence in chat.
        2.  `findBySenderIdAndClientMessageId(senderId, clientMessageId)`: Enforces idempotency lookup.
        3.  `findMessagesForSync(...)`: Synchronizes messages paginated by `afterSequence` and filters out messages prior to `hiddenAfterSequence`.

    -   **Connections:** Data access layer for message persistence and synchronization.

    -   **Why now:** The SQL queries driving message consistency.

-   [ ] `MessageQueryService.java` (`backend/src/main/java/com/aadi/sandesh/service/MessageQueryService.java`)
    -   **Responsibility:** Read-only message retrieval service.

    -   **What to understand:**

        1.  Validates caller membership via `membershipService.requireMember()`.
        2.  Resolves `hiddenAfterSequence` for caller.
        3.  Executes `messageRepository.findMessagesForSync()`.
        4.  Enriches each message with aggregate receipt status (`SENT`, `DELIVERED`, `READ`).

    -   **Connections:** Injected into `MessageController` for message history pagination.

    -   **Why now:** Separates heavy read queries from the high-throughput write path (CQRS pattern).

-   [ ] `MessageService.java` (`backend/src/main/java/com/aadi/sandesh/service/MessageService.java`)
    -   **Responsibility:** The core write-path transaction coordinator.

    -   **What to understand step-by-step:**

        1.  Validates sender membership via `membershipService.requireMember()`.
        2.  Verifies user blocking: throws `ConflictException` if direct chat recipient blocked sender.
        3.  Checks clientMessageId idempotency: if already exists with same content, returns existing message idempotently (HTTP 201). If content differs, throws `ConflictException` (HTTP 409).
        4.  Monotonic Sequence assignment: calculates $nextSequence = currentMax + 1$.
        5.  Saves `Message` entity to database.
        6.  Creates initial `MessageReceipt` with `state = SENT`.
        7.  Atomically inserts `OutboxEvent` with `EventType.MESSAGE_CREATED`.
        8.  Records audit event.

    -   **Connections:** The most important business service in the write path.

    -   **Why now:** Core write-path implementation.

-   [ ] `MessageController.java` (`backend/src/main/java/com/aadi/sandesh/controller/MessageController.java`)
    -   **Responsibility:** REST endpoints for sending and fetching messages.

    -   **What to understand:** `POST /api/conversations/{id}/messages` (201 Created) and `GET /api/conversations/{id}/messages?afterSequence=0&size=50` (200 OK).

    -   **Connections:** HTTP entry point for message operations.

    -   **Why now:** Completes the synchronous messaging API.

### Ordered Checklist

-   [ ] `ReceiptState.java` & `ReceiptEvent.java` (`backend/src/main/java/com/aadi/sandesh/domain/enums/` & `dto/event/`)
    -   **Responsibility:** Domain enum (`SENT`, `DELIVERED`, `READ`) and receipt event model.

    -   **What to understand:** The state machine transition hierarchy:
        $\text{SENT} \longrightarrow \text{DELIVERED} \longrightarrow \text{READ}$
        Transitions cannot move backwards (cannot downgrade `READ` to `DELIVERED`).

    -   **Connections:** Governs receipt state transitions.

    -   **Why now:** Domain rules for receipt progression.

-   [ ] Receipt DTOs (`backend/src/main/java/com/aadi/sandesh/dto/receipt/`)
    -   [ ] `MessageReceiptResponse.java` — Holds `messageId`, `recipientId`, `state`, `deliveredAt`, `readAt`.

    -   [ ] `AcknowledgeReceiptRequest.java` — Holds `state: ReceiptState` (DELIVERED or READ).

    -   **What to understand:** Simple DTO contracts for tick acknowledgements.

    -   **Connections:** Request/Response payloads for receipt endpoint.

    -   **Why now:** Required before reading receipt service.

-   [ ] `MessageReceipt.java` (`backend/src/main/java/com/aadi/sandesh/domain/MessageReceipt.java`)
    -   **Responsibility:** JPA Entity storing receipt state per recipient per message.

    -   **What to understand:** Composite key (`messageId`, `recipientId`), `transitionTo(newState)` enforcing forward-only progression.

    -   **Connections:** Maps to `message_receipts` table.

    -   **Why now:** Entity representing individual tick state.

-   [ ] `MessageReceiptRepository.java` (`backend/src/main/java/com/aadi/sandesh/repository/MessageReceiptRepository.java`)
    -   **Responsibility:** Spring Data JPA interface for receipts.

    -   **What to understand:** Pessimistic locking query `findByIdForUpdate(messageId, recipientId)` with `LockModeType.PESSIMISTIC_WRITE` to serialize concurrent delivery and read receipts.

    -   **Connections:** Database layer for receipt state machine.

    -   **Why now:** Concurrency control for ticks.

-   [ ] `MessageReceiptStateService.java` (`backend/src/main/java/com/aadi/sandesh/service/MessageReceiptStateService.java`)
    -   **Responsibility:** Calculates aggregate receipt status for sender view.

    -   **What to understand:**

        1.  In Direct Chat: returns recipient's exact state (`SENT`, `DELIVERED`, `READ`).
        2.  In Group Chat: returns `READ` only if all other active members read it; returns `DELIVERED` if all delivered; else `SENT`.

    -   **Connections:** Injected into `MessageQueryService` and `ReceiptService`.

    -   **Why now:** Powers WhatsApp-style group tick logic.

-   [ ] `ReceiptService.java` (`backend/src/main/java/com/aadi/sandesh/service/ReceiptService.java`)
    -   **Responsibility:** Core receipt acknowledgement transaction coordinator.

    -   **What to understand step-by-step:**

        1.  Validates caller membership and verifies caller is NOT the sender (sender cannot acknowledge their own message).
        2.  Loads receipt with `findByIdForUpdate` (row lock).
        3.  If READ: updates `member.markReadThrough(message.getSequenceNumber())`.
        4.  Only writes `OutboxEvent` (`MESSAGE_DELIVERED` / `MESSAGE_READ`) if `stateChanged == true` (idempotency).
        5.  Handles `recalculateForRemovedMember()` when members leave or are removed.

    -   **Connections:** Invoked by `MessageController.acknowledgeReceipt`.

    -   **Why now:** Core receipt update use-case.

-   [ ] Revisit `MessageController.java` (`backend/src/main/java/com/aadi/sandesh/controller/MessageController.java`)
    -   **Focus:** `POST /api/conversations/{id}/messages/{messageId}/receipts` endpoint invoking `receiptService.acknowledge(...)`.

    -   **Why now:** See how client delivery and read receipts enter the system.

### Self-Assessment & Concept Check
1. Agar do mobile clients ek sath conversation me message bhejte hain, to sequence number duplicate hone se database-level constraint kaise prevent karti hai?
2. `clientMessageId` kya problem solve karta hai aur agar user same ID se different content bhej de to system kya karta hai?
3. Reconnecting client ko missed messages fetch karne ke liye kaun sa endpoint call karna hota hai aur query parameter kya hota hai?

---

## Phase 6: Message Receipts & Monotonic State Machine (Ticks)

### Phase Objective
Is phase ke baad aap samjhenge ki single tick, double tick, aur blue ticks ka backend state machine kaise kaam karta hai, pessimistic locking race conditions ko kaise rokti hai, aur group chats me aggregate receipts kaise calculate hote hain.

### Prerequisites
- Phase 5 (Core Messaging) completed.

### Ordered Checklist

-   Phase 5 (Core Messaging) and Phase 6 (Receipts) completed.

-   Concept of Distributed Transactions, Dual-Writes, and Redis Streams.

### Self-Assessment & Concept Check
1. Group chat me 3 members hain (A = Sender, B & C = Recipients). Agar B read karta hai aur C offline hai, to Sender A ko kaun sa tick state dikhega?
2. Duplicate receipt requests redundant outbox events kyu trigger nahi karte?

---

## Phase 7: Transactional Outbox Pattern & Background Publisher

### Phase Objective
Is phase ke end tak aap samjhenge ki "Dual-Write Problem" kya hota hai, application crash hone par bhi real-time events kaise drop nahi hote, aur `OutboxPublisher` background me PostgreSQL rows ko claim karke Redis Streams me kaise push karta hai.

### Prerequisites
-   [ ] Enums: `EventType.java` & `OutboxStatus.java` (`backend/src/main/java/com/aadi/sandesh/domain/enums/`)
    -   **Responsibility:** Outbox domain enums.

    -   **What to understand:** `EventType` (`MESSAGE_CREATED`, `MESSAGE_DELIVERED`, `MESSAGE_READ`, `CONVERSATION_CREATED`, etc.) and `OutboxStatus` (`PENDING`, `PUBLISHED`, `FAILED`).

    -   **Connections:** Categorizes outbox payload and worker state.

    -   **Why now:** Foundations for outbox event lifecycle.

-   [ ] `RealtimeEvent.java` (`backend/src/main/java/com/aadi/sandesh/dto/event/RealtimeEvent.java`)
    -   **Responsibility:** Standard event envelope delivered to clients.

    -   **What to understand:** Fields: `eventId`, `eventType`, `conversationId`, `timestamp`, `payload`.

    -   **Connections:** Serialized into JSON across Redis Stream, Redis Pub/Sub, and WebSocket STOMP frames.

    -   **Why now:** The unified event wire format.

-   [ ] `OutboxEvent.java` (`backend/src/main/java/com/aadi/sandesh/domain/OutboxEvent.java`)
    -   **Responsibility:** JPA Entity for transactional outbox table.

    -   **What to understand:**

        1.  Fields: `id`, `aggregateType`, `aggregateId`, `eventType`, `payload`, `status`, `retryCount`, `nextRetryAt`.
        2.  Methods: `markPublished()`, `markFailed()`, and exponential backoff retry scheduling:
            $\text{delay} = \min(60, 2^{\text{retryCount}})\text{ seconds}$

    -   **Connections:** Maps to `outbox_events` table; manipulated by `OutboxPublisher`.

    -   **Why now:** Outbox persistence model.

-   [ ] `OutboxRepository.java` (`backend/src/main/java/com/aadi/sandesh/repository/OutboxRepository.java`)
    -   **Responsibility:** Polling repository with PostgreSQL concurrency locks.

    -   **What to understand:**

        ```sql
        SELECT * FROM outbox_events 
        WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now)
        ORDER BY created_at ASC 
        LIMIT :batchSize 
        FOR UPDATE SKIP LOCKED
        ```

        Why `SKIP LOCKED` prevents multiple application instances from claiming the same event.

    -   **Connections:** Polled by `OutboxPublisher`.

    -   **Why now:** Concurrency control at database level.

-   [ ] `OutboxPublisher.java` (`backend/src/main/java/com/aadi/sandesh/messaging/OutboxPublisher.java`)
    -   **Responsibility:** `@Scheduled` worker polling database and publishing to Redis Stream.

    -   **What to understand:**

        1.  Polling interval: `${sandesh.messaging.outbox-poll-delay-ms:100}` (every 100ms).
        2.  Runs inside `@Transactional` boundary.
        3.  Calls `redisTemplate.opsForStream().add(streamKey, Map.of("eventId", ...))`.
        4.  Marks record `PUBLISHED` only after Redis confirms success.
        5.  On Redis failure: catches `RuntimeException`, calls `event.scheduleRetry()`, records sanitized error in `lastError`, and increments Micrometer metrics.

    -   **Connections:** Bridge between PostgreSQL and Redis Stream.

    -   **Why now:** The engine that drives the outbound event pipeline.

### Ordered Checklist

-   Phase 7 (Outbox Pattern) completed.

-   Concept of Redis Streams (`XADD`, `XREADGROUP`, `XACK`, `XCLAIM`).

### Self-Assessment & Concept Check
1. "Dual-Write Problem" kya hota hai aur Sandesh ne use solve karne ke liye outbox pattern kyu chuna?
2. Multi-instance deployment me `FOR UPDATE SKIP LOCKED` duplicate publishing ko kaise prevent karta hai?

---

## Phase 8: Redis Streams, Consumer Groups & The Delivery Worker

### Phase Objective
Is phase ke baad aap samjhenge ki Redis Streams me Consumer Groups kaise kaam karte hain, `MessageDeliveryWorker` background me stream se messages kaise consume karta hai, idempotency check kaise lagti hai, aur offline users ke liye notification kaise trigger hoti hai.

### Prerequisites
-   [ ] `RedisConfig.java` (`backend/src/main/java/com/aadi/sandesh/config/RedisConfig.java`)
    -   **Responsibility:** Redis connection factory, template, and stream initializer.

    -   **What to understand:**

        1.  Configures `LettuceConnectionFactory`.
        2.  Configures `RedisTemplate<String, Object>` with `StringRedisSerializer` and `Jackson2JsonRedisSerializer`.
        3.  Automatic Stream & Consumer Group initialization: creates stream key `${sandesh.messaging.stream-name}` and consumer group `${sandesh.messaging.consumer-group}` using `XGROUP CREATE ... MKSTREAM` safely ignoring busygroup errors.

    -   **Connections:** Infrastructure bean powering Redis operations.

    -   **Why now:** Prepares Redis Stream for consumer worker.

-   [ ] `ProcessedEvent.java` & `ProcessedEventRepository.java` (`backend/src/main/java/com/aadi/sandesh/domain/` & `repository/`)
    -   **Responsibility:** Consumer idempotency tracking entity and repository.

    -   **What to understand:** Maps to `processed_events` table; saves `eventId` after successful consumer processing.

    -   **Connections:** Queried by `MessageDeliveryWorker` before processing each event.

    -   **Why now:** Guarantees exactly-once processing semantics at the worker layer.

-   [ ] `MessageDeliveryWorker.java` (`backend/src/main/java/com/aadi/sandesh/messaging/MessageDeliveryWorker.java`)
    -   **Responsibility:** Scheduled Redis Stream consumer and routing worker.

    -   **What to understand step-by-step:**

        1.  Scheduled polling: runs every 50ms using `StreamReadOptions` with consumer group.
        2.  Idempotency Check: `processedEventRepository.existsById(eventId)`. If already processed, immediately calls `redisTemplate.opsForStream().acknowledge()` (`XACK`) and skips.
        3.  Routing logic:

            -   For `MESSAGE_CREATED`: iterates `recipientIds`, constructs `RealtimeEvent`, and publishes to `RedisRealtimePublisher` **regardless of presence**!

            -   If recipient is offline: triggers `notificationService.notifyOfflineUser()`.

            -   Saves event ID in `processed_events` and acknowledges (`XACK`).
        4.  `recoverPending()`: Recovers unacknowledged messages and claims stale records idle for $>30\text{s}$ via `XCLAIM`.

    -   **Connections:** Consumes from Redis Stream; publishes to Redis Pub/Sub.

    -   **Why now:** The core routing and fanout worker of the messaging system.

### Ordered Checklist

-   Phase 2 (Security) and Phase 8 (Delivery Worker) completed.

-   Concept of WebSockets (RFC 6455), STOMP protocol (CONNECT, SUBSCRIBE, SEND frames), and Pub/Sub message broker patterns.

### Self-Assessment & Concept Check
1. Real-time message fanout me presence check kyu nahi lagaya gaya (presence sirf offline notification kyu decide karta hai)?
2. Agar Redis stream me corrupted JSON message aa jaye, to consumer group ko infinite loop ya stall hone se kaise bachaya gaya hai?

---

## Phase 9: WebSocket STOMP Gateway & Redis Pub/Sub Fanout

### Phase Objective
Is phase ke end tak aap samjhenge ki WebSocket handshake aur STOMP protocol connection me kya difference hai, `WebSocketAuthInterceptor` JWT ko kaise validate karta hai, aur Redis Pub/Sub multiple backend servers tak message kaise broadcast karta hai.

### Prerequisites
-   [ ] `WebSocketConfig.java` (`backend/src/main/java/com/aadi/sandesh/config/WebSocketConfig.java`)
    -   **Responsibility:** Spring WebSocket & STOMP configuration.

    -   **What to understand:**

        1.  `@EnableWebSocketMessageBroker`.
        2.  `registerStompEndpoints`: registers `/ws` endpoint with SockJS fallback and configures allowed origins (`${sandesh.websocket.allowed-origins}`).
        3.  `configureMessageBroker`: enables simple broker with prefix `/queue` and `/topic`, sets application destination prefix `/app`, and user destination prefix `/user`.
        4.  `configureClientInboundChannel`: registers `WebSocketAuthInterceptor`.

    -   **Connections:** Configures the real-time communication tunnel for clients.

    -   **Why now:** Transport-level configuration for real-time messaging.

-   [ ] `WebSocketAuthInterceptor.java` (`backend/src/main/java/com/aadi/sandesh/config/WebSocketAuthInterceptor.java`)
    -   **Responsibility:** `ChannelInterceptor` authenticating STOMP frames.

    -   **What to understand:**

        1.  Intercepts `StompCommand.CONNECT`.
        2.  Extracts `Authorization: Bearer <token>` from STOMP native headers.
        3.  Validates JWT via `JwtService`.
        4.  Constructs `ChatPrincipal` and attaches `accessor.setUser(auth)`.

    -   **Connections:** Security gatekeeper for WebSocket connections.

    -   **Why now:** Ensures unauthenticated clients cannot establish a STOMP session.

-   [ ] `RedisRealtimePublisher.java` (`backend/src/main/java/com/aadi/sandesh/messaging/RedisRealtimePublisher.java`)
    -   **Responsibility:** Redis Pub/Sub publisher for multi-node broadcast.

    -   **What to understand:** Calls `redisTemplate.convertAndSend("sandesh:realtime-events", event)`.

    -   **Connections:** Called by `MessageDeliveryWorker` and `TypingService`.

    -   **Why now:** Broadcasts events to all cluster nodes so whichever node holds the client's WebSocket connection can deliver it.

-   [ ] `RedisRealtimeSubscriber.java` (`backend/src/main/java/com/aadi/sandesh/messaging/RedisRealtimeSubscriber.java`)
    -   **Responsibility:** Redis Pub/Sub listener delivering events to local WebSockets.

    -   **What to understand:**

        1.  Implements `MessageListener`.
        2.  Deserializes `RealtimeEvent`.
        3.  Calls `simpMessagingTemplate.convertAndSendToUser(targetUserId, "/queue/events", event)`.
        4.  Spring's broker resolves this to active client's `/queue/events-user{sessionId}`.

    -   **Connections:** Ingress component delivering Redis events to WebSocket clients.

    -   **Why now:** Completes the real-time delivery loop from Redis to client.

-   [ ] `RealtimeTestController.java` (`backend/src/main/java/com/aadi/sandesh/controller/RealtimeTestController.java`)
    -   **Responsibility:** Dev/Test controller for inspecting real-time delivery.

    -   **What to understand:** Annotated with `@Profile({"dev", "test"})` so it is NEVER exposed in production. Allows sending test events over WebSocket.

    -   **Connections:** Debugging utility for WebSocket pipelines.

    -   **Why now:** Testing tool for manual WebSocket verification.

### Ordered Checklist

-   Phase 3 (Blocking) and Phase 9 (WebSocket/STOMP) completed.

-   Concept of Redis Data Structures: Strings, Sets, and Sorted Sets (ZSET with score).

### Self-Assessment & Concept Check
1. Browser `/ws` par HTTP connection initiate karta hai aur STOMP CONNECT frame bhejta hai. In dono steps me authentication kahan aur kaise hoti hai?
2. Multi-node deployment me Redis Pub/Sub kyu zaroori hai (agar node 1 par worker hai aur node 2 par client connected hai)?

---

## Phase 10: Real-Time Presence (ZSET) & Typing Indicators

### Phase Objective
Is phase ke baad aap samjhenge ki user ka online/offline status Redis Sorted Sets (ZSET) me multi-session ke sath kaise track hota hai, heartbeats kaise kaam karte hain, aur typing indicators direct/group chats me blocking rules ke sath kaise broadcast hote hain.

### Prerequisites
-   [ ] `PresenceResponse.java` (`backend/src/main/java/com/aadi/sandesh/dto/presence/PresenceResponse.java`) & `TypingEvent.java` (`backend/src/main/java/com/aadi/sandesh/dto/event/TypingEvent.java`)
    -   **Responsibility:** DTOs for presence status (`userId`, `online`, `lastSeen`) and typing notifications (`typing: boolean`).

    -   **What to understand:** Lightweight payloads for ephemeral real-time state.

    -   **Connections:** Used in controllers and WebSocket mappings.

    -   **Why now:** DTO prerequisites for presence and typing.

-   [ ] `PresenceService.java` (`backend/src/main/java/com/aadi/sandesh/service/PresenceService.java`)
    -   **Responsibility:** Multi-session presence manager.

    -   **What to understand:**

        1.  ZSET key: `presence:user:{userId}:sessions` with score as epoch expiration timestamp ($now + 30\text{s}$).
        2.  `connected()`: Adds/refreshes `sessionId` in ZSET and cleans stale sessions (`removeRangeByScore(key, 0, now)`).
        3.  `isOnline()`: Purges expired sessions and counts active sessions ($count > 0$).
        4.  `disconnected()`: Removes session; if remaining active count is 0, records `lastSeenKey` timestamp.
        5.  Fallback: Catches `DataAccessException` to degrade gracefully without throwing 500s when Redis is down.

    -   **Connections:** Injected into `WebSocketPresenceListener`, `MessageDeliveryWorker`, and `PresenceController`.

    -   **Why now:** The multi-session presence tracking engine.

-   [ ] `WebSocketPresenceListener.java` (`backend/src/main/java/com/aadi/sandesh/config/WebSocketPresenceListener.java`)
    -   **Responsibility:** Spring ApplicationEvent listener for WebSocket lifecycle.

    -   **What to understand:**

        1.  Listens for `SessionConnectedEvent`: extracts `ChatPrincipal` and calls `presenceService.connected(userId, sessionId)`.
        2.  Listens for `SessionDisconnectEvent`: tracks sessionId-to-userId mapping in `ConcurrentHashMap` and calls `presenceService.disconnected(userId, sessionId)`.

    -   **Connections:** Bridges Spring's internal WebSocket events to `PresenceService`.

    -   **Why now:** Automates presence registration on connect/disconnect.

-   [ ] `TypingService.java` (`backend/src/main/java/com/aadi/sandesh/service/TypingService.java`)
    -   **Responsibility:** Transient typing indicator publisher.

    -   **What to understand:**

        1.  Validates caller membership via `membershipService.requireMember()`.
        2.  Checks blocking: in DIRECT chat, if either user blocked the other, typing is suppressed.
        3.  In GROUP chat: filters out blocked members so they do not see the indicator.
        4.  Ephemeral publish: publishes directly to `RedisRealtimePublisher` (does NOT write to PostgreSQL outbox, preventing database write amplification!).

    -   **Connections:** Called by `ChatWebSocketController`.

    -   **Why now:** Real-time ephemeral communication logic.

-   [ ] `ChatWebSocketController.java` (`backend/src/main/java/com/aadi/sandesh/controller/ChatWebSocketController.java`)
    -   **Responsibility:** Inbound STOMP message mapping controller.

    -   **What to understand:**

        1.  `@MessageMapping("/presence/heartbeat")`: Client sends periodic heartbeat to keep presence alive.
        2.  `@MessageMapping("/conversations/{conversationId}/typing")`: Client sends typing true/false.

    -   **Connections:** Inbound STOMP controller for client-initiated real-time actions.

    -   **Why now:** Handles client messages sent over the WebSocket connection.

-   [ ] `PresenceController.java` (`backend/src/main/java/com/aadi/sandesh/controller/PresenceController.java`)
    -   **Responsibility:** REST endpoints for querying presence over HTTP.

    -   **What to understand:** `GET /api/presence/users/{userId}` and `GET /api/presence/conversations/{conversationId}`.

    -   **Connections:** HTTP transport for fetching online status and last seen.

    -   **Why now:** Allows clients to fetch presence when opening a chat window.

### Ordered Checklist

-   [ ] `MessageSearchResultResponse.java` (`backend/src/main/java/com/aadi/sandesh/dto/message/MessageSearchResultResponse.java`) & `MessageSearchService.java` (`backend/src/main/java/com/aadi/sandesh/service/MessageSearchService.java`)
    -   **Responsibility:** Full-text message search across visible conversations.

    -   **What to understand:** Clamps search size between 1 and 50, invokes `messageRepository.searchVisibleMessages(userId, query, pageable)`.

    -   **Connections:** Uses `MessageRepository`.

    -   **Why now:** Chat history discovery feature.

-   [ ] `MessageSearchController.java` (`backend/src/main/java/com/aadi/sandesh/controller/MessageSearchController.java`)
    -   **Responsibility:** REST endpoint `GET /api/messages/search?q={query}&size=50`.

    -   **What to understand:** Extracts current user ID and returns matching message snippets.

    -   **Connections:** HTTP transport for search.

    -   **Why now:** Search endpoint.

-   [ ] Device DTOs & Entity (`backend/src/main/java/com/aadi/sandesh/`)
    -   [ ] `DevicePlatform.java` (`dto/device/DevicePlatform.java`) — Enum (`ANDROID`, `IOS`, `WEB`).

    -   [ ] `RegisterDeviceRequest.java` (`dto/device/RegisterDeviceRequest.java`) — Request DTO holding `deviceId`, `platform`, `pushToken`.

    -   [ ] `DeviceRegistration.java` (`domain/DeviceRegistration.java`) & `DeviceRegistrationRepository.java` (`repository/DeviceRegistrationRepository.java`) — Entity and repository for FCM/APNS tokens.

    -   **What to understand:** Upserting devices per user and tracking active push tokens.

    -   **Connections:** Persisted in `devices` table.

    -   **Why now:** Mobile push notification prerequisite.

-   [ ] `DeviceService.java` (`backend/src/main/java/com/aadi/sandesh/service/DeviceService.java`) & `DeviceController.java` (`backend/src/main/java/com/aadi/sandesh/controller/DeviceController.java`)
    -   **Responsibility:** Device token registration service and REST endpoint `POST /api/devices` (204 No Content).

    -   **What to understand:** Refreshes platform and push token if device ID already registered for user.

    -   **Connections:** Ingests push notification tokens.

    -   **Why now:** Completes device registration flow.

-   [ ] `PushNotificationProvider.java` (`backend/src/main/java/com/aadi/sandesh/interfaces/PushNotificationProvider.java`) & `LoggingPushNotification.java` (`backend/src/main/java/com/aadi/sandesh/service/LoggingPushNotification.java`)
    -   **Responsibility:** Interface and logging stub for push delivery.

    -   **What to understand:** Decoupled architecture allowing easy replacement with Firebase Cloud Messaging (FCM) or Apple Push Notification Service (APNS) without changing business logic.

    -   **Connections:** Injected into `NotificationService`.

    -   **Why now:** Push provider abstraction.

-   [ ] `NotificationService.java` (`backend/src/main/java/com/aadi/sandesh/service/NotificationService.java`)
    -   **Responsibility:** Evaluates and dispatches offline notifications.

    -   **What to understand:**

        1.  Checks if recipient is conversation member.
        2.  Checks if conversation is muted: `member.get().isMutedAt(Instant.now())`.
        3.  Finds active devices via `deviceRepository.findAllByUserIdAndNotificationsEnabledTrue(userId)`.
        4.  Triggers `pushProvider.send()`.

    -   **Connections:** Called by `MessageDeliveryWorker` when recipient is offline.

    -   **Why now:** Connects Phase 8 delivery worker to the push notification subsystem.

-   [ ] Audit & Compliance (`backend/src/main/java/com/aadi/sandesh/`)
    -   [ ] `AuditEvent.java` (`domain/AuditEvent.java`) & `AuditEventRepository.java` (`repository/AuditEventRepository.java`) — Entity and repository for security audit logging.

    -   [ ] `AuditService.java` (`service/AuditService.java`) — Asynchronous service recording user logins, registrations, blocks, and role changes.

    -   **What to understand:** `@Async` non-blocking audit logging writing to `audit_events` table.

    -   **Connections:** Invoked by `AuthService`, `UserSafetyService`, and `ConversationService`.

    -   **Why now:** Security auditing.

-   [ ] `RateLimitService.java` (`backend/src/main/java/com/aadi/sandesh/service/RateLimitService.java`)
    -   **Responsibility:** Redis-based token bucket or sliding window rate limiter.

    -   **What to understand:** Prevents spamming and API abuse by rejecting requests with HTTP 429 when threshold exceeded.

    -   **Connections:** Enforces service protection.

    -   **Why now:** Production defense mechanism against DoS.

### Self-Assessment & Concept Check
1. Agar user mobile aur desktop dono se login hai aur desktop disconnect karta hai, to `PresenceService` use online kyu show karta rehta hai?
2. Typing indicators ko transactional outbox me save kyu nahi kiya jata (direct Redis Pub/Sub kyu use hota hai)?

---

## Phase 11: Auxiliary Systems: Search, Devices, Push & Audit Trail

### Phase Objective
Is phase ke end tak aap samjhenge ki message search, device push token registration, offline notifications, aur audit trails kaise kaam karte hain.

### Prerequisites
- Phases 2, 4, 5, and 8 completed.

### Ordered Checklist



### Self-Assessment & Concept Check
1. Agar user ne kisi conversation ko "Mute" kiya hua hai, to `NotificationService` me kya check hota hai?
2. `PushNotificationProvider` ko direct class ke bajaye interface kyu banaya gaya hai?

---

## Phase 12: End-to-End Application Verification, Manual Testing & System Design Defense

### Phase Objective
Is final phase me aap bina kisi test file ya test suite ke, actual running application ko boot karke real HTTP requests (Postman / cURL) aur PostgreSQL / Redis commands ke through verify karenge. Iske baad aap system design interview me poore Sandesh architecture ko confidently whiteboard par draw aur defend kar sakenge!

### Prerequisites


### Ordered Checklist & Step-by-Step Hands-On Run



---

## 🗺️ Master Coverage Index (120 Production Files)

Neeche Sandesh backend ke **120 core production files aur classes** ki complete inventory di gayi hai. Is list me koi bhi test class shamil nahi hai:



---

## 📦 Appendix: Automated Test Classes (Deferred / Ignored For Now)

> [!TIP]
> **Yeh section abhi ke liye ignore karein.** Jab aap poore 120 production classes ko achhe se samajh lein aur future me automated testing sikhna chahein, tab aap in files ko refer kar sakte hain:


-   All previous phases (0 through 11) completed!

-   Docker Desktop / containers running (`sandesh-postgres` & `sandesh-redis`).-   [ ] **Step 1: Start PostgreSQL and Redis Containers**
    ```powershell
    cd D:\sandesh\backend
    docker compose up -d
    ```
    -   Verify PostgreSQL health: `docker exec -it sandesh-postgres pg_isready -U sandesh_user -d sandesh_db` (returns "accepting connections").

    -   Verify Redis health: `docker exec -it sandesh-redis redis-cli ping` (returns `PONG`).

-   [ ] **Step 2: Run the Spring Boot Application**
    ```powershell
    cd D:\sandesh\backend
    .\mvnw.cmd spring-boot:run
    ```
    -   Observe logs:

        1.  Liquibase runs 11 SQL changesets and validates schema.
        2.  Redis Stream consumer group `sandesh:message-delivery-group` is initialized on `sandesh:message-events`.
        3.  Tomcat starts on port `8080`.

-   [ ] **Step 3: Register Two Test Users (Alice & Bob)**
    -   Register Alice:

        ```powershell
        curl -X POST http://localhost:8080/api/auth/register `
          -H "Content-Type: application/json" `
          -d '{"displayName": "Alice", "email": "alice@sandesh.io", "password": "Password@123"}'
        ```

        *(Save the returned* *`accessToken`* *as* *`$ALICE_TOKEN`* *and Alice's* *`id`)*

    -   Register Bob:

        ```powershell
        curl -X POST http://localhost:8080/api/auth/register `
          -H "Content-Type: application/json" `
          -d '{"displayName": "Bob", "email": "bob@sandesh.io", "password": "Password@123"}'
        ```

        *(Save the returned* *`accessToken`* *as* *`$BOB_TOKEN`* *and Bob's* *`id`)*

-   [ ] **Step 4: Create a Direct Conversation (Alice & Bob)**
    -   Alice creates chat with Bob:

        ```powershell
        curl -X POST http://localhost:8080/api/conversations `
          -H "Content-Type: application/json" `
          -H "Authorization: Bearer <ALICE_TOKEN>" `
          -d '{"type": "DIRECT", "memberIds": ["<BOB_UUID>"]}'
        ```

    -   Verify response: HTTP `201 Created`, returning `conversationId` and members list containing both Alice and Bob.

-   [ ] **Step 5: Send a Message & Verify Monotonic Sequence**
    -   Alice sends message to the conversation:

        ```powershell
        curl -X POST http://localhost:8080/api/conversations/<CONVERSATION_ID>/messages `
          -H "Content-Type: application/json" `
          -H "Authorization: Bearer <ALICE_TOKEN>" `
          -d '{"clientMessageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "content": "Hello Bob! Welcome to Sandesh."}'
        ```

    -   Verify response:

        -   `sequenceNumber: 1`

        -   `receiptState: "SENT"`

    -   **Idempotency Test:** Alice re-sends the exact same request. System returns the existing message with status `201` without incrementing sequence or creating duplicate rows!

-   [ ] **Step 6: Inspect PostgreSQL Database Tables**
    -   Check `messages` table:

        ```powershell
        docker exec -it sandesh-postgres psql -U sandesh_user -d sandesh_db -c "SELECT id, sequence_number, content FROM messages;"
        ```

    -   Check `outbox_events` table:

        ```powershell
        docker exec -it sandesh-postgres psql -U sandesh_user -d sandesh_db -c "SELECT id, event_type, status, retry_count FROM outbox_events;"
        ```

        *(Status will show* *`PUBLISHED`* *because* *`OutboxPublisher`* *processed it within 100ms!)*

    -   Check `processed_events` table:

        ```powershell
        docker exec -it sandesh-postgres psql -U sandesh_user -d sandesh_db -c "SELECT event_id, event_type FROM processed_events;"
        ```

        *(Confirms that* *`MessageDeliveryWorker`* *acknowledged and tracked the event).*

-   [ ] **Step 7: Inspect Redis Streams & Presence State**
    -   Check Redis stream length:

        ```powershell
        docker exec -it sandesh-redis redis-cli XLEN sandesh:message-events
        ```

    -   Check Consumer Group lag:

        ```powershell
        docker exec -it sandesh-redis redis-cli XINFO GROUPS sandesh:message-events
        ```

    -   Check user presence keys:

        ```powershell
        docker exec -it sandesh-redis redis-cli KEYS "presence:*"
        ```

-   [ ] **Step 8: Fetch Message History & Acknowledge Read Receipt (Bob's View)**
    -   Bob fetches conversation messages:

        ```powershell
        curl -X GET "http://localhost:8080/api/conversations/<CONVERSATION_ID>/messages?afterSequence=0&size=50" `
          -H "Authorization: Bearer <BOB_TOKEN>"
        ```

    -   Bob acknowledges receipt as `READ`:

        ```powershell
        curl -X POST "http://localhost:8080/api/conversations/<CONVERSATION_ID>/messages/<MESSAGE_ID>/receipts" `
          -H "Content-Type: application/json" `
          -H "Authorization: Bearer <BOB_TOKEN>" `
          -d '{"state": "READ"}'
        ```

    -   Verify: Receipt state transitions from `SENT` $\to$ `READ`.

-   [ ] **Step 9: System Design Defense Check**
    -   Whiteboard test: Draw the complete end-to-end journey of a message:
        $\text{Client} \xrightarrow{\text{REST}} \text{MessageController} \xrightarrow{\text{ACID Tx}} [\text{messages} + \text{outbox\_events}]$
        $\xrightarrow{\text{Polling (100ms)}} \text{OutboxPublisher} \xrightarrow{\text{XADD}} \text{Redis Stream}$
        $\xrightarrow{\text{Consumer Group}} \text{MessageDeliveryWorker} \xrightarrow{\text{XACK}} \text{Redis Pub/Sub}$
        $\xrightarrow{\text{Listener}} \text{WebSocket STOMP} \xrightarrow{/user/queue/events} \text{Recipient Client}$

    -   Be able to answer:

        1.  Sandesh scale kaise karta hai jab 100,000 active WebSocket connections hon?
        2.  Dual-write failure kyu nahi ho sakti?
        3.  Monotonic sequence numbers clients ko out-of-order delivery se kaise bachate hain?| No. | File / Class Name                            | Relative Path                                      | Phase | Responsibility                                  |
| :-: | :------------------------------------------- | :------------------------------------------------- | :---: | :---------------------------------------------- |
|  1  | `pom.xml`                                    | `backend/pom.xml`                                  |   0   | Maven build & dependencies definition           |
|  2  | `docker-compose.yaml`                        | `backend/docker-compose.yaml`                      |   0   | Local PostgreSQL 16 & Redis 7 infrastructure    |
|  3  | `.env.example`                               | `backend/.env.example`                             |   0   | Sanitized environment variable template         |
|  4  | `application.yaml`                           | `src/main/resources/application.yaml`              |   0   | Production application configuration            |
|  5  | `db.changelog-master.yaml`                   | `src/main/resources/db/changelog/`                 |   0   | Master Liquibase migration registry             |
|  6  | `001-create-users-schema.sql`                | `src/main/resources/db/changelog/`                 |   0   | Database schema: `users` table                  |
|  7  | `002-alter-users-schema.sql`                 | `src/main/resources/db/changelog/`                 |   0   | Database schema: user enabled column            |
|  8  | `003-create-user-blocks-schema.sql`          | `src/main/resources/db/changelog/`                 |   0   | Database schema: `user_blocks` table            |
|  9  | `004-create-conversations-schema.sql`        | `src/main/resources/db/changelog/`                 |   0   | Database schema: `conversations` table          |
|  10 | `005-create-conversation-members-schema.sql` | `src/main/resources/db/changelog/`                 |   0   | Database schema: `conversation_members` table   |
|  11 | `006-create-messages-schema.sql`             | `src/main/resources/db/changelog/`                 |   0   | Database schema: `messages` table               |
|  12 | `007-create-message-receipts-schema.sql`     | `src/main/resources/db/changelog/`                 |   0   | Database schema: `message_receipts` table       |
|  13 | `008-create-outbox-events-schema.sql`        | `src/main/resources/db/changelog/`                 |   0   | Database schema: `outbox_events` table          |
|  14 | `009-create-processed-events-schema.sql`     | `src/main/resources/db/changelog/`                 |   0   | Database schema: `processed_events` table       |
|  15 | `010-create-audit-events-schema.sql`         | `src/main/resources/db/changelog/`                 |   0   | Database schema: `audit_events` table           |
|  16 | `011-alter-device-tokens.sql`                | `src/main/resources/db/changelog/`                 |   0   | Database schema: `devices` table                |
|  17 | `SandeshApplication.java`                    | `src/main/java/com/aadi/sandesh/`                  |   1   | Spring Boot main application entry point        |
|  18 | `AsyncConfig.java`                           | `src/main/java/com/aadi/sandesh/config/`           |   1   | Async thread pool executor configuration        |
|  19 | `FieldViolation.java`                        | `src/main/java/com/aadi/sandesh/dto/common/`       |   1   | Validation field error representation record    |
|  20 | `ApiError.java`                              | `src/main/java/com/aadi/sandesh/dto/common/`       |   1   | Standardized JSON error response model          |
|  21 | `NotFoundException.java`                     | `src/main/java/com/aadi/sandesh/exception/`        |   1   | Domain HTTP 404 Not Found exception             |
|  22 | `UnauthorizedException.java`                 | `src/main/java/com/aadi/sandesh/exception/`        |   1   | Domain HTTP 401 Unauthorized exception          |
|  23 | `ForbiddenException.java`                    | `src/main/java/com/aadi/sandesh/exception/`        |   1   | Domain HTTP 403 Forbidden exception             |
|  24 | `ConflictException.java`                     | `src/main/java/com/aadi/sandesh/exception/`        |   1   | Domain HTTP 409 Conflict exception              |
|  25 | `RateLimitExceededException.java`            | `src/main/java/com/aadi/sandesh/exception/`        |   1   | Domain HTTP 429 Rate Limit exception            |
|  26 | `GlobalExceptionHandler.java`                | `src/main/java/com/aadi/sandesh/exception/`        |   1   | `@RestControllerAdvice` error response handler  |
|  27 | `UserAccount.java`                           | `src/main/java/com/aadi/sandesh/domain/`           |   2   | JPA Entity for user credentials                 |
|  28 | `UserAccountRepository.java`                 | `src/main/java/com/aadi/sandesh/repository/`       |   2   | Spring Data repository for user accounts        |
|  29 | `SecurityConfig.java`                        | `src/main/java/com/aadi/sandesh/config/`           |   2   | Spring Security 6 filter chain configuration    |
|  30 | `JwtService.java`                            | `src/main/java/com/aadi/sandesh/security/`         |   2   | JJWT token minting & signature validator        |
|  31 | `ChatPrincipal.java`                         | `src/main/java/com/aadi/sandesh/security/`         |   2   | Authenticated principal holder record           |
|  32 | `CurrentUser.java`                           | `src/main/java/com/aadi/sandesh/security/`         |   2   | Controller helper for principal extraction      |
|  33 | `IssuedToken.java`                           | `src/main/java/com/aadi/sandesh/security/`         |   2   | Token details wrapper record                    |
|  34 | `JwtAuthenticationFilter.java`               | `src/main/java/com/aadi/sandesh/security/`         |   2   | OncePerRequest bearer token filter              |
|  35 | `RegisterRequest.java`                       | `src/main/java/com/aadi/sandesh/dto/auth/`         |   2   | Registration payload validation record          |
|  36 | `LoginRequest.java`                          | `src/main/java/com/aadi/sandesh/dto/auth/`         |   2   | Login payload validation record                 |
|  37 | `AuthResponse.java`                          | `src/main/java/com/aadi/sandesh/dto/auth/`         |   2   | JWT auth response payload                       |
|  38 | `AuthService.java`                           | `src/main/java/com/aadi/sandesh/service/`          |   2   | Authentication business logic orchestrator      |
|  39 | `AuthController.java`                        | `src/main/java/com/aadi/sandesh/controller/`       |   2   | REST endpoints for register & login             |
|  40 | `UserResponse.java`                          | `src/main/java/com/aadi/sandesh/dto/user/`         |   3   | Public user profile DTO                         |
|  41 | `UserDirectoryService.java`                  | `src/main/java/com/aadi/sandesh/service/`          |   3   | User discovery and search service               |
|  42 | `UserController.java`                        | `src/main/java/com/aadi/sandesh/controller/`       |   3   | REST endpoints for user discovery               |
|  43 | `UserBlock.java`                             | `src/main/java/com/aadi/sandesh/domain/`           |   3   | JPA Entity for user blocking                    |
|  44 | `UserBlockRepository.java`                   | `src/main/java/com/aadi/sandesh/repository/`       |   3   | Repository for user block checks                |
|  45 | `BlockUserRequest.java`                      | `src/main/java/com/aadi/sandesh/dto/safety/`       |   3   | Block user request DTO                          |
|  46 | `BlockedUserResponse.java`                   | `src/main/java/com/aadi/sandesh/dto/safety/`       |   3   | Blocked user summary DTO                        |
|  47 | `UserSafetyService.java`                     | `src/main/java/com/aadi/sandesh/service/`          |   3   | User safety & block manager service             |
|  48 | `UserSafetyController.java`                  | `src/main/java/com/aadi/sandesh/controller/`       |   3   | REST endpoints for blocking & unblocking        |
|  49 | `ConversationType.java`                      | `src/main/java/com/aadi/sandesh/domain/enums/`     |   4   | DIRECT and GROUP conversation enum              |
|  50 | `MemberRole.java`                            | `src/main/java/com/aadi/sandesh/domain/enums/`     |   4   | OWNER, ADMIN, MEMBER role enum                  |
|  51 | `Conversation.java`                          | `src/main/java/com/aadi/sandesh/domain/`           |   4   | JPA Entity for chat rooms                       |
|  52 | `ConversationMember.java`                    | `src/main/java/com/aadi/sandesh/domain/`           |   4   | JPA Entity for conversation participants        |
|  53 | `ConversationRepository.java`                | `src/main/java/com/aadi/sandesh/repository/`       |   4   | Repository for conversation queries             |
|  54 | `ConversationMemberRepository.java`          | `src/main/java/com/aadi/sandesh/repository/`       |   4   | Repository for member access queries            |
|  55 | `CreateConversationRequest.java`             | `src/main/java/com/aadi/sandesh/dto/conversation/` |   4   | Chat room creation request DTO                  |
|  56 | `AddMemberRequest.java`                      | `src/main/java/com/aadi/sandesh/dto/conversation/` |   4   | Add member to group request DTO                 |
|  57 | `UpdateConversationRequest.java`             | `src/main/java/com/aadi/sandesh/dto/conversation/` |   4   | Update group title request DTO                  |
|  58 | `MemberResponse.java`                        | `src/main/java/com/aadi/sandesh/dto/conversation/` |   4   | Member summary response record                  |
|  59 | `ConversationMemberResponse.java`            | `src/main/java/com/aadi/sandesh/dto/conversation/` |   4   | Extended member details response                |
|  60 | `ConversationResponse.java`                  | `src/main/java/com/aadi/sandesh/dto/conversation/` |   4   | Conversation inbox summary response             |
|  61 | `MembershipService.java`                     | `src/main/java/com/aadi/sandesh/service/`          |   4   | Central membership & permissions gatekeeper     |
|  62 | `ConversationEventService.java`              | `src/main/java/com/aadi/sandesh/service/`          |   4   | Outbox publisher for conversation changes       |
|  63 | `ConversationService.java`                   | `src/main/java/com/aadi/sandesh/service/`          |   4   | Conversation business logic orchestrator        |
|  64 | `ConversationController.java`                | `src/main/java/com/aadi/sandesh/controller/`       |   4   | REST endpoints for conversation operations      |
|  65 | `SendMessageRequest.java`                    | `src/main/java/com/aadi/sandesh/dto/message/`      |   5   | Send message request payload                    |
|  66 | `MessageResponse.java`                       | `src/main/java/com/aadi/sandesh/dto/message/`      |   5   | Message response summary DTO                    |
|  67 | `MessageSyncResponse.java`                   | `src/main/java/com/aadi/sandesh/dto/message/`      |   5   | Paginated history sync response                 |
|  68 | `Message.java`                               | `src/main/java/com/aadi/sandesh/domain/`           |   5   | JPA Entity for chat messages                    |
|  69 | `MessageRepository.java`                     | `src/main/java/com/aadi/sandesh/repository/`       |   5   | Repository for monotonic sequencing & sync      |
|  70 | `MessageQueryService.java`                   | `src/main/java/com/aadi/sandesh/service/`          |   5   | Read-only message retrieval & pagination        |
|  71 | `MessageService.java`                        | `src/main/java/com/aadi/sandesh/service/`          |   5   | Write-path message coordinator                  |
|  72 | `MessageController.java`                     | `src/main/java/com/aadi/sandesh/controller/`       |   5   | REST endpoints for sending & fetching messages  |
|  73 | `ReceiptState.java`                          | `src/main/java/com/aadi/sandesh/domain/enums/`     |   6   | SENT, DELIVERED, READ tick state enum           |
|  74 | `ReceiptEvent.java`                          | `src/main/java/com/aadi/sandesh/dto/event/`        |   6   | Real-time receipt payload model                 |
|  75 | `MessageReceiptResponse.java`                | `src/main/java/com/aadi/sandesh/dto/receipt/`      |   6   | Receipt state response record                   |
|  76 | `AcknowledgeReceiptRequest.java`             | `src/main/java/com/aadi/sandesh/dto/receipt/`      |   6   | Receipt acknowledgement request                 |
|  77 | `MessageReceipt.java`                        | `src/main/java/com/aadi/sandesh/domain/`           |   6   | JPA Entity for recipient delivery ticks         |
|  78 | `MessageReceiptRepository.java`              | `src/main/java/com/aadi/sandesh/repository/`       |   6   | Repository with pessimistic locking             |
|  79 | `MessageReceiptStateService.java`            | `src/main/java/com/aadi/sandesh/service/`          |   6   | Group aggregate receipt calculator              |
|  80 | `ReceiptService.java`                        | `src/main/java/com/aadi/sandesh/service/`          |   6   | Receipt state machine manager                   |
|  81 | `EventType.java`                             | `src/main/java/com/aadi/sandesh/domain/enums/`     |   7   | Real-time event type categorization enum        |
|  82 | `OutboxStatus.java`                          | `src/main/java/com/aadi/sandesh/domain/enums/`     |   7   | PENDING, PUBLISHED, FAILED status enum          |
|  83 | `RealtimeEvent.java`                         | `src/main/java/com/aadi/sandesh/dto/event/`        |   7   | Universal real-time event envelope record       |
|  84 | `OutboxEvent.java`                           | `src/main/java/com/aadi/sandesh/domain/`           |   7   | JPA Entity for transactional outbox table       |
|  85 | `OutboxRepository.java`                      | `src/main/java/com/aadi/sandesh/repository/`       |   7   | Polling repository with FOR UPDATE SKIP LOCKED  |
|  86 | `OutboxPublisher.java`                       | `src/main/java/com/aadi/sandesh/messaging/`        |   7   | Background scheduled worker to Redis Stream     |
|  87 | `RedisConfig.java`                           | `src/main/java/com/aadi/sandesh/config/`           |   8   | Redis template & stream group bootstrapper      |
|  88 | `ProcessedEvent.java`                        | `src/main/java/com/aadi/sandesh/domain/`           |   8   | JPA Entity for worker idempotency deduplication |
|  89 | `ProcessedEventRepository.java`              | `src/main/java/com/aadi/sandesh/repository/`       |   8   | Repository tracking processed event IDs         |
|  90 | `MessageDeliveryWorker.java`                 | `src/main/java/com/aadi/sandesh/messaging/`        |   8   | Redis Stream consumer & fanout worker           |
|  91 | `WebSocketConfig.java`                       | `src/main/java/com/aadi/sandesh/config/`           |   9   | STOMP WebSocket endpoints & broker config       |
|  92 | `WebSocketAuthInterceptor.java`              | `src/main/java/com/aadi/sandesh/config/`           |   9   | STOMP CONNECT frame JWT interceptor             |
|  93 | `RedisRealtimePublisher.java`                | `src/main/java/com/aadi/sandesh/messaging/`        |   9   | Multi-node Redis Pub/Sub event broadcaster      |
|  94 | `RedisRealtimeSubscriber.java`               | `src/main/java/com/aadi/sandesh/messaging/`        |   9   | Redis Pub/Sub subscriber delivering to STOMP    |
|  95 | `RealtimeTestController.java`                | `src/main/java/com/aadi/sandesh/controller/`       |   9   | Dev/Test endpoint for WebSocket validation      |
|  96 | `PresenceResponse.java`                      | `src/main/java/com/aadi/sandesh/dto/presence/`     |   10  | Presence response DTO                           |
|  97 | `TypingEvent.java`                           | `src/main/java/com/aadi/sandesh/dto/event/`        |   10  | Typing indicator event payload record           |
|  98 | `PresenceService.java`                       | `src/main/java/com/aadi/sandesh/service/`          |   10  | Redis ZSET multi-session presence manager       |
|  99 | `WebSocketPresenceListener.java`             | `src/main/java/com/aadi/sandesh/config/`           |   10  | WebSocket lifecycle event listener              |
| 100 | `TypingService.java`                         | `src/main/java/com/aadi/sandesh/service/`          |   10  | Ephemeral typing indicator broadcaster          |
| 101 | `ChatWebSocketController.java`               | `src/main/java/com/aadi/sandesh/controller/`       |   10  | Inbound STOMP mapping controller                |
| 102 | `PresenceController.java`                    | `src/main/java/com/aadi/sandesh/controller/`       |   10  | REST endpoints for online status & last seen    |
| 103 | `MessageSearchResultResponse.java`           | `src/main/java/com/aadi/sandesh/dto/message/`      |   11  | Message search response record                  |
| 104 | `MessageSearchService.java`                  | `src/main/java/com/aadi/sandesh/service/`          |   11  | Full-text message search across chats           |
| 105 | `MessageSearchController.java`               | `src/main/java/com/aadi/sandesh/controller/`       |   11  | REST endpoint for message search                |
| 106 | `DevicePlatform.java`                        | `src/main/java/com/aadi/sandesh/dto/device/`       |   11  | ANDROID, IOS, WEB platform enum                 |
| 107 | `RegisterDeviceRequest.java`                 | `src/main/java/com/aadi/sandesh/dto/device/`       |   11  | Push token registration request DTO             |
| 108 | `DeviceRegistration.java`                    | `src/main/java/com/aadi/sandesh/domain/`           |   11  | JPA Entity for mobile push tokens               |
| 109 | `DeviceRegistrationRepository.java`          | `src/main/java/com/aadi/sandesh/repository/`       |   11  | Repository for active user devices              |
| 110 | `DeviceService.java`                         | `src/main/java/com/aadi/sandesh/service/`          |   11  | Device registration business service            |
| 111 | `DeviceController.java`                      | `src/main/java/com/aadi/sandesh/controller/`       |   11  | REST endpoint for registering device tokens     |
| 112 | `PushNotificationProvider.java`              | `src/main/java/com/aadi/sandesh/interfaces/`       |   11  | Push notification provider interface            |
| 113 | `LoggingPushNotification.java`               | `src/main/java/com/aadi/sandesh/service/`          |   11  | Local push provider stub implementation         |
| 114 | `NotificationService.java`                   | `src/main/java/com/aadi/sandesh/service/`          |   11  | Evaluates & dispatches offline notifications    |
| 115 | `AuditEvent.java`                            | `src/main/java/com/aadi/sandesh/domain/`           |   11  | JPA Entity for security audit trail             |
| 116 | `AuditEventRepository.java`                  | `src/main/java/com/aadi/sandesh/repository/`       |   11  | Repository for security audit records           |
| 117 | `AuditService.java`                          | `src/main/java/com/aadi/sandesh/service/`          |   11  | Asynchronous security audit logger              |
| 118 | `RateLimitService.java`                      | `src/main/java/com/aadi/sandesh/service/`          |   11  | Redis-based API rate limiting service           |
| 119 | `CorsProperties.java`                        | `src/main/java/com/aadi/sandesh/config/`           |   2   | CORS configuration properties binding           |
| 120 | `JwtProperties.java`                         | `src/main/java/com/aadi/sandesh/config/`           |   2   | JWT secret & TTL configuration properties       ||  No. | Test File Name                         | Path                                  | What It Tests                                                 |
| :--: | :------------------------------------- | :------------------------------------ | :------------------------------------------------------------ |
|  T-1 | `application.yaml` (test)              | `src/test/resources/application.yaml` | Isolated test profile with H2/test settings                   |
|  T-2 | `SandeshApplicationTests.java`         | `src/test/java/com/aadi/sandesh/`     | Spring Context bootstrap and Liquibase changelogs             |
|  T-3 | `JwtServiceTest.java`                  | `src/test/java/com/aadi/sandesh/`     | Unit test: JWT generation, parsing, expiration                |
|  T-4 | `AuthServiceTest.java`                 | `src/test/java/com/aadi/sandesh/`     | Unit test: Registration, password hashing, login              |
|  T-5 | `AuthIntegrationTest.java`             | `src/test/java/com/aadi/sandesh/`     | Integration test: Auth REST endpoints on live database        |
|  T-6 | `ConversationIntegrationTest.java`     | `src/test/java/com/aadi/sandesh/`     | Integration test: Conversation creation, roles, blocking      |
|  T-7 | `MessageServiceTest.java`              | `src/test/java/com/aadi/sandesh/`     | Unit test: Monotonic sequence assignment, idempotency         |
|  T-8 | `MessageIntegrationTest.java`          | `src/test/java/com/aadi/sandesh/`     | Integration test: Sending, sync pagination, receipts          |
|  T-9 | `ReceiptServiceTest.java`              | `src/test/java/com/aadi/sandesh/`     | Unit test: Monotonic tick transitions (SENT->DELIVERED->READ) |
| T-10 | `OutboxPublisherTest.java`             | `src/test/java/com/aadi/sandesh/`     | Unit test: Outbox event claiming and publishing               |
| T-11 | `MessageDeliveryWorkerTest.java`       | `src/test/java/com/aadi/sandesh/`     | Component test: Redis Stream fanout & offline notifications   |
| T-12 | `WebSocketStompTestClient.java`        | `src/test/java/com/aadi/sandesh/`     | Interactive standalone STOMP client utility                   |
| T-13 | `PresenceServiceTest.java`             | `src/test/java/com/aadi/sandesh/`     | Unit test: Redis ZSET multi-session presence                  |
| T-14 | `TypingServiceTest.java`               | `src/test/java/com/aadi/sandesh/`     | Unit test: Ephemeral typing event rules                       |
| T-15 | `RealtimeDeliveryIntegrationTest.java` | `src/test/java/com/aadi/sandesh/`     | Full E2E test: REST send to WebSocket STOMP receive           |