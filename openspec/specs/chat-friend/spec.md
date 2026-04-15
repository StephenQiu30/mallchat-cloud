# chat-friend

## Purpose

Define the MVP friendship flow for the chat domain, including friend application, approval or ignore handling, friend list exposure, and confirmed-friend private chat entry.

## Requirements

### Requirement: User can initiate a friend application
The system SHALL allow an authenticated user to submit a friend application to another user with a validation message, and SHALL persist the application in a pending state until it is handled.

#### Scenario: Submit a new friend application
- **WHEN** a user sends a friend application to another valid user who is not already a friend
- **THEN** the system creates a pending friend application record and returns success

#### Scenario: Reject duplicate friendship creation
- **WHEN** a user sends a friend application to a target user who is already in the user's friend list
- **THEN** the system rejects the request and SHALL NOT create a duplicate friendship or application

### Requirement: Target user can process a friend application
The system SHALL allow the target user of a pending friend application to approve or ignore the request, and SHALL update the application status exactly once.

#### Scenario: Approve a friend application
- **WHEN** the target user approves a pending friend application
- **THEN** the system marks the application as approved and creates mutual friendship records for both users

#### Scenario: Ignore a friend application
- **WHEN** the target user ignores a pending friend application
- **THEN** the system marks the application as ignored and SHALL NOT create friendship records

### Requirement: Friend relationship enables private chat discovery
The system SHALL expose the friend list of the current user and SHALL allow a private chat entry to be created or retrieved for a selected friend.

#### Scenario: Query friend list
- **WHEN** a user requests their friend list
- **THEN** the system returns the user's confirmed friends with profile information required for chat display

#### Scenario: Enter a private chat with a friend
- **WHEN** a user requests a private chat room for a confirmed friend
- **THEN** the system returns an existing private room if present or creates one stable private room for the user pair
