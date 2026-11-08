# Sandesh Frontend Implementation Plan

This plan is based on the implemented Spring Boot backend, `docs/FRONTEND_INTEGRATION_CONTRACT.md`, `docs/PROJECT_ANALYSIS.md`, and the actual REST, DTO, security, and STOMP code.

The frontend will use **React with JavaScript**, **Vite**, native browser APIs, and only focused dependencies:

- `react`
- `react-dom`
- `react-router-dom`
- `@stomp/stompjs`
- A test runner such as `vitest` with React Testing Library for the test phase

The frontend is not present in the repository today. The client described below is an external React application under `frontend/`.

## Backend Contract Summary

### REST APIs

| Area | Endpoint |
|---|---|
| Auth | `POST /api/auth/register` |
| Auth | `POST /api/auth/login` |
| Users | `GET /api/users?q=` |
| Conversations | `POST /api/conversations` |
| Conversations | `GET /api/conversations` |
| Conversations | `GET /api/conversations/{conversationId}` |
| Membership | `POST /api/conversations/{conversationId}/members` |
| Membership | `DELETE /api/conversations/{conversationId}/members/{userId}` |
| Conversation | `PATCH /api/conversations/{conversationId}` |
| Conversation | `DELETE /api/conversations/{conversationId}?permanent=` |
| Messages | `POST /api/conversations/{conversationId}/messages` |
| Messages | `GET /api/conversations/{conversationId}/messages?afterSequence=&size=` |
| Receipts | `POST /api/conversations/{conversationId}/messages/{messageId}/receipts` |
| Search | `GET /api/messages/search?q=&size=` |

### STOMP/WebSocket

- Connection: `/ws`
- Native `CONNECT` header: `Authorization: Bearer <accessToken>`
- User subscription: `/user/queue/events`
- Typing destination: `/app/conversations/{conversationId}/typing`
- Event types implemented for clients:
  - `MESSAGE_CREATED`
  - `MESSAGE_DELIVERED`
  - `MESSAGE_READ`
  - `CONVERSATION_CREATED`
  - `CONVERSATION_UPDATED`
  - `CONVERSATION_REMOVED`
  - `TYPING_STARTED`
  - `TYPING_STOPPED`

The backend has no refresh-token, logout, event-replay, role-change, mute, push-notification, or direct-conversation rejoin endpoints. The frontend must treat PostgreSQL-backed REST responses as authoritative and resynchronize messages after WebSocket reconnects.

---

# Phase 1 — Vite Foundation and Application Shell

## Objectives and features

- Create the lean React/Vite application.
- Establish responsive, polished messaging-app visual foundations.
- Create public/authenticated route boundaries.
- Build the desktop and mobile application shell.
- Add reusable UI primitives only where they are used by multiple features.

## Tasks in implementation order

1. Create the Vite React JavaScript application under `frontend/`.
2. Configure development environment variables:
   - `VITE_API_BASE_URL`, defaulting locally to `http://localhost:8080`.
   - `VITE_WS_URL`, defaulting locally to `ws://localhost:8080/ws`.
3. Configure React Router.
4. Add the global CSS reset, color tokens, typography, spacing, responsive breakpoints, and dark/light-ready surface tokens.
5. Build the public page frame and authenticated `AppShell`.
6. Add the conversation-sidebar region, content region, mobile header, and navigation controls.
7. Add shared loading, empty, error, modal, button, input, avatar, badge, and toast styles only as needed.
8. Add route placeholders for login, registration, and chat.

## Components, pages, and files

```text
frontend/
├── package.json
├── index.html
├── vite.config.js
├── .env.example
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── app/
    │   ├── router.jsx
    │   └── config.js
    ├── components/
    │   ├── AppShell.jsx
    │   ├── Button.jsx
    │   ├── Dialog.jsx
    │   ├── EmptyState.jsx
    │   ├── ErrorState.jsx
    │   ├── LoadingState.jsx
    │   └── Toast.jsx
    ├── pages/
    │   ├── LoginPage.jsx
    │   ├── RegisterPage.jsx
    │   └── ChatPage.jsx
    └── styles/
        ├── globals.css
        └── tokens.css
```

Avoid creating separate files for one-off wrappers or speculative features.

## Backend integration

No API integration is required in this phase. Only backend origin configuration is introduced.

## Dependencies and prerequisites

- Node.js and npm.
- React, React DOM, Vite, and React Router.
- No state-management library is required yet.

