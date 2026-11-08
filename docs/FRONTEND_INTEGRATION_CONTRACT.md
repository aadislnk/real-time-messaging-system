# Sandesh Frontend Integration Contract

**Backend source:** `../backend/src/main/java/com/aadi/sandesh` and Liquibase migrations  
**Base URL:** the deployed backend origin (local default `http://localhost:8080`)  
**Document status:** derived from the current implementation; where behavior is ambiguous or incomplete, this document says so rather than inventing a contract.

## 1. Product overview

Sandesh is a stateless, JWT-authenticated real-time messaging application. Users can register and log in, find other enabled users, create direct or group conversations, exchange text messages, receive message delivery/read state, search visible messages, and receive conversation/message/typing updates over STOMP WebSockets.

### Roles

There are no application-wide roles. Roles exist per conversation:

| Role | Capabilities implemented |
|---|---|
| `OWNER` | Conversation creator; can manage members and group title; can permanently delete a group. |
| `ADMIN` | Can manage members and group title. |
| `MEMBER` | Can read/send/acknowledge receipts; cannot manage members/title or permanently delete. |

`POST /api/conversations/{id}/members` accepts any `MemberRole`, including `OWNER`; the backend does not prevent creating multiple owners or otherwise enforce a role hierarchy.

### Main workflows

1. Register or log in to receive a bearer JWT.
2. Search users with `GET /api/users?q=...`.
3. Create a `DIRECT` conversation with exactly one other user, or a `GROUP` with at least two other users. The creator is automatically added as `OWNER`; supplied users are initially `MEMBER`.
4. Load conversations and messages. Message synchronization is sequence-based, not timestamp-based.
5. Send text with a unique client-generated `clientMessageId`. Repeating the same ID with the same sender, conversation, and stripped content returns the original message (idempotent retry).
6. Connect to `/ws`, authenticate the STOMP `CONNECT`, subscribe to `/user/queue/events`, and update the UI from realtime events. REST synchronization remains necessary after reconnects because no event replay API is exposed.
7. A recipient acknowledges `DELIVERED` or `READ`; read acknowledgement also advances that member’s `lastReadSequence`.
8. A user can leave/hide a conversation (`permanent=false`). A group owner can permanently delete the group for everyone.

## 2. JSON and common conventions

- JSON property names are camelCase.
- UUIDs are strings in canonical UUID format.
- `Instant` values are ISO-8601 timestamps, normally UTC, for example `2026-11-07T13:55:00Z`.
- Enum values are uppercase strings.
- Null-valued response properties are omitted because Jackson is configured with `non_null`.
- Successful list responses are JSON arrays, including an empty array.
- Request bodies must be JSON and should use `Content-Type: application/json`.
- Authenticated HTTP calls use `Authorization: Bearer <accessToken>`.
- No CSRF token is required; the API is stateless.

## 3. Authentication

### `POST /api/auth/register`

Creates an enabled account. **Status:** `201 Created`.

Request:

```json
{
  "displayName": "Aadi",
  "email": "aadi@example.com",
  "password": "at-least-10-characters"
}
```

Rules:

- `displayName`: required/non-blank, maximum 80 characters.
- `email`: required/non-blank, valid Jakarta email format, maximum 254 characters.
- `password`: required/non-blank, 10–72 characters.
- Email is stored stripped and lower-cased. Duplicate email checks are case-insensitive.

### `POST /api/auth/login`

Request:

```json
{
  "email": "aadi@example.com",
  "password": "at-least-10-characters"
}
```

`email` is required and must be a valid email; `password` is required but has no length constraint on login. **Status:** `200 OK`.

