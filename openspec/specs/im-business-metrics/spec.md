# im-business-metrics Specification

## Purpose
TBD - created by archiving change add-im-business-metrics. Update Purpose after archive.
## Requirements
### Requirement: Key IM business actions are measurable
The system SHALL record low-cardinality counters for key IM business actions.

#### Scenario: Chat message send is accepted
- **WHEN** chat-service accepts a message send
- **THEN** the system SHALL record a business metric with action `message_send` and result `success`

#### Scenario: Friend apply is accepted or deduplicated
- **WHEN** chat-service creates a friend apply
- **THEN** the system SHALL record a business metric with action `friend_apply` and result `success`
- **WHEN** chat-service returns an existing pending apply for the same direction
- **THEN** the system SHALL record a business metric with action `friend_apply` and result `duplicate`

#### Scenario: Moment interaction succeeds
- **WHEN** a moment like or comment succeeds
- **THEN** the system SHALL record a business metric with action `moment_like` or `moment_comment` and result `success`

#### Scenario: Metric tags stay low cardinality
- **WHEN** recording business metrics
- **THEN** user ids, room ids, message ids, moment ids, and comment ids SHALL NOT be used as metric tags