## Testing and completion criteria

- `npm run dev` starts the application.
- `npm run build` succeeds.
- Public and chat route placeholders render.
- Desktop, tablet, and mobile layouts are usable.
- Shared controls have visible focus states and keyboard support.
- No backend API or unsupported UI feature is invented.

---

# Phase 2 — Authentication, Session, and Typed REST Client

## Objectives and features

- Implement registration and login.
- Store and restore the authenticated session.
- Attach bearer tokens to protected HTTP requests.
- Protect authenticated routes.
- Handle token expiration and local logout.
- Normalize backend errors into usable form and toast messages.

## Tasks in implementation order

1. Define JavaScript API model conventions and response normalization.
2. Create a small `fetch`-based HTTP client.
3. Add automatic `Authorization: Bearer ...` headers.
4. Add shared response/error handling for the backend `ApiError` shape.
5. Create a minimal auth context or reducer; do not add a store library unless later complexity justifies it.
6. Implement registration form and API call.
7. Implement login form and API call.
8. Restore the stored `AuthResponse` on startup.
9. Use `expiresAt` for proactive expiry handling.
10. Implement logout by disconnecting realtime later, clearing session data, and redirecting to login.
11. Add `ProtectedRoute` and public-only route behavior.

## Components and files

```text
src/
├── api/
│   ├── httpClient.js
│   └── apiError.js
├── auth/
│   ├── AuthProvider.jsx
│   ├── authStorage.js
│   ├── authApi.js
│   ├── LoginForm.jsx
│   └── RegisterForm.jsx
├── routing/
│   ├── ProtectedRoute.jsx
│   └── PublicOnlyRoute.jsx
└── pages/
    ├── LoginPage.jsx
    └── RegisterPage.jsx
```

## Backend APIs

### Registration

```http
POST /api/auth/register
```

Request constraints:

- `displayName`: required, maximum 80.
- `email`: required, valid email, maximum 254.
- `password`: required, 10–72 characters.

### Login

```http
POST /api/auth/login
```

Response fields:

```js
{
  accessToken,
  tokenType,
  expiresAt,
  userId,
  displayName,
  email
}
```

There is no backend logout endpoint. Local logout must discard the token and cached authenticated data.

## Error behavior

Handle:

- `400 VALIDATION_ERROR`: show field violations.
- `401 UNAUTHORIZED`: show generic invalid-credentials/session-expired feedback.
- `403 FORBIDDEN`: show access-denied feedback.
- `404 NOT_FOUND`, `409 CONFLICT`, `429 RATE_LIMIT_EXCEEDED`, and `500 INTERNAL_SERVER_ERROR`.

Do not log passwords, tokens, or authorization headers.

## Dependencies and prerequisites

- Phase 1 shell.
- React context/reducer or a similarly small session mechanism.
- Browser storage adapter.

## Testing and completion criteria

- Valid registration creates a session and enters the chat shell.
- Login handles success and generic invalid-credential failure.
- Client validation mirrors backend limits.
- Protected routes redirect unauthenticated users.
- Every protected request sends the bearer token.
- Expired sessions are cleared and redirected to login.
- Logout removes the token and cached user data.
- API validation errors render field-specific feedback.

---

# Phase 3 — Users, Conversations, and Conversation Management

## Objectives and features

Implement navigation and conversation lifecycle:

- Enabled-user search.
- Direct conversation creation.
- Group conversation creation.
- Conversation list/detail loading.
- Role-aware member management.
- Group rename.
- Soft hide and permanent group deletion.

## Tasks in implementation order

1. Define response shapes for users, conversations, and members.
2. Implement the user and conversation API modules.
3. Load the authenticated user’s conversations.
4. Build the conversation list with preview, avatar, active, empty, and loading states.
5. Build user search with debouncing and empty results.
6. Implement direct-conversation creation.
7. Implement group creation with title and selected members.
8. Load conversation details when selected.
9. Add role-aware conversation actions.
10. Implement group member addition/removal.
11. Implement group rename.
12. Implement soft hide and permanent delete confirmations.
13. Invalidate or refresh conversation data after each mutation.

## Components and files

```text
src/
├── users/
│   ├── userApi.js
│   ├── UserSearch.jsx
│   └── UserSearchResults.jsx
└── conversations/
    ├── conversationApi.js
    ├── ConversationList.jsx
    ├── ConversationListItem.jsx
    ├── ConversationHeader.jsx
    ├── CreateConversationDialog.jsx
    ├── ConversationMembers.jsx
    ├── AddMemberDialog.jsx
    ├── RenameGroupDialog.jsx
    └── ConversationActions.jsx
```

