## ADDED Requirements

### Requirement: 合并转发应先保留后端骨架设计
The system SHALL document a minimal backend contract before implementing merged-forward messages.

#### Scenario: m8 只交付设计骨架
- **WHEN** the m8 rich-message Epic is delivered
- **THEN** merged-forward messages remain design-only
- **AND** the production implementation is deferred to a later change
