# cache-recovery Specification

## Purpose
TBD - created by archiving change verify-redis-cache-recovery. Update Purpose after archive.
## Requirements
### Requirement: Redis cache loss is recoverable without losing business facts
The system SHALL recover cache-derived IM state from durable facts or live connections after Redis cache loss.

#### Scenario: Friend cache is missing
- **WHEN** the friend cache key is missing
- **THEN** friend relationship checks SHALL load active friend facts from the database before deciding the result

#### Scenario: Room member cache is missing
- **WHEN** the room member cache key is missing
- **THEN** room membership checks SHALL load active member facts from the database before deciding the result

#### Scenario: WebSocket connection cache is missing but local channel is alive
- **WHEN** a connected user's Redis WebSocket connection keys are missing
- **THEN** the heartbeat refresh path SHALL rebuild the user's Redis connection set and metadata from local connection state

#### Scenario: Login session cache is missing
- **WHEN** Redis-backed login session data is missing
- **THEN** existing tokens MAY become invalid and users SHALL recover by logging in again

