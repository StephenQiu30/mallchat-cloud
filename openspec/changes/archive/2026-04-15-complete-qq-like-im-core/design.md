## Context

The repository already contains most of the technical primitives required for an IM system: chat-domain entities, session records, WebSocket handling through Netty, RabbitMQ-backed event distribution, and cache-backed online connection tracking. The missing piece is a unified design that treats these parts as one QQ-like communication experience instead of several isolated APIs.

The implementation spans multiple modules and concerns: REST APIs in `mallchat-chat-service`, shared contracts in `mallchat-api-chat`, real-time push infrastructure in `mallchat-common-websocket` and `mallchat-common-rabbitmq`, and relational storage defined in `sql/mallchat.sql`. Because the user-visible behavior depends on consistency across these layers, this change benefits from an explicit design before feature completion.

## Goals / Non-Goals

**Goals:**
- Define the minimum QQ-like IM core supported by the current architecture.
- Reuse the existing chat schema and infrastructure wherever possible.
- Make friendship, messaging, sessions, and presence work as one coherent system.
- Keep the change implementation-ready by mapping product expectations onto current service boundaries.
- Keep code implementation style consistent with the existing project and satisfy the MVP with the smallest reasonable set of changes.

**Non-Goals:**
- Audio/video calls, screen sharing, or live streaming.
- Full QQ ecosystem features such as social feed, file disk, or game integrations.
- End-to-end encryption, multi-region delivery, or internet-scale message routing redesign.
- Replacing RabbitMQ, Netty, or the current microservice structure.

## MVP Feature Boundary

**MVP Includes:**
- Completing friend application and approval into a usable friend relationship flow
- Completing confirmed-friend private chat and existing-model group chat
- Completing message send, history, recall, and read behavior for existing message types
- Completing session list and unread-state behavior tied to message lifecycle
- Completing distributed online-status tracking and push

**MVP Excludes:**
- Any new message type beyond the current text, image, and file model
- Advanced group-management features beyond creation, membership, and basic room communication
- Any new storage, dispatch, or orchestration framework not already present in the repository
- Any optimization work that is not necessary to make the current end-to-end flow correct and testable

## Decisions

### Decision: Keep capability names aligned with existing chat-domain modules
The change uses capability names that match the repository's current domain language: `chat-friend`, `chat-message`, `chat-session`, and `chat-online-status`.

Rationale:
- The current codebase consistently uses `Chat*` and `UserFriend*` naming in services, controllers, DTOs, entities, and tables.
- Matching OpenSpec capability names to that style makes the artifacts easier to trace back to concrete modules during implementation.
- It still preserves the core IM product boundaries: relationship, messaging, session state, and presence.

Alternatives considered:
- One large `im-core` capability: rejected because it becomes hard to review, test, and evolve.
- Using `im-*` capability names: rejected because it introduces a second naming system that is inconsistent with the current repository.

### Decision: Follow existing service-layer patterns and avoid new architectural layers
The implementation should extend the current controller-service-mapper-entity structure and shared MQ/WebSocket utilities instead of introducing new orchestration layers, generic engines, or premature extension points.

Rationale:
- The repository already has a clear implementation style in `mallchat-chat-service`, and matching it keeps the code easier to understand and maintain.
- This change is intended to complete the QQ-like IM core as an MVP, so solving the required user flows is more important than building a highly abstract framework for future possibilities.

Alternatives considered:
- Adding a new IM facade/domain layer: rejected because it increases indirection without being necessary for the current scope.
- Designing for advanced future features up front: rejected because it adds complexity before the MVP behavior is stable.

### Decision: Keep room-centric message flow as the core abstraction
Both private chats and group chats continue to use room-based storage and event delivery.

Rationale:
- The current schema already models private and group messaging through `chat_room` with companion tables for members and private room mapping.
- Session updates, history queries, recall, and read flow can all hang off room identifiers without introducing a second message model.

Alternatives considered:
- Separate private-message and group-message models: rejected because it duplicates message lifecycle logic.

### Decision: Use MQ-backed WebSocket push for all real-time state changes
Message delivery, recall, read receipts, session updates, friend events, and online status changes are all treated as push events routed through MQ to WebSocket consumers.

Rationale:
- The repository already contains event enums and push producers for these behaviors.
- MQ-backed push keeps the design compatible with multiple chat-service instances and distributed connection ownership.

Alternatives considered:
- Direct in-process WebSocket writes only: rejected because it breaks cross-instance delivery.

### Decision: Model unread and display state at the session layer
Unread count, last message preview, ordering, top status, and deletion semantics belong to `chat_session`, while room/member state remains the source for authorization and read progress.

