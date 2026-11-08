# Sandesh Project Analysis

## 1. Project Overview and Goals

Sandesh is a stateless, JWT-authenticated real-time messaging backend. It supports user registration and login, user discovery, direct and group conversations, text messages, sequence-based synchronization, delivery/read receipts, message search, typing indicators, and realtime event delivery over STOMP WebSockets.

The repository contains the Spring Boot backend and infrastructure needed to run PostgreSQL, Redis, and pgAdmin locally. It does **not** contain a frontend implementation. `FRONTEND_INTEGRATION_CONTRACT.md` is an integration contract derived from the backend and describes the client behavior expected by the API.

The central design is:

1. PostgreSQL is the source of truth for users, conversations, memberships, messages, receipts, and durable outbox records.
2. REST endpoints perform authenticated reads and writes.
3. Writes that should produce realtime notifications create transactional outbox events in the same database transaction.
4. A scheduled outbox publisher transfers events to a Redis Stream.
5. A scheduled delivery worker consumes the stream, deduplicates events, and publishes targeted Redis Pub/Sub messages.
6. A Redis subscriber forwards those events to STOMP user destinations.

This gives database writes stronger durability than direct WebSocket publication, but realtime delivery remains asynchronous and best effort. Clients must use REST synchronization after connection loss.

## 2. Features and Scope

### Implemented features

- Account registration with BCrypt password hashing.
- Login returning a signed JWT access token.
- Stateless bearer-token authentication for REST and STOMP `CONNECT`.
- Enabled-user directory search.
- Direct conversations with exactly two members.
- Group conversations with at least three members.
- Per-conversation roles: `OWNER`, `ADMIN`, and `MEMBER`.
- Group member addition and removal.
- Group title changes.
- Per-user soft hiding of conversations.
- Permanent group deletion by an owner.
- Text messages and system messages.
- Client-message-id idempotency.
- Per-conversation monotonically increasing message sequences.
- Message synchronization with hidden-history filtering.
- Delivery and read acknowledgements with monotonic state transitions.
- Sender-oriented aggregate receipt states: `SENT`, `DELIVERED`, `READ`.
- SQL `LIKE`-based message search.
- STOMP typing events.
- Redis Pub/Sub and Redis Stream realtime delivery.
- Transactional outbox with retry and exponential backoff.
- Redis Stream consumer recovery and processed-event deduplication.
- Actuator dependency and Prometheus registry support.

### Explicitly out of scope or not implemented

There is no frontend source, refresh-token flow, logout/revocation endpoint, role-change endpoint, mute API, individual receipt query API, direct-conversation leave/rejoin API, persisted typing state, user-blocking behavior, audit-event behavior, push notification integration, CI/CD workflow, or production deployment configuration.

## 3. Technology Stack

| Area | Technology | Use |
|---|---|---|
| Language | Java 25 | Backend implementation |
| Framework | Spring Boot 4.1.1 | Application bootstrap and dependency management |
| HTTP | Spring MVC | REST controllers and JSON |
| Security | Spring Security | Stateless security filter chain, CORS, error responses |
| Persistence | Spring Data JPA/Hibernate | Entity mapping and repository queries |
| Database | PostgreSQL 16 Alpine in Compose | Durable application data |
| Migrations | Liquibase SQL changelog | Ordered schema creation and transformation |
| Realtime protocol | Spring WebSocket/STOMP | Browser/client connection and user destinations |
| Messaging | Redis Pub/Sub and Redis Streams | Realtime fan-out and durable asynchronous handoff |
| Authentication token | JJWT 0.13.0 | JWT issue and parse |
| Validation | Jakarta/Spring validation | DTO constraints and request validation |
| Metrics | Actuator and Micrometer Prometheus registry | Runtime observability capability |
| Boilerplate | Lombok | Getters, constructors, and related generated code |
| Local orchestration | Docker Compose | PostgreSQL, Redis, and pgAdmin |
| Build | Maven wrapper 3.9.16 | Reproducible Maven invocation |

Important dependency observations: the presence of a dependency does not imply a feature. For example, Actuator and Prometheus support are dependencies, but no custom metrics or production monitoring configuration is present; Redis repositories are explicitly disabled; and there are no tests despite the normal Maven test lifecycle.

## 4. Repository Structure

```text
.
├── PROJECT_ANALYSIS.md
├── docs/
│   └── FRONTEND_INTEGRATION_CONTRACT.md
└── backend/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    ├── docker-compose.yaml
    ├── .env.example
    ├── pgadmin/servers.json              # local/ignored configuration
    └── src/main/
        ├── java/com/aadi/sandesh/
        │   ├── SandeshApplication.java
        │   ├── config/
        │   ├── controller/
        │   ├── domain/
        │   ├── dto/
        │   ├── exception/
        │   ├── messaging/
        │   ├── repository/
        │   ├── security/
        │   └── service/
        └── resources/
            ├── application.yaml
            └── db/changelog/
```