Successful response (both register and login):

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-14T13:55:00Z",
  "userId": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
  "displayName": "Aadi",
  "email": "aadi@example.com"
}
```

The default token TTL is seven days (`SANDESH_ACCESS_TOKEN_TTL` can change it). The JWT subject is the user ID and it also contains an `email` claim. There is no refresh-token or logout endpoint; the frontend must discard the token locally to log out.

## 4. Users

### `GET /api/users`

Requires authentication. Query parameter `q` is optional and defaults to an empty string:

```http
GET /api/users?q=adi
Authorization: Bearer <token>
```

Search is case-insensitive against enabled users’ display names and emails. The current user is excluded. Results are capped at 50 and sorted by lower-case display name.

Response:

```json
[
  {
    "id": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
    "displayName": "Aadi",
    "email": "aadi@example.com"
  }
]
```

## 5. Conversations

### Conversation schemas

`ConversationType`: `DIRECT` or `GROUP`.

```json
{
  "id": "1ce5c7f5-81e2-46d3-bc5f-222222222222",
  "type": "GROUP",
  "title": "Project team",
  "createdBy": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
  "createdAt": "2026-11-07T13:55:00Z",
  "members": [
    {
      "userId": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
      "displayName": "Aadi",
      "role": "OWNER",
      "lastReadSequence": 0
    }
  ],
  "latestMessagePreview": "Aadi started the conversation"
}
```

`title` is nullable/omitted for direct conversations; `latestMessagePreview` is nullable/omitted when there is no visible latest message.

### `POST /api/conversations`

Requires authentication. **Status:** `201 Created`.

Request:

```json
{
  "type": "GROUP",
  "title": "Project team",
  "memberIds": [
    "11111111-1111-1111-1111-111111111111",
    "22222222-2222-2222-2222-222222222222"
  ]
}
```

Rules:

- `type`: required.
- `memberIds`: required, non-empty set, maximum 250 UUIDs. The creator is added automatically; duplicate IDs collapse because the backend uses a set.
- `title`: maximum 120 characters. It is only retained for groups. A blank title is accepted at bean validation time and stored as null on creation.
- A direct conversation must contain exactly two total members, so the request normally contains exactly one other user.
- A group must contain at least three total members, so the request normally contains at least two other users.
- Creating a duplicate direct conversation returns the existing conversation with `200 OK` (despite the create endpoint normally returning `201`).
- All supplied users must exist.
- Group creation emits a `SYSTEM` message such as `"{displayName} started the conversation"`.

### `GET /api/conversations`

Returns the authenticated user’s visible conversation list, ordered by membership join time descending. **Status:** `200 OK`.

### `GET /api/conversations/{conversationId}`

Returns one conversation if the user is a member. **Status:** `200 OK`.

### `POST /api/conversations/{conversationId}/members`

Requires `OWNER` or `ADMIN`. Request:

```json
{
  "userId": "33333333-3333-3333-3333-333333333333",
  "role": "MEMBER"
}
```

Both fields are required. Only groups support adding members. The new member is hidden from existing history through the current latest sequence and receives future messages. A system message `"{displayName} joined"` is created. **Status:** `200 OK`, returning the updated conversation.

### `DELETE /api/conversations/{conversationId}/members/{userId}`

Requires `OWNER` or `ADMIN`. Only groups support removal. The actor cannot remove themself; an owner cannot be removed. A system message is created for remaining members and the removed user receives a `CONVERSATION_REMOVED` event. **Status:** `200 OK`, returning the updated conversation.

### `PATCH /api/conversations/{conversationId}`

Requires `OWNER` or `ADMIN`; direct conversations cannot be renamed. Request:

```json
{ "title": "Renamed project" }
```

`title` has a maximum of 120 characters. The domain requires non-null/non-blank titles, so a blank or null value results in `400 BAD_REQUEST`; use a non-blank group title. A system message describing the rename is emitted. **Status:** `200 OK`.

### `DELETE /api/conversations/{conversationId}?permanent=false`

Requires membership. With the default `permanent=false`, the user is soft-hidden from the conversation at its current latest sequence and receives a removal event. This is the leave/hide operation; it does not delete messages or the conversation. **Status:** `204 No Content`.

With `permanent=true`, only a group `OWNER` may delete. All members receive removal events and the conversation/messages/members are deleted. **Status:** `204 No Content`.

## 6. Messages and synchronization

### `POST /api/conversations/{conversationId}/messages`

Requires membership. **Status:** `201 Created`.

Request:

```json
{
  "clientMessageId": "web-01J9Z8NQ7M2K",
  "content": "Hello"
}
```

Rules:

- `clientMessageId`: required/non-blank, maximum 100 characters; unique per sender across all conversations.
- `content`: required/non-blank, maximum 4000 characters. It is stripped before storage/comparison.
- Only `TEXT` messages can be sent by this endpoint.
- The sender must have at least one other current recipient; otherwise the backend returns not found.
- The default rate limit is 60 sends per user per minute (`SANDESH_RATE_LIMIT_MESSAGES_PER_MINUTE`).
- Reusing a client ID with different content or conversation returns `409 CONFLICT`.
- Reusing it with identical sender/content/conversation returns the original response and does not create a duplicate.

Response:

```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "conversationId": "1ce5c7f5-81e2-46d3-bc5f-222222222222",
  "senderId": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
  "clientMessageId": "web-01J9Z8NQ7M2K",
  "sequenceNumber": 1,
  "type": "TEXT",
  "content": "Hello",
  "createdAt": "2026-11-07T13:55:00Z",
  "receiptState": "SENT"
}
```

`receiptState` is included only when the requesting user is the sender. It is an aggregate state over active recipients: `SENT` until all are delivered, `DELIVERED` until all are read, then `READ`. For messages received by the current user it is omitted.

### `GET /api/conversations/{conversationId}/messages`

Requires membership.

```http
GET /api/conversations/{conversationId}/messages?afterSequence=0&size=50
```

- `afterSequence` defaults to `0`.
- `size` defaults to `50` and is clamped to 1–100.
- `afterSequence=0` returns the latest page in chronological order.
- `afterSequence>0` returns messages with sequence strictly greater than the effective cursor, in ascending sequence order.
- After a leave/rejoin-style membership hide, history at or before `hiddenAfterSequence` is excluded.

Response:

```json
{
  "messages": [],
  "nextAfterSequence": 12,
  "hasMore": true
}
```

Use the last returned message’s `sequenceNumber` as the next `afterSequence`. `nextAfterSequence` is null when the page is empty.

### `POST /api/conversations/{conversationId}/messages/{messageId}/receipts`

Requires membership and a receipt row for the current user. Request:

```json
{ "type": "DELIVERED" }
```

`type` is required and is `DELIVERED` or `READ`.

- `DELIVERED` sets `deliveredAt` only if neither delivered nor read is already set.
- `READ` sets both delivery/read as needed and advances the user’s conversation `lastReadSequence`.
- Transitions are monotonic and repeated acknowledgements are no-ops.
- The response’s `receiptState` is calculated for the message sender, so a non-sender may receive a response whose aggregate state is not useful for their own UI.

Response:

```json
{
  "messageId": "44444444-4444-4444-4444-444444444444",
  "userId": "55555555-5555-5555-5555-555555555555",
  "deliveredAt": "2026-11-07T13:56:00Z",
  "readAt": "2026-11-07T13:57:00Z",
  "receiptState": "READ"
}
```

## 7. Message search

### `GET /api/messages/search`

Requires authentication:

```http
GET /api/messages/search?q=deployment&size=20
Authorization: Bearer <token>
```

`q` is required by Spring MVC but has no non-blank validation. `size` defaults to 50 and is clamped to 1–50. Results are sorted newest first and include messages from conversations in which the user is a member.

```json
[
  {
    "messageId": "44444444-4444-4444-4444-444444444444",
    "conversationId": "1ce5c7f5-81e2-46d3-bc5f-222222222222",
    "senderId": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
    "content": "Deployment is complete",
    "createdAt": "2026-11-07T13:55:00Z",
    "sequenceNumber": 12
  }
]
```

## 8. WebSocket/STOMP contract

### Connection

- STOMP endpoint: `/ws` on the backend origin.
- Allowed origins default to `http://localhost:4173,http://localhost:4200`; deployment controls this with `SANDESH_WS_ALLOWED_ORIGINS`.
- The HTTP security rule permits the endpoint, but the STOMP `CONNECT` must include a native header `Authorization: Bearer <JWT>`.
- Missing/non-Bearer/invalid/expired credentials cause the CONNECT to be rejected. The interceptor throws an exception; a JSON `ApiError` WebSocket frame is not defined.

