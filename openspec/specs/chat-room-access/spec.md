## ADDED Requirements

### Requirement: Room membership entry follows controlled MVP paths
The system SHALL allow room membership to be created only through controlled MVP entry paths, including group creation, group invitation, and private room initialization between confirmed friends.

#### Scenario: Create a group room with initial members
- **WHEN** an authenticated user creates a new group room and specifies valid invited members
- **THEN** the system creates the room, adds the creator as owner, and adds only the invited members admitted through the controlled creation flow

#### Scenario: Invite a friend into an existing group room
- **WHEN** a room member invites a confirmed friend into an existing group room
- **THEN** the system creates room membership for that invited friend through the invitation flow

#### Scenario: Initialize a private room for confirmed friends
- **WHEN** a user requests a private chat room with a confirmed friend
- **THEN** the system returns the existing stable private room or creates one private room and membership for the two confirmed friends only

### Requirement: Uncontrolled room join is rejected
The system SHALL reject uncontrolled direct room-join attempts that are not backed by a controlled MVP entry path.

#### Scenario: Reject direct join for a group room
- **WHEN** a user attempts to join a group room through an uncontrolled public join request
- **THEN** the system rejects the request and SHALL NOT create room membership

#### Scenario: Reject direct join for a private room
- **WHEN** a user attempts to join a private room through a manual join request
- **THEN** the system rejects the request and SHALL NOT create room membership