## Backend APIs

### User search

```http
GET /api/users?q={query}
```

The backend excludes the current user, returns enabled users only, caps results at 50, and sorts by display name.

### Conversation creation

```http
POST /api/conversations
```

Direct:

```json
{
  "type": "DIRECT",
  "memberIds": ["other-user-id"]
}
```

Group:

```json
{
  "type": "GROUP",
  "title": "Project team",
  "memberIds": ["user-id-1", "user-id-2"]
}
```

The creator is automatically included by the backend. A direct conversation requires exactly two total members. A group requires at least three. Duplicate direct creation may return the existing conversation.

### Conversation operations

```http
GET /api/conversations
GET /api/conversations/{conversationId}
POST /api/conversations/{conversationId}/members
DELETE /api/conversations/{conversationId}/members/{userId}
PATCH /api/conversations/{conversationId}
DELETE /api/conversations/{conversationId}?permanent=false
DELETE /api/conversations/{conversationId}?permanent=true
```

The UI should expose member/title management only to `OWNER` or `ADMIN`, and permanent deletion only to a group `OWNER`. The backend remains authoritative and may return `403` or `409`.

Do not build role-change, mute, blocking, or rejoin controls because no corresponding API exists.

## Dependencies and prerequisites

- Phase 2 authenticated HTTP client.
- Current user ID.
- Dialog, form, toast, and list components from previous phases.

## Testing and completion criteria

- User search debounces requests and shows loading/empty/error states.
- Direct creation selects one other user.
- Group creation requires at least two other users in the UI.
- Conversation list and detail data load correctly.
- Management controls reflect conversation type and current member role.
- Add/remove/rename/hide/delete flows refresh server state.
- `403`, `404`, and `409` responses are displayed clearly.
- The sidebar and conversation detail remain usable on mobile.

---

# Phase 4 — Messages, Synchronization, and Receipts

## Objectives and features

Implement the core chat experience:

- Message history.
- Text composer.
- Optimistic sending.
- Stable client-message idempotency.
- Sequence-based synchronization.
- System-message rendering.
- Delivery/read acknowledgements.
- Message status and retry UI.

## Tasks in implementation order

1. Implement message API functions.
2. Build message timeline and message bubble components.
3. Build the composer with client-side limits.
4. Generate a stable `clientMessageId` before each send.
5. Add optimistic local messages.
6. Reconcile optimistic messages with the REST response by client ID/message ID.
7. Implement initial synchronization with `afterSequence=0`.
8. Track the highest known sequence per conversation.
9. Implement incremental synchronization with positive `afterSequence`.
10. Add delivered/read receipt calls for received messages.
11. Render sender-only aggregate receipt state.
12. Add system-message styling and send failure retry.
13. Handle conversation switches and stale requests.

## Components and files

```text
src/messages/
├── messageApi.js
├── MessageTimeline.jsx
├── MessageBubble.jsx
├── SystemMessage.jsx
├── MessageComposer.jsx
├── MessageStatus.jsx
├── useMessages.js
├── useSendMessage.js
├── messageStore.js
└── clientMessageId.js
```

## Backend APIs

### Send

```http
POST /api/conversations/{conversationId}/messages
```

```json
{
  "clientMessageId": "web-generated-unique-id",
  "content": "Hello"
}
```

The client ID must remain unchanged across retries. The backend strips content, enforces a 4,000-character limit, requires a recipient, and applies a default 60-message-per-minute user rate limit.

### Synchronize

```http
GET /api/conversations/{conversationId}/messages?afterSequence=0&size=50
```

The backend returns:

```js
{
  messages,
  nextAfterSequence,
  hasMore
}
```

Use server `sequenceNumber` for ordering. Initial requests return the latest visible page; positive cursors return newer messages. Respect hidden-history behavior.

The backend’s initial `hasMore` describes older messages while `nextAfterSequence` is a forward cursor. Do not invent a backward-pagination endpoint. If older-history loading is deferred, document it as a known limitation.

### Receipts

```http
POST /api/conversations/{conversationId}/messages/{messageId}/receipts
```

```json
{ "type": "DELIVERED" }
```

or:

```json
{ "type": "READ" }
```

