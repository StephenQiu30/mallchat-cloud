# im-product-mvp

## MODIFIED Requirements

### Requirement: IM MVP scope is explicit
The system SHALL define the MallChat IM MVP around friend relationships, sessions, private rooms, group rooms, message delivery, unread/read state, online status, mobile chat experience, and the basic moments feed.

#### Scenario: MVP scope is used for implementation decisions
- **WHEN** a new IM task is planned
- **THEN** the task is classified as MVP, P1, or P2 according to the PRD scope before implementation starts
- **AND** basic moments feed tasks SHALL be treated as MVP work
- **AND** advanced space decoration, public square, visitor records, and recommendation feed SHALL remain outside the MVP unless a later change explicitly promotes them

#### Scenario: Moments MVP completion is reviewed
- **WHEN** the `add-moments-feed-mvp` change is archived
- **THEN** reviewers SHALL treat publishing, friend-visible listing, and author deletion as completed foundation work only
- **AND** full moments MVP completion SHALL still require likes, comments, and interaction notifications from a later change