`../.idea` contains IDE metadata and is not an application module. There is no `src/test` tree, frontend directory, Dockerfile for the backend, Kubernetes directory, or `.github/workflows` directory.

## 5. Application Entry Points and Initialization

`../backend/src/main/java/com/aadi/sandesh/SandeshApplication.java` is the entry point. `SandeshApplication` is annotated with `@SpringBootApplication` and `@EnableScheduling`. Spring scans the `com.aadi.sandesh` package, creates the MVC, JPA, Security, WebSocket, Redis, and Liquibase infrastructure, and registers scheduled publishers/workers.

Startup sequence, at implementation level:

1. Spring Boot reads `../backend/src/main/resources/application.yaml` and environment substitutions.
2. A PostgreSQL datasource and Hikari pool are created. `SPRING_DATASOURCE_PASSWORD` and `SANDESH_JWT_SECRET` are required.
3. Liquibase applies `db.changelog-master.yaml` in changeset order.
4. Hibernate validates the resulting schema by default (`ddl-auto=validate`); it does not create the schema.
5. Security config registers the stateless JWT filter, BCrypt encoder, CORS rules, and JSON authentication/authorization handlers.
6. WebSocket config exposes `/ws` with STOMP application prefix `/app`, user prefix `/user`, and simple broker destinations `/queue` and `/topic`.
7. Redis beans configure Pub/Sub, stream access, and scheduled messaging components.
8. Scheduled outbox and delivery jobs begin polling at 100 ms defaults.

Graceful HTTP shutdown is enabled with a 20-second lifecycle timeout. The Compose file starts dependencies only; the Spring backend must be started separately.

## 6. High-Level System Architecture — Textual Explanation

The system has six logical layers:

1. **Transport**: `AuthController`, `UserController`, `ConversationController`, `MessageController`, `MessageSearchController`, and `ChatWebSocketController`.
2. **Security**: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `ChatPrincipal`, and `CurrentUser`.
3. **Application services**: authentication, membership authorization, conversation mutation, message creation/query, receipts, search, typing, rate limiting, and event creation.
4. **Persistence**: JPA entities in `domain/` and Spring Data repositories in `repository/`.
5. **Durable event delivery**: `OutboxPublisher`, Redis Stream, `MessageDeliveryWorker`, `ProcessedEventRepository`.
6. **Realtime fan-out**: Redis Pub/Sub, `RedisRealtimeSubscriber`, and STOMP `/user/queue/events`.

REST writes generally execute inside service-level transactions. A message write locks its conversation, allocates a sequence, inserts the message and recipient receipts, and inserts an outbox event atomically. Realtime delivery occurs later, outside that write transaction.

Read paths query PostgreSQL directly. Message ordering is based on `sequence_number`, not timestamps. A member’s `hidden_after_sequence` acts as a visibility boundary for synchronization and previews, although search does not consistently apply it.

## 7. Component and Module Breakdown

### Configuration

- `config/SecurityConfig.java`: BCrypt strength 12, stateless sessions, CSRF disabled, CORS, JWT filter placement, public routes, JSON 401/403 handlers, and a rejecting `UserDetailsService`.
- `config/WebSocketConfig.java`: STOMP endpoint `/ws`, application prefix `/app`, user prefix `/user`, simple broker prefixes, allowed origins.
- `config/WebSocketAuthInterceptor.java`: authenticates only STOMP `CONNECT` using a native `Authorization` bearer header.
- `config/RedisConfig.java`: Redis connection and listener/container setup.

### Controllers

- `AuthController`: `/api/auth/register`, `/api/auth/login`.
- `UserController`: enabled-user search.
- `ConversationController`: conversation CRUD-like operations and membership management.
- `MessageController`: send, synchronize, and acknowledge messages.
- `MessageSearchController`: authenticated cross-conversation search.
- `ChatWebSocketController`: client-to-server typing messages.

Controllers validate request DTOs and delegate business rules to services. They do not directly manipulate repositories.

### Services

- `AuthService`: transactional register/login and token response construction.
- `UserDirectoryService`: normalized, bounded user search.
- `MembershipService`: membership and owner/admin checks.
- `ConversationService`: create/list/get/update/delete conversations, add/remove members, system-message and event coordination.
- `MessageService`: regular/system message creation, sequence allocation, recipient receipt creation, outbox creation, DTO mapping.
- `MessageQueryService`: cursor/page normalization, hidden-history filtering, response mapping.
- `MessageReceiptStateService`: sender-oriented aggregate state.
- `ReceiptService`: locked, monotonic receipt updates, member-removal recalculation, receipt events.
- `MessageSearchService`: bounded SQL `LIKE` search.
- `TypingService`: membership check and transient Redis typing publication.
- `ConversationEventService`: targeted conversation events.
- `RateLimitService`: Redis minute-bucket limiting with a per-JVM fallback.
- `MessageDeliveryWorker`: Redis Stream group consumption, recovery, deduplication, and Pub/Sub publication.

