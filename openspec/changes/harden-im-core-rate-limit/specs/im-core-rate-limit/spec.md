## ADDED Requirements

### Requirement: IM core write endpoints are rate limited at gateway
The gateway SHALL apply user-scoped rate limiting to high-frequency IM write endpoints before the general chat route.

#### Scenario: Chat message send has a dedicated rate limit route
- **WHEN** a request path matches `/api/chat/message/send`
- **THEN** the gateway SHALL match a dedicated route before the general `/api/chat/**` route
- **AND** the route SHALL use `RequestRateLimiter`
- **AND** the route SHALL use the user key resolver

#### Scenario: General chat APIs keep existing rate limit route
- **WHEN** a request path matches other `/api/chat/**` APIs
- **THEN** the gateway SHALL keep the existing general chat route
- **AND** the route SHALL keep the existing user key resolver rate limit policy
