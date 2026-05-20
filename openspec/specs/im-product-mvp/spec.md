# im-product-mvp Specification

## Purpose
TBD - created by archiving change orchestrate-im-product-mvp. Update Purpose after archive.
## Requirements
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

### Requirement: Cross-client responsibilities are separated
The system SHALL treat `mallchat-cloud` as the chat domain source, `mallchat-taro` as the first mobile UI restoration client, and `mallchat-uniapp`, `mallchat_flutter`, and `mallchat-admin` as follow-up synchronization surfaces unless a change explicitly targets them.

#### Scenario: Taro is selected as first design restoration target
- **WHEN** a change references `demo.html` mobile IM design restoration
- **THEN** the first executable UI implementation SHALL target `mallchat-taro` unless the change states otherwise

### Requirement: Product documentation is traceable to OpenSpec
The system SHALL keep IM PRD, execution plan, OpenSpec tasks, and acceptance conclusions linked so that future implementation can trace why a behavior exists.

#### Scenario: Acceptance is reviewed
- **WHEN** QA reviews an IM MVP change
- **THEN** the review can find the PRD, plan, OpenSpec change, and acceptance result from docs or OpenSpec paths