### Domain and enums

`UserAccount`, `Conversation`, `ConversationMember`, `Message`, `MessageReceipt`, `OutboxEvent`, and `ProcessedEvent` represent persisted state. Enums under `domain/enums/` define conversation type, message type, member roles, receipt types/states, outbox status, event types, and resource types.

### DTOs

Request/response DTOs are grouped under `dto/auth`, `dto/conversation`, `dto/message`, `dto/event`, `dto/user`, and `dto/common`. `ApiError` and `FieldViolation` provide the common REST error shape. Jackson omits null response fields.

### Repositories

Repositories encapsulate queries for users, conversations, members, messages, receipts, outbox records, and processed stream events. The key concurrency queries are `ConversationRepository.findByIdForUpdate`, `OutboxRepository.findReadyForPublish` using `FOR UPDATE SKIP LOCKED`, and locked receipt queries in `MessageReceiptRepository`.

## 8. Complete Feature Documentation

### Authentication and account lifecycle

Registration normalizes email by stripping and lowercasing it, hashes the password with BCrypt, stores an enabled `UserAccount`, and issues a JWT. Login looks up the email case-insensitively, rejects missing/disabled users and invalid passwords with the same generic unauthorized response, and issues a token.

`UserAccount` fields are `id`, `displayName`, `email`, `passwordHash`, `enabled`, `createdAt`, and `updatedAt`. There is no endpoint for disabling an account and no post-issuance token check against `enabled`.

### User directory

`GET /api/users` searches only enabled users, excludes the authenticated user, matches display name/email case-insensitively, sorts by lowercase display name, and caps results at 50.

### Conversations and memberships

Creation adds the creator automatically. A direct conversation must have exactly two total members and is deduplicated by the exact member set. A group must have at least three total members. The creator is `OWNER`, supplied members are normally `MEMBER`, and group creation creates a `SYSTEM` message.

Managers (`OWNER` or `ADMIN`) can add/remove members and rename groups. The implementation does not enforce a role hierarchy when adding a member: a caller may assign `OWNER`, creating multiple owners. Direct conversations reject member operations. A new group member receives `hidden_after_sequence` equal to the current latest sequence, preventing access to earlier history.

Soft deletion is per membership, not database deletion. Permanent deletion is group-owner-only and deletes the conversation plus cascaded members, messages, and receipts. Outbox records are not foreign-keyed to conversations and remain publishable.

### Messaging

Only text messages are accepted by the public send endpoint. `MessageService` strips content and creates a per-conversation sequence under a pessimistic conversation lock. A unique `(sender_id, client_message_id)` supports retry idempotency across conversations. Same sender/client ID/conversation/content returns the existing message; a differing content or conversation returns conflict.

Each current recipient gets a receipt row. The sender is not a recipient and receives the REST response rather than a normal `MESSAGE_CREATED` event. System messages follow the same receipt/event infrastructure but are generated by conversation operations.

### Synchronization

`GET /messages` uses sequence cursors. The initial request returns the latest visible page in chronological order; a positive cursor returns strictly newer messages. The effective cursor is the maximum of the requested cursor and membership `hidden_after_sequence`. Page size is clamped to 1–100.

The implementation’s `hasMore` on an initial page describes older messages, while `nextAfterSequence` is a newer-message cursor. Consequently, the response does not provide a complete backward-pagination contract.

### Receipts

A member can acknowledge a message only when a corresponding receipt row exists. `DELIVERED` and `READ` transitions are monotonic. `READ` also advances the member’s `last_read_sequence`. Receipt rows are locked during updates. State-change events target the message sender.

Aggregate state is calculated over active recipients: no active receipts or any undelivered recipient means `SENT`; all delivered but not all read means `DELIVERED`; all read means `READ`. Historical messages are not held back by newly added members because they have no historical receipt rows.

### Search

Message search joins through conversation membership, strips the query, clamps size to 1–50, uses case-insensitive SQL `LIKE`, and sorts newest first. It does not apply the member’s hidden-history boundary, so soft-hidden history can be returned.

### Typing

Typing is a transient STOMP-to-Redis Pub/Sub workflow. It is not stored, not put through the outbox, and has no retry. Events contain the actor display name and `expiresInSeconds=5`; clients should expire the indicator.

## 9. Complete Workflow Documentation

### 9.1 Registration

**Trigger/preconditions:** unauthenticated `POST /api/auth/register` with valid display name, email, and 10–72 character password.

**Execution:** `AuthController.register` validates `RegisterRequest`; `AuthService.register` checks `UserAccountRepository.existsByEmailIgnoreCase`, BCrypt-hashes the password, creates and persists `UserAccount`, calls `JwtService.issue`, and maps `AuthResponse`.

**Alternatives/failures:** validation returns `400 VALIDATION_ERROR`; duplicate email returns `409 CONFLICT`; a database uniqueness race is not specially translated and may become `500`. The transaction commits the user before the token response is returned.