Receipt transitions are monotonic. `receiptState` is useful primarily for messages sent by the current user; do not infer received-message state from it.

## Message state rules

Use local metadata without changing the backend response shape:

```js
{
  optimistic: true,
  status: "sending" // sending | sent | failed | delivered | read
}
```

Send sequence:

1. Create and retain `clientMessageId`.
2. Add an optimistic message.
3. Send the request.
4. Replace/reconcile using `clientMessageId` and returned ID.
5. Preserve the same client ID for retry.

## Dependencies and prerequisites

- Phase 3 selected conversation and current user identity.
- Typed/normalized HTTP client.
- A focused message state module; do not introduce a global store unless message/conversation state genuinely requires it.

## Testing and completion criteria

- Messages load in sequence order.
- Sending does not duplicate optimistic and server messages.
- Retry uses the same client ID.
- Failed sends have a visible retry action.
- Empty, loading, and error states work.
- Delivered/read calls are not sent for the sender’s own message.
- Receipt status updates are correctly scoped to own messages.
- Hidden history is not displayed by synchronization.
- Switching conversations does not render stale responses.

---

# Phase 5 — STOMP Realtime, Typing, and Reconnection

## Objectives and features

- Authenticate a STOMP connection with the current JWT.
- Subscribe to user-targeted realtime events.
- Reconcile messages, receipts, and conversations.
- Implement typing indicators.
- Handle reconnect and REST resynchronization.
- Disconnect on logout or token expiration.

## Tasks in implementation order

1. Add `@stomp/stompjs`.
2. Create one shared STOMP client instance per authenticated session.
3. Configure `/ws` and `Authorization` `CONNECT` headers.
4. Subscribe to `/user/queue/events`.
5. Normalize and route event envelopes.
6. Handle message-created events.
7. Handle receipt events.
8. Handle conversation-created/updated/removed events.
9. Add connection status and bounded reconnect backoff.
10. Synchronize active conversations after reconnect.
11. Add typing send debounce/throttle.
12. Add typing event expiry.
13. Disconnect on logout, token expiry, and application teardown.

## Components and files

```text
src/realtime/
├── stompClient.js
├── realtimeEvents.js
├── realtimeEventRouter.js
├── realtimeStore.js
└── useRealtime.js

src/typing/
├── TypingIndicator.jsx
└── typingState.js
```

## WebSocket integration

Connect to:

```text
ws://localhost:8080/ws
```

Send:

```js
connectHeaders: {
  Authorization: `Bearer ${accessToken}`
}
```

Subscribe to:

```text
/user/queue/events
```

Send typing to:

```text
/app/conversations/{conversationId}/typing
```

Payload:

```json
{ "typing": true }
```

## Event processing

### `MESSAGE_CREATED`

Payload is a `MessageResponse`. Insert by message ID and sequence number, reconcile by `clientMessageId` when applicable, update conversation preview, and mark inactive conversations as having new activity. The sender does not receive this event for their own normal message.

### `MESSAGE_DELIVERED` and `MESSAGE_READ`

Payload is a `ReceiptResponse`. Update status only for relevant messages sent by the current user. Do not treat this as a full receipt-details feed for received messages.

### `CONVERSATION_CREATED` and `CONVERSATION_UPDATED`

Payload is a `ConversationResponse`. Upsert the list item, update title/member/preview data, and update the active header when applicable.

### `CONVERSATION_REMOVED`

Refresh the conversation list, close the active conversation when necessary, and distinguish soft hiding from permanent deletion where the payload provides that information.

### `TYPING_STARTED` and `TYPING_STOPPED`

Show or clear the indicator for the relevant conversation. The payload provides `actorDisplayName` and `expiresInSeconds`, normally five seconds. Typing is transient and is not persisted.

## Reconnect behavior

The backend does not replay missed events. After reconnect:

1. Reconnect with the current token.
2. Resubscribe.
3. Reload conversations.
4. Synchronize each open conversation from its last known sequence.
5. Merge by message ID and sequence.
6. Clear stale typing state.

The backend validates the WebSocket token at `CONNECT`, not continuously during the socket lifetime. The client should prevent reconnect attempts with an expired session.

## Dependencies and prerequisites

- Phase 2 session/token lifecycle.
- Phase 3 conversation state.
- Phase 4 message and receipt state.
- `@stomp/stompjs`.

## Testing and completion criteria

