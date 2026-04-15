## Why

Current IM MVP flows are usable, but several core behaviors are not yet stable enough for continued feature expansion. Room access can still be bypassed through a public join path, session unread state does not correctly reflect partial read boundaries, and online-status notification depends on warmed friend cache state, which makes presence behavior occasionally unreliable.

This change is needed now to harden the existing MVP before adding more IM capabilities. It keeps the current architecture and interaction model intact while closing the most visible consistency and authorization gaps.

## What Changes

- Restrict room-join behavior so room membership cannot be created through an unguarded public path.
- Clarify and enforce MVP room access rules for group chat and private chat.
- Fix session unread-state handling so read progress follows the submitted read boundary instead of always clearing unread state.
- Preserve current message, session, and websocket event models while correcting state-update behavior behind them.
- Harden online-status notification subscriber resolution so friend presence updates remain reliable during cold start or cache miss scenarios.

## Capabilities

### New Capabilities

- `chat-room-access`: define MVP room access and membership-entry rules for group chat and private chat.

### Modified Capabilities

- `chat-message`: refine read-progress requirements so unread state follows the actual read boundary instead of clearing unconditionally.
- `chat-session`: refine unread maintenance requirements so session unread count remains consistent with message read progress.
- `chat-online-status`: refine presence-notification requirements so friend online-status push remains reliable even when friend cache is cold or missing.

## Impact

- Affected code:
  - `mallchat-service/mallchat-chat-service` room, message, and session services/controllers
  - `mallchat-common/mallchat-common-websocket` channel manager and online-status push path
- Affected APIs:
  - `/chat/room/join`
  - `/chat/message/read`
  - websocket online-status push behavior
- Affected systems:
  - room membership persistence
  - session unread-state maintenance
  - Redis-backed friend cache and distributed presence notification