**Data/outcome:** one `users` row and a signed seven-day default JWT. Password plaintext is never persisted.

### 9.2 Login and authenticated request

**Trigger/preconditions:** `POST /api/auth/login`, then a protected REST request with `Authorization: Bearer <JWT>`.

**Execution:** `AuthService.login` loads the case-insensitive email and verifies enabled state/password with BCrypt. `JwtService` creates a JWT whose subject is the user UUID and which includes email, issued-at, and expiration. On later requests, `JwtAuthenticationFilter` parses the token and creates an authenticated `UsernamePasswordAuthenticationToken` with empty authorities; `CurrentUser.id` extracts the UUID.

**Alternatives/failures:** missing user, disabled user, or wrong password returns generic `401`. Invalid/expired bearer tokens clear the context; protected routes then return JSON `401`. There is no refresh, revocation, logout endpoint, or token invalidation after account disablement.

### 9.3 User search

**Trigger:** authenticated `GET /api/users?q=...`.

**Execution:** `UserController` calls `UserDirectoryService.searchUsers`, which strips/lowercases the query and calls the repository for enabled users excluding the current UUID. Results are limited to 50 and sorted by lowercase display name.

**Failure/outcome:** missing/invalid authentication is `401`; otherwise an empty search is a valid empty array.

### 9.4 Create direct or group conversation

**Trigger/preconditions:** authenticated member-to-be submits `POST /api/conversations`.

**Execution:** `ConversationService.create` adds the creator to the requested set, validates type-specific cardinality and user existence, and checks duplicate direct membership. It creates the conversation and memberships in a transaction. Group creation also calls `MessageService.sendSystemMessage` and creates a targeted conversation-created/update event as implemented.

**Alternatives:** invalid type/cardinality/user IDs produce validation, not-found, or conflict errors. An existing exact direct conversation is returned rather than duplicated; the controller method is annotated `201`, so the actual status behavior should be treated as implementation-sensitive rather than relying on the integration document’s `200` claim.

**Data/outcome:** `conversations`, `conversation_members`, possibly `messages`, `message_receipts`, and `outbox_events` are written atomically.

### 9.5 List/get conversations

`GET /api/conversations` finds memberships ordered by `joined_at DESC`, loads latest visible previews, and applies additional soft-hidden filtering. Hidden groups, hidden conversations created by the current user, and hidden conversations with messages may remain in the result. A hidden empty direct conversation not created by the current user may be excluded.

`GET /api/conversations/{id}` requires membership, including a soft-hidden membership, and returns members plus a latest preview. Hidden filtering is not perfectly consistent between list, detail, and preview conversion.

### 9.6 Add a group member

**Trigger:** an authenticated owner/admin calls `POST /api/conversations/{id}/members`.

**Execution:** `MembershipService.requireManager` authorizes. `ConversationService` rejects direct conversations, checks target user and duplicate membership, creates a membership with the requested role and `hidden_after_sequence` at the current latest sequence, creates a join system message, recalculates affected receipt aggregates, and emits conversation updates.

**Alternatives:** non-manager `403`, direct/duplicate operation `409`, missing target `404`. The requested role is not hierarchy-checked and may be `OWNER`.

### 9.7 Remove a group member

The manager check runs first. Direct conversations, self-removal, and owner removal are rejected. The member row is removed, a system message is sent to remaining members, the removed user receives `CONVERSATION_REMOVED`, and receipt aggregates are recalculated. The recalculation response appears to use the message sender as `userId`, unlike normal receipt responses, which is an implementation inconsistency.

### 9.8 Rename a group

An owner/admin calls `PATCH /api/conversations/{id}`. Direct conversations are rejected. `Conversation.rename` rejects null/blank titles even though the request DTO only has a maximum-length constraint. The service updates the title, creates a system message, and emits a conversation update in one transaction. Blank titles become `400 BAD_REQUEST`.

### 9.9 Soft hide or permanently delete

For `DELETE /api/conversations/{id}?permanent=false`, any member’s `hidden_after_sequence` is set to the current latest sequence. Data remains intact and a non-permanent removal event is emitted. There is no explicit rejoin endpoint.

For `permanent=true`, the caller must be a group owner. Removal events are generated for all current members, then the conversation is deleted. Database cascades remove memberships, messages, and receipts. Outbox rows remain because they have no conversation foreign key.

### 9.10 Send a message

**Trigger:** authenticated member calls `POST /api/conversations/{id}/messages`.

**Execution:** membership is required; the sender/client ID is checked for an existing message; content and conversation mismatches conflict. `RateLimitService` increments Redis key `rate:message:{userId}:{epochMinute}`. Recipients are loaded. The conversation is pessimistically locked; `allocateNextMessageSequence()` increments the sequence; the message, recipient receipts, and `MESSAGE_CREATED` outbox event are inserted in one transaction.