Rationale:
- This matches the current schema and avoids recomputing conversation list state from raw message history on every request.
- It supports the QQ-like UX where the session list is the primary inbox surface.

Alternatives considered:
- Computing session state on read from message tables only: rejected for performance and implementation complexity.

## Risks / Trade-offs

- [Unread state can drift from room read progress] → Mitigation: treat read APIs and message send flows as the only writers of unread/session state and verify them together with tests.
- [Real-time event duplication or out-of-order delivery may confuse clients] → Mitigation: keep stable `bizId` values and make client event handling idempotent where possible.
- [Friendship and private-room creation can race under concurrent requests] → Mitigation: rely on unique constraints and implement get-or-create flows around stable user-pair mappings.
- [Session deletion semantics may be misunderstood as hard deletion of message history] → Mitigation: define session deletion as user-local inbox cleanup only, not room/message removal.
- [Overdesign makes the change harder to implement and review] → Mitigation: keep to existing module boundaries, reuse current infrastructure, and only implement behavior required by the MVP specs.

## Migration Plan

1. Confirm existing chat schema and APIs cover the required entities and identify missing endpoints or state transitions.
2. Implement or normalize friendship flows and private-room creation semantics.
3. Implement or normalize message lifecycle flows, including send, history, read, and recall.
4. Implement or normalize session-state maintenance and real-time session updates.
5. Verify distributed presence updates through cache and MQ-backed push.
6. Roll out behind normal service deployment; rollback is service-level because the schema already exists and this change is primarily behavior completion.

## Minimal Implementation Checklist

### `chat-friend`

**Primary controllers:**
- `com.stephen.cloud.chat.controller.ChatFriendController`
- `com.stephen.cloud.chat.controller.ChatFriendApplyController`
- `com.stephen.cloud.chat.controller.ChatRoomController` (`/chat/room/private` for private-room entry)

**Primary services:**
- `UserFriendService` / `UserFriendServiceImpl`
- `UserFriendApplyService` / `UserFriendApplyServiceImpl`
- `ChatRoomService` for approved-friend private-room creation
- `ChatOnlineStatusService` for friend-list online status display

**Primary mappers and tables:**
- `UserFriendMapper` ↔ `user_friend`
- `UserFriendApplyMapper` ↔ `user_friend_apply`
- `ChatPrivateRoomMapper` ↔ `chat_private_room`
- `ChatRoomMapper` ↔ `chat_room`

**Existing behavior already present:**
- Direct add-friend and friend-list query
- Friend application submit, approve, ignore
- Approval path creates mutual friendship and initializes private room
- Friend list can return peer profile and online status

**Minimum implementation focus:**
- Verify apply/approve/ignore states fully match `chat-friend` spec
- Verify duplicate apply and duplicate friend creation remain idempotent
- Verify private-room creation is only used for confirmed friends in MVP
- Avoid introducing a separate friendship domain layer; extend current services only

### `chat-message`

**Primary controllers:**
- `com.stephen.cloud.chat.controller.ChatMessageController`

**Primary services:**
- `ChatMessageService` / `ChatMessageServiceImpl`
- `ChatRoomService`
- `ChatRoomMemberService`
- `ChatPrivateRoomService`
- `UserFriendService`
- `ChatSessionService`
- `ChatMqProducer`

**Primary mappers and tables:**
- `ChatMessageMapper` ↔ `chat_message`
- `ChatRoomMapper` ↔ `chat_room`
- `ChatRoomMemberMapper` ↔ `chat_room_member`
- `ChatPrivateRoomMapper` ↔ `chat_private_room`

**Existing behavior already present:**
- Send text/image/file message
- Query room history with cursor pagination
- Mark message read
- Recall own message within 2 minutes
- Push message, recall, and read events through MQ/WebSocket flow

**Minimum implementation focus:**
- Verify private-chat send permission strictly depends on mutual friendship
- Verify group-chat send/history/read all require room membership
- Verify send/recall/read event payloads are enough for current clients
- Keep message model limited to current types; do not add new message abstractions

### `chat-session`

**Primary controllers:**
- `com.stephen.cloud.chat.controller.ChatSessionController`

**Primary services:**
- `ChatSessionService` / `ChatSessionServiceImpl`
- `ChatMessageService` for read/recall-driven session refresh
- `ChatRoomService`, `ChatGroupInfoService`, `ChatPrivateRoomService`
- `ChatOnlineStatusService`
- `ChatMqProducer`

**Primary mappers and tables:**
- `ChatSessionMapper` ↔ `chat_session`
- `ChatGroupInfoMapper` ↔ `chat_group_info`
- `ChatPrivateRoomMapper` ↔ `chat_private_room`
- `ChatRoomMapper` ↔ `chat_room`
- `ChatMessageMapper` ↔ `chat_message`

