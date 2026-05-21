## ADDED Requirements

### Requirement: 多端 E2E 矩阵定义 IM 最小验收面
The system SHALL maintain a multi-client E2E acceptance matrix for core IM flows before implementing broad client automation.

#### Scenario: Core mobile clients share the same IM acceptance baseline
- **WHEN** QA plans Taro, UniApp, or Flutter E2E coverage
- **THEN** the matrix SHALL include login state, friend relationship, group membership, message send/receive, message state, moments feed, and notification flows
- **AND** each flow SHALL name the required backend API or WebSocket contract

#### Scenario: Admin acceptance is scoped to governance flows
- **WHEN** QA plans Admin E2E coverage
- **THEN** the matrix SHALL include user, group, message, moments, notification, report, and audit log governance checks
- **AND** Admin coverage SHALL depend on stable backend APIs before page automation starts

#### Scenario: Matrix does not imply immediate client implementation
- **WHEN** the m10 backend PR is reviewed
- **THEN** reviewers SHALL treat the matrix as planning and acceptance documentation
- **AND** missing Taro, UniApp, Flutter, or Admin scripts SHALL NOT block the backend audit search API delivery
