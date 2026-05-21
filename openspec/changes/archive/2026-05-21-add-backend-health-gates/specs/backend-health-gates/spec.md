## ADDED Requirements

### Requirement: Backend health gates expose diagnostic readiness
The system SHALL expose backend health endpoints that distinguish process liveness from dependency readiness.

#### Scenario: Health endpoints are exposed
- **WHEN** backend services import the shared web configuration
- **THEN** Actuator SHALL expose `health`, `info`, and `metrics`

#### Scenario: Readiness includes core dependencies
- **WHEN** a backend service imports cache, MySQL, or RabbitMQ configuration
- **THEN** the readiness group SHALL include only the matching Redis, database, RabbitMQ, and ping indicators for those imported dependencies

#### Scenario: Liveness is independent from downstream dependencies
- **WHEN** liveness health is evaluated
- **THEN** the liveness group SHALL include only the basic process indicator
