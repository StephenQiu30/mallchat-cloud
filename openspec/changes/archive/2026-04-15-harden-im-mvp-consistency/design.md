## Context

The current IM MVP already supports friend application, private and group messaging, session list updates, and distributed online-status push. The main gaps are not feature absence but inconsistent enforcement of core IM rules across room access, read-state handling, and presence subscriber resolution.

Three implementation paths currently weaken MVP stability:

- Room membership can still be created through a public join path without invitation or room-type restrictions.
- Message read reporting updates the member read boundary, but session unread state is cleared unconditionally instead of following the submitted read boundary.
- Online-status push computes recipients directly from Redis friend cache, so cold start or cache expiration can result in missing friend notifications.

The codebase already has stable module boundaries that should be preserved:

- `controller -> service -> mapper -> entity`
- Room and message lifecycle centered in `mallchat-chat-service`
- WebSocket connection lifecycle centered in `mallchat-common-websocket`
- Friend relationship cache loading already exists in `UserFriendService`

This design therefore focuses on hardening existing flows rather than redesigning the IM architecture.

## Goals / Non-Goals

**Goals:**

- Enforce MVP room access rules so users cannot join rooms through uncontrolled public paths.
- Make session unread count consistent with message read boundary semantics.
- Make friend online-status notification reliable when friend cache is cold or missing.
- Reuse existing service structure and cache conventions.
- Preserve current API shapes, message event formats, and MQ push model wherever possible.

**Non-Goals:**

- No new IM features such as blacklist, invisible mode, stranger chat, or message edit.
- No redesign of room, session, or websocket architecture.
- No change to existing websocket event payload format.
- No new storage model for presence or unread tracking.
- No broad performance refactor beyond the minimum required to remove the current consistency gaps.

## Decisions

### 1. Room membership will only be created through controlled service paths

`/chat/room/join` currently exposes an uncontrolled membership entry point. MVP room access should instead be created only through service flows that already carry business intent:

- group room creation
- group invitation
- private room initialization between confirmed friends

Design decision:

- treat direct public room join as unsupported for MVP
- reject manual join for private rooms
- reject uncontrolled join for group rooms unless a future invitation-token or approval flow is explicitly introduced

Rationale:

- This is the smallest change that closes the current authorization gap.
- It matches the already implemented room-entry patterns instead of adding a parallel rule set.

Alternatives considered:

- Keep `/chat/room/join` and add best-effort room-type checks.
  - Rejected because it still leaves undefined entry semantics for group rooms.
- Add a new invitation-token or join-approval model now.
  - Rejected because it expands product scope beyond MVP hardening.

### 2. Read progress will use boundary-based unread maintenance

`lastReadMessageId` should be treated as a read boundary, not as a signal to clear all unread state. Session unread count should represent the number of room messages newer than the stored read boundary for that user.

Design decision:

- keep `ChatRoomMember.lastReadMessageId` as the source of truth for room-level read progress
- update `ChatSession.lastReadMessageId` together with room-member read progress
- recalculate or adjust `ChatSession.unreadCount` based on the submitted boundary instead of setting it to `0`

Rationale:

- The current data model already contains the required fields.
- This preserves the existing session projection pattern while correcting the state semantics.

Alternatives considered:

- Remove stored unread count and always compute unread dynamically.
  - Rejected because it changes the current session model and likely expands query cost.
- Keep direct clear-to-zero behavior and weaken the spec.
  - Rejected because it produces visibly wrong unread state for partial reads.

### 3. Presence subscriber resolution will move to the friend domain

`ChannelManager` should remain responsible for connection lifecycle and broadcast execution, not for resolving business-level subscriber relationships from cache keys.

Design decision:

- introduce a friend-domain method that returns friend IDs for notification use
- use cache-first, database-fallback, and cache-fill behavior inside the friend domain
- keep `ChannelManager` responsible only for triggering online/offline notification and sending the event

Rationale:

- Friend relationship and its cache semantics already belong to `UserFriendService`.
- This reduces business coupling inside the websocket infrastructure layer.
- It reuses existing empty-set placeholder and friend-cache loading patterns instead of inventing a second cache strategy.

Alternatives considered:

- Add DB fallback directly inside `ChannelManager`.
  - Rejected because it pushes friendship query logic deeper into the websocket infrastructure layer.
- Always query DB for notification targets.
  - Rejected because it removes the benefit of the existing friend cache on a hot path.

### 4. Existing client-visible protocols will remain unchanged

The current HTTP request shapes and websocket event payload structures are already integrated into the surrounding chat flow.

Design decision:

- preserve `/chat/message/read` request structure
- preserve websocket online-status event shape
- preserve room/session push event shapes
- change only server-side authorization and state-maintenance behavior

Rationale:

- This keeps the hardening change safe, localized, and MVP-aligned.

Alternatives considered:

- Introduce new read-state or presence event payloads.
  - Rejected because it would expand the scope from behavior correction into protocol migration.

## Risks / Trade-offs

- [Rejecting public room join may affect any hidden caller of `/chat/room/join`] -> Limit the MVP decision to explicit rejection and document that room entry must come from controlled creation or invitation flows.
- [Unread recalculation can introduce extra query cost] -> Keep the logic scoped to the affected room and user, and reuse existing message/session identifiers rather than introducing global recomputation.
- [Moving friend subscriber resolution into the friend domain increases service coupling between websocket and chat friendship logic] -> Keep the new method narrow and read-only so it behaves as a lightweight query capability rather than a new orchestration layer.
- [Cold-cache fallback may still depend on DB correctness and cache refill timing] -> Reuse the existing friend cache loading conventions and empty-set placeholder behavior to avoid introducing a second cache truth model.

## Migration Plan

1. Update room access behavior first so uncontrolled membership creation is blocked before further IM expansion.
2. Update read-boundary handling so session unread state reflects partial read semantics.
3. Move online-status subscriber resolution behind the friend-domain query method while keeping the websocket event format unchanged.
4. Add regression coverage for:
   - unauthorized room join attempts
   - partial read versus full read session state
   - cold-cache online-status notification

Rollback strategy:

- Room access hardening can be reverted by restoring the prior join behavior if necessary, though this reopens the authorization gap.
- Read-state changes can be reverted independently because they do not require schema changes.
- Presence subscriber resolution can fall back to direct cache usage if the friend-domain query method causes unexpected runtime issues.

No schema migration is required for this change.

## Open Questions

- Should `/chat/room/join` be retained as an explicit rejection endpoint for backward compatibility, or removed from external use in a later cleanup change?
- For unread maintenance, should the implementation recompute unread count from message IDs each time, or optimize by subtracting only the newly covered range?
- Should the friend-domain notification-target method stay in `UserFriendService`, or be exposed through a narrower chat-facing facade later if more presence visibility rules are added?