Example STOMP flow:

```text
CONNECT
accept-version:1.2
Authorization:Bearer eyJ...

SUBSCRIBE
id:events
destination:/user/queue/events
```

The server sends user-targeted events to `/user/queue/events`. Event envelope:

```json
{
  "type": "MESSAGE_CREATED",
  "targetUserId": "55555555-5555-5555-5555-555555555555",
  "conversationId": "1ce5c7f5-81e2-46d3-bc5f-222222222222",
  "actorUserId": "8b0d7c3a-1e4c-4c79-9a5d-4c3b39f1a111",
  "messageId": "44444444-4444-4444-4444-444444444444",
  "payload": {},
  "occurredAt": "2026-11-07T13:55:00Z"
}
```

Possible event types:

| Type | Payload |
|---|---|
| `MESSAGE_CREATED` | `MessageResponse`; sent to each recipient, not the sender. |
| `MESSAGE_DELIVERED` / `MESSAGE_READ` | `ReceiptResponse`. |
| `CONVERSATION_CREATED` / `CONVERSATION_UPDATED` | `ConversationResponse`. |
| `CONVERSATION_REMOVED` | Object with `conversationId` and either `removed: true` or `permanent: true`. |
| `TYPING_STARTED` / `TYPING_STOPPED` | Object with `actorDisplayName` and `expiresInSeconds: 5`. |

