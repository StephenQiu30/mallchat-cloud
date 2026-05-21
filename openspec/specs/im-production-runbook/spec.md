# im-production-runbook Specification

## Purpose
TBD - created by archiving change document-im-production-runbook. Update Purpose after archive.
## Requirements
### Requirement: IM production runbook is actionable
The system SHALL provide an operations runbook for backend production release and incident handling.

#### Scenario: Release operator prepares deployment
- **WHEN** an operator follows the runbook
- **THEN** the runbook SHALL list startup prerequisites, required configuration groups, health checks, and validation commands

#### Scenario: Runtime failure occurs
- **WHEN** a core backend dependency or IM path fails
- **THEN** the runbook SHALL provide first checks for gateway, chat-service, notification-service, Redis, MySQL, RabbitMQ, and Nacos

#### Scenario: Rollback or recovery is needed
- **WHEN** a release needs rollback or data recovery
- **THEN** the runbook SHALL include rollback, database backup/restore, and cache recovery guidance

