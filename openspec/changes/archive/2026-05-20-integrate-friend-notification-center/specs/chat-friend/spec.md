## MODIFIED Requirements

### Requirement: User can initiate a friend application
The system SHALL allow an authenticated user to submit a friend application to another user with a validation message, SHALL persist the application in a pending state until it is handled, and SHALL create a notification-center record for the target user when a new application is created.

#### Scenario: Submit a new friend application
- **WHEN** a user sends a friend application to another valid user who is not already a friend
- **THEN** the system creates a pending friend application record and returns success
- **AND** the system creates a `user` notification for the target user
- **AND** the notification `relatedType` is `user_friend_apply`
- **AND** the notification `relatedId` is the friend application ID

#### Scenario: Reject duplicate friendship creation
- **WHEN** a user sends a friend application to a target user who is already in the user's friend list
- **THEN** the system rejects the request and SHALL NOT create a duplicate friendship or application
- **AND** the system SHALL NOT create a notification-center record

#### Scenario: Return existing pending application without duplicate notification
- **WHEN** a user sends a friend application that matches an existing same-direction pending application
- **THEN** the system returns the existing application ID
- **AND** the system SHALL NOT create another notification-center record

#### Scenario: Friend application notification failure is degraded
- **WHEN** a new friend application is persisted successfully but notification-center creation fails
- **THEN** the friend application remains pending
- **AND** the existing friend-application WebSocket event remains attempted
- **AND** the application API still returns the application ID

### Requirement: Target user can process a friend application
The system SHALL allow the target user of a pending friend application to approve or ignore the request, SHALL update the application status exactly once, and SHALL create a notification-center record for the applicant when the application is approved.

#### Scenario: Approve a friend application
- **WHEN** the target user approves a pending friend application
- **THEN** the system marks the application as approved and creates mutual friendship records for both users
- **AND** the system creates a `user` notification for the applicant
- **AND** the notification `relatedType` is `user_friend_apply`
- **AND** the notification `relatedId` is the friend application ID

#### Scenario: Ignore a friend application
- **WHEN** the target user ignores a pending friend application
- **THEN** the system marks the application as ignored
- **AND** the system SHALL NOT create friendship records
- **AND** the system SHALL NOT create an approval notification

#### Scenario: Friend approval notification failure is degraded
- **WHEN** a pending friend application is approved and notification-center creation fails
- **THEN** the mutual friendship records and private room creation remain successful
- **AND** the existing friend-approval WebSocket event remains attempted
- **AND** the approval API still follows the friend approval result