**Existing behavior already present:**
- Query session list
- Top and delete session
- Build private/group session display data
- Maintain unread count and last message on message activity
- Push session update and session delete events

**Minimum implementation focus:**
- Verify sender and receiver session updates follow MVP unread rules
- Verify read operation clears unread through explicit read-state update only
- Verify recall and read both trigger session refresh where needed
- Treat delete as user-local inbox removal only, not message deletion

### `chat-online-status`

**Primary controllers:**
- No dedicated REST controller is required for MVP
- Online status is surfaced through friend list, session list, and WebSocket push

**Primary services and infrastructure:**
- `ChatOnlineStatusService` / `ChatOnlineStatusServiceImpl`
- `com.stephen.cloud.common.websocket.manager.ChannelManager`
- `ChatMqProducer`
- shared WebSocket constants and cache utilities

**Primary tables / storage:**
- No dedicated relational table
- Redis/cache keys under WebSocket connection metadata and friend cache

**Existing behavior already present:**
- Query single and batch online status
- Track distributed connection existence from WebSocket cache keys
- Push online/offline events on first connect and last disconnect

**Minimum implementation focus:**
- Verify first-connect and last-disconnect semantics across multiple connections
- Verify online-status push targets only the user and their friends
- Reuse current cache-based approach; do not add a new persistence model for presence
- Keep presence logic as a support capability, not a standalone product feature

## Recommended Development Order

### 1. `chat-friend`
Start here because it establishes the MVP relationship boundary: who is allowed to open a private chat and who can see presence changes as a friend.

**Why first:**
- It has the clearest business boundary and the fewest moving parts.
- Private chat permission in `chat-message` depends on confirmed friendship.
- Online-status push is more meaningful once the friend graph is reliable.

**Most stable files to inspect and adjust first:**
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatFriendApplyController.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatFriendController.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/UserFriendApplyServiceImpl.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/UserFriendServiceImpl.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/ChatRoomService.java`

### 2. `chat-message`
Do this next because message sending is the main user flow and it already hangs on friendship and room membership rules.

**Why second:**
- Once friendship and private-room rules are stable, message permission checks become straightforward.
- This capability drives later session updates and read-state behavior.
- It gives the fastest visible MVP result: users can actually chat.

**Most stable files to inspect and adjust first:**
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatMessageController.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/ChatMessageServiceImpl.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/ChatRoomMemberService.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/ChatPrivateRoomService.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/mq/producer/ChatMqProducer.java`

### 3. `chat-session`
Handle session behavior after message flow is stable, because session state is largely derived from message send, recall, and read operations.

**Why third:**
- Session correctness depends on the message lifecycle already being trustworthy.
- It is easier to verify unread count and last-message preview once real message paths are working.
- This keeps the implementation MVP-focused by treating session as a projection layer, not a separate domain rewrite.

**Most stable files to inspect and adjust first:**
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatSessionController.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/ChatSessionServiceImpl.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/listener/ChatSessionListener.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/convert/ChatSessionConvert.java`

### 4. `chat-online-status`
Finish here because presence is a support capability that should be verified after friendship, messaging, and session flows are stable.

**Why fourth:**
- Presence is already structurally present in the codebase.
- The MVP value of online status depends on the friend list and session display already being correct.
- It is safer to do final verification here than to start with infrastructure tuning.

**Most stable files to inspect and adjust first:**
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/ChatOnlineStatusServiceImpl.java`
- `mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/manager/ChannelManager.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/mq/producer/ChatMqProducer.java`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/UserFriendServiceImpl.java`

## Open Questions

- Private chat creation in the MVP should be restricted to confirmed friends to keep the relationship model simple and consistent with the current friend flow.
- The MVP should reuse the current recall behavior and avoid introducing a new recall-window rule unless the existing implementation proves insufficient.
- Unread count in the MVP should be cleared by explicit read-state updates rather than by passive history queries.

## Demo Checklist

- Submit a friend application to an existing user and confirm duplicate or reverse-direction pending requests are handled safely.
- Approve the friend application and verify the pair can get or create exactly one private room.
- Send a text, image, or file message in the private room and verify the sender and peer both receive the real-time message event.
- Query room history with and without a cursor and verify messages are returned in stable chronological order.
- Mark the latest room message as read and verify unread count is cleared and a read event is emitted.
- Recall a just-sent message as the original sender and verify the room receives the recall event and the session preview updates.
- Load the session list and verify top status, unread count, last-message preview, peer avatar/name, and private-chat online status are present.
- Top and delete a session and verify the current user receives the corresponding session update or deletion push.
- Open and close WebSocket connections for a user and verify online-status events are pushed to the user and their friends only on first connect and last disconnect.