### Client-to-server typing

The client sends a STOMP message to:

```text
SEND
destination:/app/conversations/{conversationId}/typing
content-type:application/json

{"typing":true}
```

`typing` is required and boolean. The sender must be a conversation member. The backend broadcasts start/stop events to all other members. No typing state is persisted; the frontend should expire the indicator after the supplied five seconds.

## 9. Errors and status codes

Most REST errors use:

```json
{
  "timestamp": "2026-11-07T13:58:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed for one or more fields",
  "path": "/api/auth/register",
  "violations": [
    { "field": "password", "message": "size must be between 10 and 72" }
  ]
}
```

`violations` is an empty array for non-validation errors. Known mappings:

| Status | Code(s) | Typical causes |
|---:|---|---|
| `400` | `VALIDATION_ERROR`, `INVALID_PARAMETER`, `MALFORMED_REQUEST`, `BAD_REQUEST` | Bean validation, invalid UUID/query/path value, malformed JSON, blank group title, illegal argument. |
| `401` | `UNAUTHORIZED` | Missing/invalid/expired bearer token or invalid credentials. |
| `403` | `FORBIDDEN` | Not a member, or member lacks owner/admin management permission. |
| `404` | `NOT_FOUND` | Conversation/message/user/member/recipient does not exist. |
| `409` | `CONFLICT` | Duplicate email/direct conversation constraints, duplicate member, invalid direct/group operation, client ID reuse conflict, invalid permanent-delete request. |
| `429` | `RATE_LIMIT_EXCEEDED` | More than the configured message sends per minute. |
| `500` | `INTERNAL_SERVER_ERROR` | Unexpected backend failure; generic message only. |
| `204` | — | Successful conversation hide/delete; no body. |

For browser clients, CORS allows `Authorization`, `Cache-Control`, `Content-Type`, and `X-Request-Id`; methods include `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, and `OPTIONS`; credentials are allowed. The backend does not itself generate or document an `X-Request-Id`.

## 10. Frontend implementation constraints

- Persist the access token securely for the target platform and attach it to every protected REST request and STOMP `CONNECT`.
- Treat `expiresAt` as authoritative for proactive token expiry; there is no refresh endpoint.
- Generate a stable unique client message ID before sending and retain it across retries.
- Reconcile optimistic messages by `clientMessageId`/message ID and sequence number. `MESSAGE_CREATED` is delivered only to recipients, so the sender must use the REST response for its own message.
- On WebSocket reconnect, reload conversations and synchronize each open conversation from the last known sequence. Events are not guaranteed to be replayed to a newly connected client.
- Do not infer receipt details for received messages from `MessageResponse.receiptState`; it is sender-only.
- Use server sequence numbers for ordering and cursors. Do not use client timestamps as ordering keys.
- Hide/manage UI actions based on the current member’s role and conversation type, but still handle `403`/`409` because the server is authoritative.
- Direct conversation titles are not supported. Group title creation/update should use a non-blank value.
- A removed/soft-hidden user may still be a database member with history hidden; refresh the conversation list after leave, add, remove, or delete events.

## 11. Known ambiguities and backend gaps

1. `CONVERSATION_MEMBER_ADDED` and `CONVERSATION_MEMBER_REMOVED` exist in `EventType`, but the implemented conversation service emits `CONVERSATION_UPDATED` for remaining members and `CONVERSATION_REMOVED` for a removed member. Clients should not wait for the unused event names.
2. There is no endpoint to change an existing member’s role, mute a conversation, fetch individual receipt details, refresh a token, or explicitly leave a direct conversation other than soft deletion.
3. Message search joins membership but does not apply `hiddenAfterSequence`; unlike message synchronization, it may return older messages from a conversation a user hid/rejoined.
4. `GET /api/conversations` has special hidden-conversation filtering logic that can retain some soft-hidden conversations (for example groups or conversations with messages). The exact UI visibility should follow the returned list, not a client-side recreation.
5. `POST /api/conversations` may return `200` for an existing direct conversation and `201` for a new one.
6. Realtime delivery depends on Redis outbox/stream workers. REST writes can succeed before the WebSocket event is delivered; UI state must not depend solely on realtime delivery.
7. WebSocket authorization is checked only during STOMP `CONNECT`; token expiry while a socket remains open is not revalidated by this code.
8. The initial migration creates legacy user columns and the next migration transforms them to match the entity. A clean database must run the full Liquibase changelog in order; frontend behavior is unaffected, but partial/manual schema setup is unsupported.
