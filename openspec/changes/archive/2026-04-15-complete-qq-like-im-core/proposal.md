## Why

The repository already contains a usable chat foundation, including chat rooms, messages, sessions, WebSocket connectivity, and MQ-based push. What is missing is a formal product-level definition of a QQ-like IM core so the system can be completed consistently rather than growing as disconnected chat endpoints.

This change defines the minimum integrated IM experience expected from a QQ-style communication system: friend relationships, private and group messaging, session management, and real-time presence and message events. The capability and module names in this change follow the existing repository naming style so the specification aligns with the current `chat-*` domain structure and related services. Capturing these expectations now will make implementation scope clearer for development, demo, and evaluation.

## What Changes

- Define a complete IM core experience around three user-facing domains: friendship, messaging, and sessions/presence.
- Standardize private chat and group chat behavior, including room lifecycle, history retrieval, message delivery, read state, and recall events.
- Define how friend applications, approvals, and friend list visibility should work as part of the communication flow.
- Define session list behavior, including unread counts, last message preview, ordering, top status, and deletion semantics.
- Define real-time online status and message event delivery expectations over WebSocket and MQ-backed push.
- Establish implementation tasks that align the existing `chat-service`, database schema, and push infrastructure with these capabilities.
- Keep implementation aligned with the current repository code style and module boundaries, and avoid introducing unnecessary abstractions beyond the MVP scope.

## MVP Scope

### In Scope
- Friend application, approval, ignore flow, and friend list query
- Stable private chat room creation and lookup for confirmed friends
- Group chat room creation based on existing room and member model
- Text, image, and file message send flow based on current message types
- Message history query, message read update, and message recall
- Session list query, unread count maintenance, top session, and delete session
- Online status tracking and push for the user and their friends
- Existing REST API contracts and WebSocket/MQ event flow required to complete the above behaviors

### Explicitly Out of Scope
- Audio/video calls, voice messages, red packets, reactions, or message editing
- Complex group operations such as transfer owner, mute management, or detailed permission systems
- Message forwarding, message pinning inside a room, favorites, or multi-device message roaming redesign
- Stranger chat, recommendation-based contacts, or broader social discovery features
- New infrastructure layers, new message storage models, or large-scale performance redesign
- Any UI beautification or frontend interaction redesign beyond what is required to verify API and push behavior

## Capabilities

### New Capabilities
- `chat-friend`: Covers friend application, approval, friend list management, and private chat entry creation between friends.
- `chat-message`: Covers private chat and group chat messaging, history query, message recall, and message read events.
- `chat-session`: Covers conversation list behavior, unread state, session updates, top/delete actions, and session-facing display state.
- `chat-online-status`: Covers distributed connection tracking and user online status push.

### Modified Capabilities

None.

## Impact

- Affected services: `mallchat-service/mallchat-chat-service`, `mallchat-api/mallchat-api-chat`
- Affected infrastructure: `mallchat-common-websocket`, `mallchat-common-rabbitmq`, cache-based online connection tracking
- Affected data model: `chat_room`, `chat_room_member`, `chat_message`, `chat_private_room`, `chat_session`, `user_friend`, `user_friend_apply`, `chat_group_info`
- Affected clients: any frontend or gateway consuming chat REST APIs and WebSocket push events
