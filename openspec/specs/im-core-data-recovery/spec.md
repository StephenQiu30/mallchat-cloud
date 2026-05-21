# im-core-data-recovery Specification

## Purpose
TBD - created by archiving change verify-im-core-data-recovery. Update Purpose after archive.
## Requirements
### Requirement: Core IM tables have a recoverable backup smoke path
The system SHALL provide a minimal backup and recovery smoke path for core IM business facts.

#### Scenario: Core table list is fixed
- **WHEN** an operator backs up IM business facts
- **THEN** the backup script SHALL include user, friend, room, member, message, session, group, private room, and moment tables

#### Scenario: Recovery uses an isolated smoke database
- **WHEN** recovery smoke verification runs
- **THEN** the script SHALL restore the backup into a temporary database instead of overwriting the source database

#### Scenario: Restored facts are queryable
- **WHEN** recovery smoke verification completes
- **THEN** the script SHALL check message-room, session-message, room-member, private-room, group-info, friend, and moment relationships

