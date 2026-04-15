## ADDED Requirements

### Requirement: Friend online status is visible in real time
The system SHALL track distributed user connection state and SHALL push online status changes to the user and their friends when the first connection is established or the last connection is removed.

#### Scenario: User comes online
- **WHEN** a user's first active connection is established across the cluster
- **THEN** the system marks the user as online and pushes an online status event to the user and their friends

#### Scenario: User goes offline
- **WHEN** a user's last active connection is removed across the cluster
- **THEN** the system marks the user as offline and pushes an online status event to the user and their friends