**Alternatives/failures:** non-member `403`; missing conversation or no recipients `404`; over limit `429`; idempotency mismatch `409`; unexpected Redis/database errors generally `500`. Concurrent idempotency races can surface as generic constraint failures.

**Outcome:** sender gets `201` and a `MessageResponse` with `SENT`; recipients later receive realtime events asynchronously.

### 9.11 Publish and deliver a realtime event

After a transaction commits, `OutboxPublisher.publishPendingEvents` runs every 100 ms, selects up to 100 ready pending rows using `FOR UPDATE SKIP LOCKED`, publishes each serialized event to Redis Stream, and marks success `PUBLISHED`. Failures increment attempts, store a truncated error, and schedule exponential retry capped at 300 seconds; after ten attempts status becomes `FAILED`.

`MessageDeliveryWorker.consume` reads new records in consumer group `sandesh-delivery`. `recoverPending` retries current and stale pending records. It validates UUID/event type, uses `processed_events` with `INSERT ... ON CONFLICT DO NOTHING`, publishes supported events to Redis Pub/Sub, and acknowledges successful or malformed/unsupported records. Transient failures remain pending.

`RedisRealtimeSubscriber` deserializes Pub/Sub payloads and forwards it with `SimpMessagingTemplate.convertAndSendToUser` to `/queue/events`; clients subscribe to `/user/queue/events`. A failure after publishing to some recipients can cause duplicates on retry. Stream retention is unbounded because no trimming policy exists.

### 9.12 Synchronize messages

The authenticated member calls `GET /messages`. `MessageQueryService` clamps size, applies `max(afterSequence, hiddenAfterSequence)`, queries either latest visible rows descending or newer rows ascending, reverses initial results for chronological output, and maps receipt state only for messages sent by the requester.

An empty page returns no cursor. The initial-page `hasMore` refers to older history but the exposed cursor advances toward newer messages, so clients cannot reliably walk backward with the documented fields.

### 9.13 Acknowledge delivery/read

A recipient calls the receipt endpoint. `ReceiptService` verifies membership, message/conversation ownership, and the recipient’s receipt row, locks all receipts for the message, and applies a monotonic transition. `READ` also marks delivered and advances `last_read_sequence`. If aggregate state changes, an outbox event targets the sender.

Repeated acknowledgements are no-ops. The response aggregate is sender-oriented, so a recipient should not interpret it as their own message state.

### 9.14 WebSocket connect and typing

The client connects to `/ws`, sends a STOMP `CONNECT` native bearer header, then subscribes to `/user/queue/events`. `WebSocketAuthInterceptor` validates only the `CONNECT`; missing, malformed, invalid, or expired tokens throw `IllegalArgumentException`. Existing sockets are not revalidated on later messages.

Typing is sent to `/app/conversations/{conversationId}/typing` with `{ "typing": true|false }`. `TypingService` authenticates the principal, requires membership, resolves other members and the actor display name, and publishes a transient start/stop event directly through Redis Pub/Sub. Redis failure is not retried and no REST error envelope is defined.

## 10. API Reference

All protected HTTP requests use `Authorization: Bearer <JWT>`. JSON uses camelCase, UUIDs are strings, enum values are uppercase, and null response properties are omitted.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create account and issue token |
| POST | `/api/auth/login` | Public | Verify credentials and issue token |
| GET | `/api/users?q=` | Required | Search enabled users |
| POST | `/api/conversations` | Required | Create direct/group conversation |
| GET | `/api/conversations` | Required | List memberships/conversations |
| GET | `/api/conversations/{conversationId}` | Member | Get conversation |
| POST | `/api/conversations/{conversationId}/members` | Owner/admin | Add group member |
| DELETE | `/api/conversations/{conversationId}/members/{userId}` | Owner/admin | Remove group member |
| PATCH | `/api/conversations/{conversationId}` | Owner/admin | Rename group |
| DELETE | `/api/conversations/{conversationId}?permanent=` | Member or owner | Soft hide or permanently delete |
| POST | `/api/conversations/{conversationId}/messages` | Member | Send text message |
| GET | `/api/conversations/{conversationId}/messages?afterSequence=&size=` | Member | Synchronize messages |
| POST | `/api/conversations/{conversationId}/messages/{messageId}/receipts` | Member with receipt | Mark delivered/read |
| GET | `/api/messages/search?q=&size=` | Required | Search member-visible messages |

### Representative request/response DTOs

- `RegisterRequest(displayName, email, password)` → `AuthResponse`.
- `LoginRequest(email, password)` → `AuthResponse`.
- `CreateConversationRequest(type, title, memberIds)` → `ConversationResponse`.
- `AddMemberRequest(userId, role)` → `ConversationResponse`.
- `UpdateConversationRequest(title)` → `ConversationResponse`.
- `SendMessageRequest(clientMessageId, content)` → `MessageResponse`.
- `ReceiptRequest(type)` → `ReceiptResponse`.
- `MessagePageResponse(messages, nextAfterSequence, hasMore)`.
- `MessageSearchResultResponse(messageId, conversationId, senderId, content, createdAt, sequenceNumber)`.

