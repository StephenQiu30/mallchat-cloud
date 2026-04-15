## 1. Friendship flow

- [x] 1.1 Audit current friend-apply, approve, and list APIs against the `chat-friend` spec.
- [x] 1.2 Complete missing validation and state transitions for friend application processing.
- [x] 1.3 Ensure private-room get-or-create behavior is stable for a confirmed friend pair.
- [x] 1.4 Add or update tests for friend application, approval, duplicate handling, and private-room creation.

## 2. Messaging flow

- [x] 2.1 Audit current send-message, history-query, recall, and read APIs against the `chat-message` spec.
- [x] 2.2 Complete missing message authorization and lifecycle handling for private and group rooms.
- [x] 2.3 Ensure message send, recall, and read flows publish the required real-time events.
- [x] 2.4 Add or update tests for message persistence, history pagination, recall authorization, and read updates.

## 3. Session and presence flow

- [x] 3.1 Audit current session list, top, delete, and unread behavior against the `chat-session` spec.
- [x] 3.2 Audit current distributed connection tracking and push behavior against the `chat-online-status` spec.
- [x] 3.3 Complete session-state updates so sender and receiver behavior matches the spec.
- [x] 3.4 Ensure session update and online-status events are pushed to the correct connected users.
- [x] 3.5 Add or update tests for session ordering, unread count changes, top/delete actions, and online-status transitions.

## 4. Integration and verification

- [x] 4.1 Verify REST API contracts in `mallchat-api-chat` match the completed chat-service behaviors.
- [x] 4.2 Verify SQL schema, cache keys, and MQ event types remain aligned with the implemented flows.
- [x] 4.3 Run focused service tests for chat messaging, sessions, and online-status scenarios.
- [x] 4.4 Prepare a demo checklist covering friend add, private chat, group chat, read, recall, session updates, and presence.

## 5. MVP guardrails

- [x] 5.1 During implementation, reuse existing controller, service, mapper, entity, and MQ/WebSocket patterns instead of introducing new architectural layers.
- [x] 5.2 Limit delivery to the capabilities defined in this change and defer non-MVP enhancements unless they are required to complete the current user flows.
- [x] 5.3 Restrict MVP delivery to confirmed-friend private chat, existing message types, basic group chat, session maintenance, and online-status push.
- [x] 5.4 Reject or defer non-MVP ideas such as audio/video, advanced group permissions, social discovery, or new storage/dispatch abstractions during implementation.

## 6. Capability-to-Code checklist

- [x] 6.1 `chat-friend`: verify `ChatFriendController`, `ChatFriendApplyController`, `UserFriendService`, `UserFriendApplyService`, `ChatRoomService`, and tables `user_friend` / `user_friend_apply` / `chat_private_room` stay aligned with the spec.
- [x] 6.2 `chat-message`: verify `ChatMessageController`, `ChatMessageService`, `ChatRoomMemberService`, `ChatPrivateRoomService`, `ChatSessionService`, `ChatMqProducer`, and table `chat_message` stay aligned with the spec.
- [x] 6.3 `chat-session`: verify `ChatSessionController`, `ChatSessionService`, related room/group/private-room services, and table `chat_session` stay aligned with the spec.
- [x] 6.4 `chat-online-status`: verify `ChatOnlineStatusService`, `ChannelManager`, MQ push flow, and cache-based connection keys stay aligned with the spec.
- [x] 6.5 Use the mapping in `design.md` as the source of truth before adding any new file, and prefer filling gaps in existing modules first.

## 7. Recommended implementation sequence

- [x] 7.1 Complete `chat-friend` first so private-chat entry and friend-based permissions are stable.
- [x] 7.2 Complete `chat-message` second so the MVP can already support end-to-end chatting.
- [x] 7.3 Complete `chat-session` third so unread counts and conversation display are derived from stable message behavior.
- [x] 7.4 Complete `chat-online-status` last as a support capability layered on top of the stable friend and session graph.
