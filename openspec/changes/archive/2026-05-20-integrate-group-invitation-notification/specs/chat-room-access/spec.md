## MODIFIED Requirements

### Requirement: Room membership entry follows controlled MVP paths
The system SHALL allow room membership to be created only through controlled MVP entry paths, including group creation, group invitation, and private room initialization between confirmed friends; direct group invitations SHALL create notification-center records for invited members.

#### Scenario: Create a group room with initial members
- **WHEN** an authenticated user creates a new group room and specifies valid invited members
- **THEN** the system creates the room, adds the creator as owner, and adds only the invited members admitted through the controlled creation flow
- **AND** the system creates a `user` notification for each invited member
- **AND** each notification `relatedType` is `chat_room`
- **AND** each notification `relatedId` is the group room ID

#### Scenario: Invite a friend into an existing group room
- **WHEN** a room member invites a confirmed friend into an existing group room
- **THEN** the system creates room membership for that invited friend through the invitation flow
- **AND** the system creates a `user` notification for the invited friend
- **AND** the notification `relatedType` is `chat_room`
- **AND** the notification `relatedId` is the group room ID

#### Scenario: Group invitation notification failure is degraded
- **WHEN** a direct group invitation creates membership and session facts but notification-center creation fails
- **THEN** the invited member remains in the room
- **AND** the invited member keeps the created or updated chat session
- **AND** the existing session update event remains attempted

#### Scenario: Initialize a private room for confirmed friends
- **WHEN** a user requests a private chat room with a confirmed friend
- **THEN** the system returns the existing stable private room or creates one private room and membership for the two confirmed friends only