### Error contract

`GlobalExceptionHandler` maps validation and domain failures to `ApiError(timestamp, status, code, message, path, violations)`. Known statuses are 400, 401, 403, 404, 409, 429, and generic 500. Missing parameters and database constraint races do not have equally specific mappings. WebSocket failures do not use `ApiError`.

## 11. Database Architecture and Data Lifecycle

### Final effective tables

| Table | Important columns/constraints | Lifecycle |
|---|---|---|
| `users` | UUID PK; display name 80; email 254 unique; password hash 100; enabled; created/updated timestamps | Created on registration; referenced by conversations/messages |
| `conversations` | UUID PK; type; nullable title; creator FK; next sequence; timestamps | Created by conversation creation; permanently deleted only for group-owner delete |
| `conversation_members` | UUID PK; conversation/user FKs; unique conversation/user; role; last read; joined; muted; hidden boundary | Added/removed with membership operations; cascades on conversation deletion |
| `messages` | UUID PK; conversation/sender FKs; unique sender/client ID; unique conversation/sequence; type/content/time | Created for user/system messages; cascades with conversation |
| `message_receipts` | UUID PK; message/user FKs; unique message/user; delivered/read timestamps | Created per active recipient; cascades with message |
| `outbox_events` | UUID PK; aggregate/event/payload/status/attempts/availability/timestamps/error | Created with domain writes; retained after conversation deletion; eventually published/failed |
| `processed_events` | Stream event UUID PK; processed timestamp | Deduplicates delivery-worker processing |

### Migration history

`db.changelog-master.yaml` executes 13 SQL changesets in order. Early migrations create/transmute users, legacy audit/device/user-safety structures, conversations/messages/receipts, and outbox support. Later migrations remove `device_registration`, `audit_events`, and `user_blocks`, leaving the seven final tables above.

The final schema has indexes for member lookup, conversation/sequence retrieval, receipts, outbox readiness, and legacy user-block lookup. The database does not enforce enum values, nonblank strings, role hierarchy, direct/group cardinality, or message-type semantics. Most timestamps are PostgreSQL `TIMESTAMP` while Java uses `Instant` with Hibernate UTC configuration.

### Transactions and concurrency

Authentication register/login, conversation mutations, message creation, and receipt updates are transactional at the service layer. Message creation locks the conversation row before sequence allocation. Outbox selection uses PostgreSQL `FOR UPDATE SKIP LOCKED` to support multiple publishers. Receipt updates lock all receipts for a message. Redis `INCR` is atomic for the distributed rate-limit path.

Database cascades apply from conversations to members/messages and from messages to receipts. User deletion cascades are defined for several foreign keys, but no user-deletion workflow exists. Outbox and processed events are intentionally independent of conversation/message foreign keys.

## 12. Authentication, Authorization, and Security

Passwords are hashed with BCrypt strength 12. JWTs are signed with an HMAC key created from the raw UTF-8 bytes of `SANDESH_JWT_SECRET`. The example comment says Base64, but `JwtService` does not Base64-decode the value; it must therefore be sufficiently long as raw text.

The JWT contains user UUID subject, email, issued-at, and expiration. There are no authorities in the authentication token. Application authorization is resource-based:

- Any authenticated user can use the user directory.
- Conversation members can read/send/acknowledge.
- Owners/admins manage group members and title.
- Only group owners permanently delete.

The JWT filter ignores malformed tokens after clearing the context, allowing Spring’s protected-route handling to produce 401. CSRF is disabled because the API is stateless. CORS allows configured origins, common methods, selected headers, credentials, and 3600-second preflight caching.

Security gaps include no refresh/revocation/token rotation, no current `enabled` lookup during JWT validation, STOMP token validation only at connect time, local plaintext credentials in ignored pgAdmin configuration, no TLS/reverse proxy configuration, and no application-wide roles or audit trail.

## 13. Frontend Architecture

No frontend code exists in this repository. The only frontend-facing artifact is `FRONTEND_INTEGRATION_CONTRACT.md`.

The expected client architecture is therefore inferred from the implemented contract:

1. Store the access token and attach it to every protected REST request.
2. Attach the same token to STOMP `CONNECT`.
3. Use the REST send response for the sender’s own message because `MESSAGE_CREATED` targets recipients only.
4. Use `clientMessageId` to reconcile retries and optimistic UI.
5. Use sequence numbers for ordering and synchronization.
6. Resynchronize conversations after WebSocket reconnect because events are not replayed.
7. Treat `receiptState` as sender-oriented.
8. Respect server responses for hidden conversations and role changes instead of duplicating backend rules.

The expected STOMP subscription is `/user/queue/events`. Supported durable event types include message creation, receipt changes, conversation creation/update/removal. Typing events are transient and include a five-second expiry hint.

## 14. Infrastructure, Configuration, and Deployment

`../backend/docker-compose.yaml` defines:

