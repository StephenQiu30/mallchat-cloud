## ADDED Requirements

### Requirement: IM core write endpoints are rate limited at gateway
The gateway SHALL apply user-scoped rate limiting to high-frequency IM write endpoints before the general chat route.

#### Scenario: Core IM write API has a dedicated rate limit route
- **WHEN** a request path matches `/api/chat/message/send`, `/api/chat/friend/apply/add`, `/api/chat/moment/publish`, or `/api/file/upload`
- **THEN** the gateway SHALL match a dedicated route before the general `/api/chat/**` route
- **AND** the route SHALL use `RequestRateLimiter`
- **AND** the route SHALL use the user key resolver

#### Scenario: General APIs keep existing rate limit route
- **WHEN** a request path matches other `/api/chat/**` or `/api/file/**` APIs
- **THEN** the gateway SHALL keep the existing general route
- **AND** the route SHALL keep the existing user key resolver rate limit policy

#### Scenario: Rate limiter denies excessive core writes
- **WHEN** the user-scoped rate limiter rejects a core IM write request for message send, friend apply, moment publish, or file upload
- **THEN** the gateway SHALL return HTTP 429
- **AND** the downstream service chain SHALL NOT be invoked