- `CONNECT` includes the bearer token.
- Successful connection subscribes exactly once.
- Connection failures do not crash the application.
- Reconnect uses bounded backoff.
- Duplicate realtime messages do not duplicate timeline entries.
- Realtime message events update inactive previews.
- Receipt events update own-message status.
- Conversation removal refreshes/cleans active state.
- Typing indicators expire and stop correctly.
- Reconnect triggers REST synchronization.
- Logout disconnects before session removal.

---

# Phase 6 — Search, Polish, Accessibility, and Release Readiness

## Objectives and features

- Implement message search.
- Complete management and profile UI.
- Improve responsive behavior and accessibility.
- Add offline/realtime status.
- Add focused automated tests.
- Verify production build and backend integration.

## Tasks in implementation order

1. Implement message search dialog/page.
2. Navigate from a result to its conversation.
3. Complete member and group settings interactions.
4. Add profile menu and local logout.
5. Add connection/offline indicators.
6. Refine loading, empty, error, retry, and destructive-confirmation states.
7. Add keyboard navigation, focus management, labels, and reduced-motion behavior.
8. Optimize only measured bottlenecks, especially long message lists.
9. Add unit/component/integration tests.
10. Add production environment configuration and build documentation.
11. Run manual integration testing against PostgreSQL, Redis, and the Spring backend.

## Components and files

```text
src/
├── search/
│   ├── searchApi.js
│   ├── MessageSearchDialog.jsx
│   └── SearchResult.jsx
├── components/
│   ├── ConnectionStatus.jsx
│   ├── OfflineBanner.jsx
│   └── ProfileMenu.jsx
└── test/
    ├── setup.js
    ├── fixtures.js
    └── mocks.js
```

## Backend API

```http
GET /api/messages/search?q={query}&size={size}
```

The backend searches member conversations using case-insensitive SQL `LIKE`, caps size at 50, and returns newest-first results. It does not apply `hiddenAfterSequence`; the UI must not claim search is guaranteed to match synchronized visible history.

## Testing

### Unit tests

Cover:

- Session expiry.
- Error normalization.
- Client message ID generation.
- Message reconciliation.
- Sequence merging.
- Event routing.
- Receipt state handling.
- Typing expiry.
- Role/action selectors.

### Component tests

Cover:

- Login/register forms.
- Conversation list and creation dialogs.
- Message composer/timeline.
- Receipt indicators.
- Member management.
- Search results.
- Loading/error/empty states.

### Integration tests

Mock REST and STOMP boundaries for:

- Login to protected shell.
- Conversation selection to message sync.
- Optimistic send to server reconciliation.
- Realtime event to timeline/preview update.
- Reconnect to REST synchronization.
- Conversation removal cleanup.
- Logout to STOMP disconnect.

### Manual integration verification

Against the actual backend:

- Register and login with two users.
- Create direct and group conversations.
- Exchange messages.
- Verify delivery/read receipts.
- Verify typing events.
- Disconnect/reconnect and confirm synchronization.
- Add/remove members and rename groups.
- Search messages.
- Soft hide and permanently delete groups.

## Completion criteria

- Every implemented backend feature has a usable frontend path.
- No component depends on an unimplemented endpoint or event.
- REST and STOMP communication is centralized and maintainable.
- The UI remains useful when realtime delivery is delayed or unavailable.
- Build succeeds with configurable API/WebSocket origins.
- High-risk reconciliation and reconnect behavior has automated coverage.
- Accessibility and responsive behavior meet the project’s UI standard.

---

# Recommended Incremental AI-Assisted Development Order

Implement one vertical slice at a time:

1. Phase 1 shell and routing.
2. Phase 2 login with a protected chat route.
3. Phase 3 conversation list and user search.
4. Phase 3 conversation creation.
5. Phase 4 message synchronization.
6. Phase 4 message sending and idempotent reconciliation.
7. Phase 4 receipts.
8. Phase 5 STOMP connection and message events.
9. Phase 5 typing and reconnect synchronization.
10. Phase 3 management actions.
11. Phase 6 search and polish.
12. Phase 6 automated tests and production verification.

After each slice, run the frontend build and a focused manual flow against the real backend before proceeding.

# Explicit Non-Goals

Do not add frontend flows for:

- Refresh tokens.
- Server-side logout.
- Role changes.
- Conversation mute.
- User blocking.
- Push notifications.
- Audit history.
- Direct-conversation rejoin.
- Event replay.
- Backend features not listed in the implemented API contract.