- PostgreSQL 16 Alpine on host port 5432, persistent `postgres_data`, health check via `pg_isready`.
- pgAdmin on port 5050, persistent `pgadmin_data`, mounted `pgadmin/servers.json`, dependent on healthy PostgreSQL.
- Redis 7 Alpine on port 6379, persistent `redis_data`, append-only file enabled, health check via `redis-cli ping`.

All services share `sandesh-network`. Compose does not build or run the backend.

`application.yaml` defaults HTTP to 8080, PostgreSQL to localhost database `sandesh`, Redis to localhost:6379, pool size 20/5, UTC Hibernate time zone, access-token TTL seven days, message limit 60/minute, and local WebSocket origins on ports 4173 and 4200. Environment variables are documented in `.env.example`.

There is no tracked Dockerfile, Kubernetes/Helm configuration, cloud deployment, TLS, reverse proxy, secrets manager, CI workflow, or release automation. The ignored local `.env` and pgAdmin configuration may contain credentials; this document intentionally does not reproduce them.

## 15. Testing and Reliability

No tracked tests exist under `src/test`, so there are no unit, repository, integration, API, WebSocket, end-to-end, concurrency, migration, or failure-injection tests. The Maven test lifecycle can complete successfully without executing tests.

Reliability mechanisms that are implemented:

- Transactional outbox prevents a committed domain write from depending on immediate WebSocket availability.
- Outbox retries use attempts, error storage, exponential backoff, and a ten-attempt terminal failure.
- Redis Stream consumer groups support pending-message recovery and stale-consumer claiming.
- `processed_events` makes event handling idempotent at the worker level.
- Message sequence and database uniqueness constraints protect ordering/idempotency.
- Pessimistic locks protect sequence allocation and receipt aggregation.
- Redis rate limiting uses atomic increment and has a local fallback.

Reliability limitations:

- Redis Pub/Sub is not durable and has no replay.
- Typing bypasses the outbox.
- A partial recipient publish can cause duplicates on stream retry.
- The Redis stream has no trimming/retention policy.
- Local rate-limit fallback is not cross-instance.
- Redis failures outside the specifically handled exception type are not normalized.
- Constraint races can become 500 responses.
- No automated regression coverage protects these behaviors.

## 16. Cross-Component Interactions

### Message write and realtime read model

`MessageController` → `MessageService` → `ConversationMemberRepository`/`ConversationRepository`/`MessageRepository`/`MessageReceiptRepository`/`OutboxRepository` → PostgreSQL commit → `OutboxPublisher` → Redis Stream → `MessageDeliveryWorker` → Redis Pub/Sub → `RedisRealtimeSubscriber` → STOMP user queue.

### Conversation membership mutation

`ConversationController` → `MembershipService` authorization → `ConversationService` mutation → optional `MessageService.sendSystemMessage` → `ConversationEventService` → outbox → same delivery pipeline. Removal additionally invokes `ReceiptService.recalculateForRemovedMember`.

### Authentication

REST request → `JwtAuthenticationFilter` → `JwtService.parse` → `ChatPrincipal` → controller `Authentication` → `CurrentUser`/membership service. STOMP follows the same token service at `CONNECT`, but not for every subsequent frame.

### Database visibility

`ConversationMember.hiddenAfterSequence` influences message sync and latest previews. The same boundary is not applied by message search, and list/detail filtering has additional asymmetric logic. This is a cross-component invariant clients must not attempt to recreate independently.

## 17. Important Business Rules and System Invariants

- Emails are normalized to stripped lowercase values by the application.
- A creator is always a conversation member.
- Direct conversations have exactly two total members and cannot be expanded or renamed.
- Groups have at least three total members at creation.
- Conversation membership is unique per user/conversation.
- Message sequences are unique within a conversation and allocated while the conversation is locked.
- Client message IDs are unique per sender across conversations.
- Message content is stripped and bounded to 4000 characters.
- Only current recipients get receipt rows for a message.
- Receipt transitions never regress.
- Read acknowledgement advances member `last_read_sequence`.
- A newly joined member cannot see history at or before their hidden boundary.
- Only owners/admins manage group members and titles.
- Only a group owner permanently deletes.
- REST persistence does not imply realtime delivery has completed.
- Realtime clients must resynchronize after reconnect.

## 18. Design Decisions and Implementation Details

The transactional outbox is the primary consistency decision: domain writes and notification intent commit together, while Redis/WebSocket delivery is asynchronous. Redis Streams add consumer-group recovery beyond simple Pub/Sub, and `processed_events` provides application-level idempotency.

Sequence-based synchronization avoids timestamp ordering ambiguity and allows a client to request messages after a known position. Pessimistic conversation locking ensures sequence allocation is serialized. Client-generated message IDs provide safe retry behavior.

Soft hiding uses a sequence boundary rather than deleting historical rows, allowing a user’s view to start after the point at which they hid or joined. The implementation is not fully uniform because search and some conversation list/detail paths apply different visibility rules.

