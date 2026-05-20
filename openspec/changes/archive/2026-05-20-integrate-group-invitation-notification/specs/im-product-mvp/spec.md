## MODIFIED Requirements

### Requirement: IM MVP scope is explicit
The system SHALL define the MallChat IM MVP around friend relationships, sessions, private rooms, group rooms, message delivery, unread/read state, online status, mobile chat experience, the basic moments feed, and recoverable notification-center records for friend application, friend approval, and direct group invitation reminders.

#### Scenario: MVP scope is used for implementation decisions
- **WHEN** a new IM task is planned
- **THEN** the task is classified as MVP, P1, or P2 according to the PRD scope before implementation starts
- **AND** basic moments feed tasks SHALL be treated as MVP work
- **AND** advanced space decoration, public square, visitor records, and recommendation feed SHALL remain outside the MVP unless a later change explicitly promotes them

#### Scenario: Moments MVP completion is reviewed
- **WHEN** the `enhance-moments-interaction` change is archived after `add-moments-feed-mvp`
- **THEN** reviewers SHALL treat publishing, friend-visible listing, author deletion, likes, comments, and interaction notifications as the completed backend moments MVP foundation
- **AND** frontend API integration and advanced Qzone-style features SHALL remain separate follow-up work

#### Scenario: Friend application can be recovered from notification center
- **WHEN** a user receives a valid friend application
- **THEN** the user can receive the existing realtime friend-application event
- **AND** the user can later find a corresponding notification-center record

#### Scenario: Friend approval can be recovered from notification center
- **WHEN** a user's friend application is approved
- **THEN** the user can receive the existing realtime friend-approval event
- **AND** the user can later find a corresponding notification-center record

#### Scenario: Group invitation can be recovered from notification center
- **WHEN** a user is directly invited into a group room
- **THEN** the user can receive the existing session update event
- **AND** the user can later find a corresponding notification-center record