The service/repository split keeps domain decisions in services and SQL/query shape in repositories. DTOs prevent direct entity exposure. Generic error mapping avoids leaking stack traces, but some validation and constraint cases remain broad.

## 19. Known Limitations and Implementation Gaps

1. No automated tests are present.
2. No frontend implementation is present.
3. No backend container, production deployment, CI/CD, TLS, reverse proxy, or secret-management configuration is present.
4. No refresh token, logout, revocation, or token rotation exists.
5. Existing JWTs remain usable after a user is disabled.
6. JWT documentation says Base64 while implementation consumes raw UTF-8 secret bytes.
7. STOMP authentication is not rechecked after `CONNECT`.
8. Search can expose soft-hidden message history.
9. Initial message pagination exposes a forward cursor while `hasMore` describes older results.
10. Hidden conversation list/detail/preview behavior is asymmetric.
11. Any manager can assign `OWNER`; role hierarchy and role changes are incomplete.
12. There is no rejoin operation after soft hiding.
13. `muted_until` is stored but unused.
14. `CONVERSATION_MEMBER_ADDED` and `CONVERSATION_MEMBER_REMOVED` enum values are not emitted by current workflows.
15. Typing is volatile and bypasses durable delivery.
16. Redis stream retention is unbounded.
17. Partial fan-out can duplicate events.
18. Per-JVM rate-limit fallback is not distributed.
19. Database uniqueness/concurrency failures are not consistently mapped to domain errors.
20. No database checks enforce enum values or most business rules.
21. Legacy migrations create and then remove audit, device, and block structures; those features are not active.
22. No application audit trail or user-block behavior exists.
23. A removed-member receipt recalculation appears to populate an inconsistent `userId`.

## 20. Complete End-to-End System Walkthrough

1. An operator starts PostgreSQL and Redis with Compose and starts the Spring backend separately.
2. Liquibase creates/transforms the database; Hibernate validates it.
3. A client registers or logs in and receives a JWT.
4. The client attaches the bearer token to REST calls and authenticates a STOMP connection.
5. The client searches enabled users and creates a direct or group conversation.
6. The conversation service writes memberships and, for groups, a system message and event.
7. The client loads the conversation and synchronizes visible messages by sequence.
8. A sender submits a client-generated ID and text. The service checks idempotency, rate limits, locks the conversation, allocates a sequence, writes the message/receipts/outbox row, and returns the message.
9. Scheduled workers publish the outbox event through Redis Stream and Pub/Sub to each recipient’s STOMP queue.
10. Recipients acknowledge delivery/read. Locked receipt transitions update PostgreSQL and generate sender-targeted events when aggregate state changes.
11. Typing travels directly from STOMP through Redis Pub/Sub and is not persisted.
12. Members may add/remove group members, rename groups, or soft hide conversations; owners may permanently delete groups.
13. If Redis/WebSocket delivery fails or the client reconnects, REST synchronization is the recovery mechanism. No event replay endpoint exists.

## 21. Glossary and Codebase Reference Index

### Glossary

- **Conversation sequence**: Monotonically increasing per-conversation message number used for ordering and synchronization.
- **Hidden boundary**: `conversation_members.hidden_after_sequence`; messages at or before it are hidden from synchronization.
- **Transactional outbox**: Database rows created with domain writes and published asynchronously.
- **Redis Stream**: Durable-ish asynchronous handoff using consumer groups and pending-entry recovery.
- **Pub/Sub**: Redis transient fan-out to application instances.
- **Receipt aggregate**: Sender-facing state derived from active recipient receipt rows.
- **System message**: Persisted `MessageType.SYSTEM` text generated by conversation operations.

### Reference index

| Concern | Primary files |
|---|---|
| Bootstrap | `../backend/src/main/java/com/aadi/sandesh/SandeshApplication.java` |
| HTTP security | `config/SecurityConfig.java`, `security/JwtAuthenticationFilter.java`, `security/JwtService.java` |
| WebSocket security | `config/WebSocketConfig.java`, `config/WebSocketAuthInterceptor.java` |
| REST controllers | `controller/*.java` |
| Domain | `domain/*.java`, `domain/enums/*.java` |
| DTO contract | `dto/**/*.java` |
| Business services | `service/*.java` |
| Persistence queries | `repository/*.java` |
| Realtime pipeline | `messaging/*.java`, `service/MessageDeliveryWorker.java` |
| Error mapping | `exception/*.java` |
| Runtime config | `src/main/resources/application.yaml`, `.env.example` |
| Schema | `src/main/resources/db/changelog/*.sql`, `db.changelog-master.yaml` |
| Local dependencies | `../backend/docker-compose.yaml` |
| Client contract | `FRONTEND_INTEGRATION_CONTRACT.md` |

This analysis is based on the tracked source/configuration and the existing integration contract. Generated IDE metadata, ignored credentials, and irrelevant dependency internals were excluded; no secret values are reproduced.
